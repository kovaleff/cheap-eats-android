package com.kovaleff.cheapeats.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListResponse<T>(
    val data: List<T> = emptyList(),
)

@Serializable
data class SingleResponse<T>(
    val data: T,
)

@Serializable
data class PageResponse<T>(
    val data: List<T> = emptyList(),
    val links: PageLinks? = null,
    val meta: PageMeta? = null,
)

@Serializable
data class PageLinks(
    val first: String? = null,
    val last: String? = null,
    val prev: String? = null,
    val next: String? = null,
)

@Serializable
data class PageMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    val total: Int = 0,
)

@Serializable
data class OverpassElementDto(
    val type: String? = null,
    val id: Long = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenterDto? = null,
    val tags: Map<String, String>? = null,
)

@Serializable
data class OverpassCenterDto(
    val lat: Double,
    val lon: Double,
)

@Serializable
data class OverpassResponseDto(
    val elements: List<OverpassElementDto> = emptyList(),
)

@Serializable
data class SyncOverpassRequestDto(
    val elements: List<SyncOverpassElementDto>,
)

@Serializable
data class SyncOverpassElementDto(
    @SerialName("osm_id") val osmId: String,
    val lat: Double,
    val lon: Double,
    val name: String? = null,
    val amenity: String? = null,
    val tags: Map<String, String>? = null,
)

@Serializable
data class GeocodeResponseDto(
    val lat: Double,
    val lng: Double,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class GeocodeRequestDto(
    val address: String,
)

@Serializable
data class CommentRequestDto(
    @SerialName("check_amount") val checkAmount: Int,
    @SerialName("price_quality") val priceQuality: Int,
    @SerialName("portion_size") val portionSize: Int,
    @SerialName("poison_risk") val poisonRisk: Boolean = false,
    val comment: String,
)
