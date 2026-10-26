package com.kovaleff.cheapeats.data.repository

import com.kovaleff.cheapeats.data.remote.ApiClient
import com.kovaleff.cheapeats.data.api.ApiConfig
import com.kovaleff.cheapeats.data.model.CategoryDto
import com.kovaleff.cheapeats.data.model.CityDto
import com.kovaleff.cheapeats.data.model.GeocodeRequestDto
import com.kovaleff.cheapeats.data.model.OverpassElementDto
import com.kovaleff.cheapeats.data.model.ReviewDto
import com.kovaleff.cheapeats.data.model.SyncOverpassElementDto
import com.kovaleff.cheapeats.data.model.SyncOverpassRequestDto
import com.kovaleff.cheapeats.data.model.VenueDto
import com.kovaleff.cheapeats.data.model.VenueSyncResultDto

data class MapFilters(
    val categoryIds: Set<Long> = emptySet(),
    val priceLevel: String? = null,
    val worksAtNight: Boolean = false,
)

data class OverpassPlace(
    val osmId: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val amenity: String?,
    val tags: Map<String, String>,
    val venue: VenueDto?,
    val synced: Boolean,
)

class CheapEatsRepository {

    private val api = ApiClient.cheapEatsApi

    suspend fun getCities(): List<CityDto> = api.getCities().data

    suspend fun getCategories(): List<CategoryDto> = api.getCategories().data

    suspend fun getLatestReviews(perPage: Int = 10): List<ReviewDto> =
        api.getReviews(perPage = perPage).data

    /**
     * Полный сценарий карты, как в веб-версии (resources/js/services/overpass.js):
     * 1. around-запрос к Overpass (cafe|fast_food|restaurant),
     * 2. батч-синк найденных элементов через POST /venues/sync-overpass,
     * 3. склейка POI с заведениями базы.
     */
    suspend fun loadOverpassPlaces(lat: Double, lon: Double, radiusMeters: Int = 2000): List<OverpassPlace> {
        val elements = fetchOverpassElements(lat, lon, radiusMeters)
        val points = elements.mapNotNull { it.toPointOrNull() }
        if (points.isEmpty()) return emptyList()

        val syncResults = syncOverpass(points)
        val byOsmId = syncResults.associateBy { it.osmId }

        return points.map { p ->
            val sync = byOsmId[p.osmId]
            OverpassPlace(
                osmId = p.osmId,
                lat = p.lat,
                lon = p.lon,
                name = sync?.venue?.name ?: p.name ?: "Без названия",
                amenity = p.amenity,
                tags = p.tags,
                venue = sync?.venue,
                synced = sync != null,
            )
        }
    }

    private suspend fun fetchOverpassElements(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
    ): List<OverpassElementDto> {
        val query = """
            [out:json][timeout:25];
            (
                node["amenity"="cafe"](around:${radiusMeters},${lat},${lon});
                node["amenity"="fast_food"](around:${radiusMeters},${lat},${lon});
                node["amenity"="restaurant"](around:${radiusMeters},${lat},${lon});
                way["amenity"="cafe"](around:${radiusMeters},${lat},${lon});
                way["amenity"="fast_food"](around:${radiusMeters},${lat},${lon});
                way["amenity"="restaurant"](around:${radiusMeters},${lat},${lon});
            );
            out geom;
        """.trimIndent()
        // Те же эндпоинты, что в веб-версии; зеркала часто перегружены — до 5 попыток с паузами
        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )
        var lastError: Exception? = null
        outer@ for (round in 0..1) {
            for (endpoint in endpoints) {
                try {
                    return ApiClient.overpassApi.interpret(endpoint, query).elements
                } catch (e: Exception) {
                    lastError = e
                    kotlinx.coroutines.delay(1500L * (round + 1))
                }
            }
        }
        throw lastError ?: Exception("Overpass unavailable")
    }

    private data class OverpassPoint(
        val osmId: String,
        val lat: Double,
        val lon: Double,
        val name: String?,
        val amenity: String?,
        val tags: Map<String, String>,
    )

    private fun OverpassElementDto.toPointOrNull(): OverpassPoint? {
        val tags = tags ?: return null
        val amenity = tags["amenity"] ?: return null
        val lat = lat ?: center?.lat ?: return null
        val lon = lon ?: center?.lon ?: return null
        return OverpassPoint(
            osmId = id.toString(),
            lat = lat,
            lon = lon,
            name = tags["name"] ?: tags["operator"],
            amenity = amenity,
            tags = tags,
        )
    }

    private suspend fun syncOverpass(points: List<OverpassPoint>): List<VenueSyncResultDto> {
        return points.chunked(200).flatMap { chunk ->
            api.syncOverpass(
                SyncOverpassRequestDto(
                    elements = chunk.map {
                        SyncOverpassElementDto(
                            osmId = it.osmId,
                            lat = it.lat,
                            lon = it.lon,
                            name = it.name,
                            amenity = it.amenity,
                            tags = it.tags,
                        )
                    }
                )
            ).data
        }
    }

    /** Адрес -> координаты: сначала наш бэк (Nominatim с кэшем/ограничениями), при неудаче — напрямую Nominatim. */
    suspend fun geocode(address: String): Pair<Double, Double> {
        try {
            val result = api.geocode(GeocodeRequestDto(address = address))
            return result.lat to result.lng
        } catch (_: Exception) {
            // fallback: прямой запрос к Nominatim
        }
        val places = ApiClient.nominatimApi.search(address)
        val first = places.firstOrNull() ?: throw Exception("Адрес не найден")
        val lat = first.lat.toDoubleOrNull() ?: throw Exception("Адрес не найден")
        val lon = first.lon.toDoubleOrNull() ?: throw Exception("Адрес не найден")
        return lat to lon
    }

    /** Абсолютный URL картинки из /storage/... */
    fun absoluteUrl(path: String?): String? {
        val p = path ?: return null
        return if (p.startsWith("http")) p else ApiConfig.appBaseUrl + p
    }
}
