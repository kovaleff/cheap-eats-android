package com.kovaleff.cheapeats.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kovaleff.cheapeats.data.repository.OverpassPlace
import com.kovaleff.cheapeats.ui.theme.AccentLemon
import com.kovaleff.cheapeats.ui.theme.DarkInk
import com.kovaleff.cheapeats.ui.theme.GreyPanel
import com.kovaleff.cheapeats.ui.theme.GreyPanelSoft
import com.kovaleff.cheapeats.ui.theme.TextMuted
import com.kovaleff.cheapeats.ui.theme.priceLevelColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    preselectedCategoryId: Long? = null,
    viewModel: MapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var selectedPlace by remember { mutableStateOf<OverpassPlace?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Если пришли с тапом на категорию с главной — включаем фильтр
    LaunchedEffect(preselectedCategoryId) {
        preselectedCategoryId?.let { viewModel.toggleCategory(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            requestLocationUpdate(context, viewModel)
        } else {
            viewModel.locateFailed("Геолокация недоступна — разрешение не выдано")
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission(context)) {
            requestLocationUpdate(context, viewModel)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CheapEatsMapView(
            places = state.filteredPlaces,
            centerLat = state.centerLat,
            centerLon = state.centerLon,
            userLat = state.userLat,
            userLon = state.userLon,
            onPlaceClick = { selectedPlace = it },
            modifier = Modifier.fillMaxSize(),
        )

        // Верхняя панель: назад + поиск адреса
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DarkInk.copy(alpha = 0.85f),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = AccentLemon,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Адрес или место", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) viewModel.geocode(searchQuery.trim())
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Найти", tint = AccentLemon)
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            FiltersPanel(
                state = state,
                onToggleCategory = viewModel::toggleCategory,
                onPrice = viewModel::setPriceFilter,
                onToggleNight = viewModel::toggleNight,
            )
        }

        // Кнопка локации
        Surface(
            shape = CircleShape,
            color = DarkInk.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickable {
                        if (viewModel.hasLocationPermission(context)) {
                            viewModel.setLocating(true)
                            requestLocationUpdate(context, viewModel)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.locating) {
                    CircularProgressIndicator(
                        color = AccentLemon,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Моё местоположение",
                        tint = AccentLemon,
                    )
                }
            }
        }

        // Оверлей загрузки
        if (state.loading) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkInk.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = AccentLemon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Загрузка данных…", color = Color.White)
                }
            }
        }

        // Ошибка
        state.error?.let { error ->
            LaunchedEffect(error) { kotlinx.coroutines.delay(3500); viewModel.dismissError() }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp),
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    selectedPlace?.let { place ->
        PlaceBottomSheet(place = place, onDismiss = { selectedPlace = null })
    }
}

/** Запрос свежей позиции: getCurrentLocation на API 30+, иначе последняя известная. */
private fun requestLocationUpdate(context: android.content.Context, viewModel: MapViewModel) {
    viewModel.setLocating(true)
    try {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE)
            as android.location.LocationManager
        val provider = when {
            lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ->
                android.location.LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ->
                android.location.LocationManager.NETWORK_PROVIDER
            else -> null
        }

        fun deliver(loc: android.location.Location?) {
            viewModel.setLocating(false)
            if (loc != null) {
                viewModel.setLocation(loc.latitude, loc.longitude)
            } else {
                viewModel.locateFailed("Позиция пока неизвестна — поиск по адресу или сдвиньте карту")
            }
        }

        if (provider == null) {
            deliver(null)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            lm.getCurrentLocation(
                provider,
                null,
                context.mainExecutor,
                java.util.function.Consumer<android.location.Location> { loc -> deliver(loc) },
            )
        } else {
            val last = lm.getLastKnownLocation(provider)
                ?: lm.allProviders.firstNotNullOfOrNull { lm.getLastKnownLocation(it) }
            deliver(last)
        }
    } catch (e: Exception) {
        viewModel.setLocating(false)
        viewModel.locateFailed("Геолокация недоступна: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersPanel(
    state: MapUiState,
    onToggleCategory: (Long) -> Unit,
    onPrice: (PriceFilter) -> Unit,
    onToggleNight: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkInk.copy(alpha = 0.85f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories, key = { it.id }) { category ->
                    val selected = category.id in state.selectedCategoryIds
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleCategory(category.id) },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GreyPanelSoft,
                            selectedContainerColor = AccentLemon,
                            selectedLabelColor = DarkInk,
                            labelColor = Color.White,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(PriceFilter.entries.toList(), key = { it.level }) { filter ->
                    val selected = state.priceFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onPrice(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GreyPanelSoft,
                            selectedContainerColor = AccentLemon,
                            selectedLabelColor = DarkInk,
                            labelColor = Color.White,
                        ),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ночью", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Switch(checked = state.worksAtNight, onCheckedChange = { onToggleNight() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceBottomSheet(place: OverpassPlace, onDismiss: () -> Unit) {
    val venue = place.venue
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(place.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = priceLevelColor(venue?.priceLevel),
                ) {
                    Text(
                        text = venue?.priceLevelLabel ?: amenityLabel(place.amenity),
                        color = DarkInk,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = amenityEmoji(place.amenity) + " " + amenityLabel(place.amenity),
                    color = TextMuted,
                )
                if (venue == null || !venue.exists) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "⚠️ Не проверено",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            venue?.averageCheck?.let {
                Spacer(Modifier.height(8.dp))
                Text("Средний чек: ${it.trimEnd('0').trimEnd('.')} ₽")
            }

            place.tags["opening_hours"]?.let {
                Spacer(Modifier.height(6.dp))
                Text("🕒 ${it}")
            }
            place.tags["cuisine"]?.let {
                Spacer(Modifier.height(6.dp))
                Text("🍳 Кухня: ${it.replace(';', ',')}")
            }
            place.tags["addr:street"]?.let { street ->
                Spacer(Modifier.height(6.dp))
                Text("📍 $street${place.tags["addr:housenumber"]?.let { ", $it" } ?: ""}")
            }
            place.tags["phone"]?.let {
                Spacer(Modifier.height(6.dp))
                Text("📞 $it")
            }

            venue?.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            venue?.ratingPriceQuality?.let {
                Spacer(Modifier.height(10.dp))
                Text("Оценка цена/качество: $it/10", color = AccentLemon)
            }
        }
    }
}
