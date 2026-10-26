package com.kovaleff.cheapeats.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kovaleff.cheapeats.data.model.CategoryDto
import com.kovaleff.cheapeats.data.repository.CheapEatsRepository
import com.kovaleff.cheapeats.data.repository.MapFilters
import com.kovaleff.cheapeats.data.repository.OverpassPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PriceFilter(val level: String, val label: String) {
    CHEAP("cheap", "до 300 ₽"),
    MODERATE("moderate", "300–700 ₽"),
    EXPENSIVE("expensive", "более 700 ₽"),
    ALL("", "любая"),
}

data class MapUiState(
    val loading: Boolean = true,
    val locating: Boolean = false,
    val error: String? = null,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val userLat: Double? = null,
    val userLon: Double? = null,
    val places: List<OverpassPlace> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val priceFilter: PriceFilter = PriceFilter.ALL,
    val worksAtNight: Boolean = false,
) {
    val filteredPlaces: List<OverpassPlace>
        get() = places.filter { p ->
            val v = p.venue
            if (selectedCategoryIds.isNotEmpty() && v?.categoryId !in selectedCategoryIds) return@filter false
            val price = priceFilter.level
            if (price.isNotEmpty() && v?.priceLevel != price) return@filter false
            if (worksAtNight && v?.openNight != true) return@filter false
            true
        }
}

class MapViewModel(
    private val repository: CheapEatsRepository = CheapEatsRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val categories = repository.getCategories()
                _state.update { it.copy(categories = categories) }
            } catch (_: Exception) {
                // категории нужны только для фильтра — не блокируем карту
            }
        }
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun setLocation(lat: Double, lon: Double) {
        val isFirst = _state.value.centerLat == null
        _state.update {
            it.copy(
                userLat = lat,
                userLon = lon,
                centerLat = if (isFirst) lat else it.centerLat,
                centerLon = if (isFirst) lon else it.centerLon,
            )
        }
        // Повторная загрузка POI только если центр заметно сместился или это первая фиксация
        val s = _state.value
        val needReload = isFirst || s.centerLat == null || s.centerLon == null || run {
            val dLat = kotlin.math.abs(s.centerLat - lat)
            val dLon = kotlin.math.abs(s.centerLon - lon)
            dLat > 0.005 || dLon > 0.005
        }
        if (needReload) loadPlaces(lat, lon)
    }

    fun setMapCenter(lat: Double, lon: Double) {
        _state.update { it.copy(centerLat = lat, centerLon = lon) }
    }

    fun locateFailed(message: String) {
        _state.update { it.copy(locating = false, error = message) }
    }

    fun setLocating(value: Boolean) {
        _state.update { it.copy(locating = value) }
    }

    fun reloadForCenter() {
        val s = _state.value
        val lat = s.centerLat ?: return
        val lon = s.centerLon ?: return
        loadPlaces(lat, lon)
    }

    fun toggleCategory(categoryId: Long) {
        _state.update { st ->
            val ids = st.selectedCategoryIds.toMutableSet()
            if (!ids.add(categoryId)) ids.remove(categoryId)
            st.copy(selectedCategoryIds = ids)
        }
    }

    fun setPriceFilter(filter: PriceFilter) {
        _state.update { it.copy(priceFilter = filter) }
    }

    fun toggleNight() {
        _state.update { it.copy(worksAtNight = !it.worksAtNight) }
    }

    fun geocode(address: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val (lat, lon) = repository.geocode(address)
                _state.update { it.copy(loading = false, centerLat = lat, centerLon = lon) }
                loadPlaces(lat, lon)
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Адрес не найден") }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun loadPlaces(lat: Double, lon: Double) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val places = repository.loadOverpassPlaces(lat, lon)
                _state.update { it.copy(loading = false, places = places) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = "Не удалось загрузить данные OpenStreetMap")
                }
            }
        }
    }

    /** Фильтры для карточки заведения (используются при смене фильтров) */
    fun currentFilters(): MapFilters = MapFilters(
        categoryIds = _state.value.selectedCategoryIds,
        priceLevel = _state.value.priceFilter.level.ifEmpty { null },
        worksAtNight = _state.value.worksAtNight,
    )
}
