package com.kovaleff.cheapeats.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class CityDto(
    val id: Long,
    val name: String,
    val code: String? = null,
    @SerialName("id_region") val idRegion: Long? = null,
    @SerialName("is_default") val isDefault: Int = 0,
    val geo: GeoPoint? = null,
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val slug: String? = null,
    val image: String? = null,
    val position: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("venues_count") val venuesCount: Int? = null,
)

@Serializable
data class VenueDto(
    val id: Long,
    val name: String,
    val slug: String? = null,
    @SerialName("osm_id") val osmId: Long? = null,
    val description: String? = null,
    @SerialName("average_check") val averageCheck: String? = null,
    @SerialName("price_level") val priceLevel: String? = null,
    @SerialName("price_level_label") val priceLevelLabel: String? = null,
    @SerialName("category_id") val categoryId: Long? = null,
    val photo: String? = null,
    @SerialName("rating_price_quality") val ratingPriceQuality: Int? = null,
    @SerialName("rating_portion_size") val ratingPortionSize: Int? = null,
    @SerialName("rating_poison_risk") val ratingPoisonRisk: Int? = null,
    @SerialName("open_night") val openNight: Boolean = false,
    @SerialName("exists") val exists: Boolean = false,
    val tags: Map<String, String>? = null,
    val amenity: String? = null,
    val geo: GeoPoint? = null,
    val category: CategoryDto? = null,
    @SerialName("reviews_count") val reviewsCount: Int? = null,
)

@Serializable
data class CollectionDto(
    val id: Long,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("venues_count") val venuesCount: Int? = null,
)

@Serializable
data class UserDto(
    val id: Long,
    @SerialName("vk_id") val vkId: Long? = null,
    val name: String,
    val email: String? = null,
    @SerialName("photo_200") val photo200: String? = null,
)

@Serializable
data class ReviewDto(
    val id: Long,
    val comment: String,
    val reaction: String? = null,
    @SerialName("reaction_emoji") val reactionEmoji: String? = null,
    val rating: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val user: ReviewUserDto? = null,
)

@Serializable
data class ReviewUserDto(
    val id: Long,
    val name: String,
    @SerialName("photo_200") val photo200: String? = null,
)

@Serializable
data class CommentDto(
    val id: Long,
    @SerialName("check_amount") val checkAmount: Int? = null,
    @SerialName("price_quality") val priceQuality: Int? = null,
    @SerialName("portion_size") val portionSize: Int? = null,
    @SerialName("poison_risk") val poisonRisk: Boolean? = null,
    val comment: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class VenueSyncResultDto(
    @SerialName("osm_id") val osmId: String,
    @SerialName("exists") val exists: Boolean = false,
    val venue: VenueDto? = null,
)
