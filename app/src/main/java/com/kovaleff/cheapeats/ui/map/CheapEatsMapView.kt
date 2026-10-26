package com.kovaleff.cheapeats.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kovaleff.cheapeats.data.repository.OverpassPlace
import com.kovaleff.cheapeats.ui.theme.BrandAmber
import com.kovaleff.cheapeats.ui.theme.BrandGreen
import com.kovaleff.cheapeats.ui.theme.BrandRed
import com.kovaleff.cheapeats.ui.theme.TextMuted
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private fun priceColor(level: String?): Color = when (level) {
    "cheap" -> BrandGreen
    "moderate" -> BrandAmber
    "expensive" -> BrandRed
    else -> TextMuted
}

fun amenityEmoji(amenity: String?): String = when (amenity) {
    "cafe" -> "☕"
    "fast_food" -> "🍔"
    "restaurant" -> "🍽"
    else -> "🍴"
}

fun amenityLabel(amenity: String?): String = when (amenity) {
    "cafe" -> "Кафе"
    "fast_food" -> "Фастфуд"
    "restaurant" -> "Ресторан"
    else -> "Заведение"
}

/** Пин с цветом ценового уровня и эмодзи типа заведения, как getOverpassIcon в веб-версии. */
private fun makePinDrawable(context: Context, pinColor: Color, emoji: String): Drawable {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2.6f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = pinColor.toArgb() }
    canvas.drawCircle(cx, cy, 30f, paint)

    val tail = Path().apply {
        moveTo(cx - 11f, cy + 24f)
        lineTo(cx, cy + 44f)
        lineTo(cx + 11f, cy + 24f)
        close()
    }
    canvas.drawPath(tail, paint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }
    val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(emoji, cx, baseline, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/** Синяя точка пользователя — как showUserLocation в веб-версии. */
private fun makeUserDotDrawable(context: Context): Drawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(cx, cy, 26f, outer)
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFF3B82F6.toInt()
    }
    canvas.drawCircle(cx, cy, 20f, inner)
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun CheapEatsMapView(
    places: List<OverpassPlace>,
    centerLat: Double?,
    centerLon: Double?,
    userLat: Double?,
    userLon: Double?,
    onPlaceClick: (OverpassPlace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = "CheapEatsAndroid/1.0"
            osmdroidBasePath = File(appContext.cacheDir, "osmdroid")
            osmdroidTileCache = File(appContext.cacheDir, "osmdroid/tiles")
        }
        MapView(appContext).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    // Центрирование при смене центра из ViewModel (геокодинг / локация)
    LaunchedEffect(centerLat, centerLon) {
        val lat = centerLat ?: return@LaunchedEffect
        val lon = centerLon ?: return@LaunchedEffect
        val center = mapView.mapCenter
        if (kotlin.math.abs(center.latitude - lat) > 1e-6 ||
            kotlin.math.abs(center.longitude - lon) > 1e-6
        ) {
            mapView.controller.animateTo(GeoPoint(lat, lon))
        }
    }

    // Маркеры заведений
    LaunchedEffect(places) {
        mapView.overlays.removeAll { it is Marker && it !is UserLocationMarker }
        places.forEach { place ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(place.lat, place.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = place.name
                snippet = amenityLabel(place.amenity)
                setOnMarkerClickListener { _, _ ->
                    onPlaceClick(place)
                    true
                }
            }
            marker.icon = makePinDrawable(mapView.context, priceColor(place.venue?.priceLevel), amenityEmoji(place.amenity))
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    // Маркер пользователя
    LaunchedEffect(userLat, userLon) {
        val lat = userLat ?: return@LaunchedEffect
        val lon = userLon ?: return@LaunchedEffect
        mapView.overlays.removeAll { it is UserLocationMarker }
        val marker = UserLocationMarker(mapView).apply {
            position = GeoPoint(lat, lon)
        }
        marker.icon = makeUserDotDrawable(mapView.context)
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
    )
}

/** Маркер позиции пользователя, чтобы отличать его от пинов заведений при перерисовке. */
class UserLocationMarker(mapView: MapView) : Marker(mapView) {
    init {
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "Вы здесь"
    }
}
