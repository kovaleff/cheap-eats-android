package com.kovaleff.cheapeats.data.api

import com.kovaleff.cheapeats.data.model.CommentRequestDto
import com.kovaleff.cheapeats.data.model.GeocodeRequestDto
import com.kovaleff.cheapeats.data.model.CategoryDto
import com.kovaleff.cheapeats.data.model.CityDto
import com.kovaleff.cheapeats.data.model.CollectionDto
import com.kovaleff.cheapeats.data.model.CommentDto
import com.kovaleff.cheapeats.data.model.GeocodeResponseDto
import com.kovaleff.cheapeats.data.model.ListResponse
import com.kovaleff.cheapeats.data.model.OverpassResponseDto
import com.kovaleff.cheapeats.data.model.PageResponse
import com.kovaleff.cheapeats.data.model.ReviewDto
import com.kovaleff.cheapeats.data.model.SingleResponse
import com.kovaleff.cheapeats.data.model.SyncOverpassRequestDto
import com.kovaleff.cheapeats.data.model.UserDto
import com.kovaleff.cheapeats.data.model.VenueDto
import com.kovaleff.cheapeats.data.model.VenueSyncResultDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CheapEatsApiService {

    @GET("cities")
    suspend fun getCities(): ListResponse<CityDto>

    @GET("cities/default")
    suspend fun getDefaultCity(): SingleResponse<CityDto>

    @GET("categories")
    suspend fun getCategories(): ListResponse<CategoryDto>

    @GET("collections")
    suspend fun getCollections(): ListResponse<CollectionDto>

    @GET("collections/{slug}")
    suspend fun getCollection(@Path("slug") slug: String): SingleResponse<CollectionDto>

    @GET("venues")
    suspend fun getVenues(
        @Query("price_level") priceLevel: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("open_night") openNight: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("distance") distance: Double? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
    ): PageResponse<VenueDto>

    @GET("venues/nearby")
    suspend fun getNearbyVenues(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("distance") distance: Double? = null,
    ): PageResponse<VenueDto>

    @GET("venues/{slug}")
    suspend fun getVenue(@Path("slug") slug: String): SingleResponse<VenueDto>

    @POST("venues/sync-overpass")
    suspend fun syncOverpass(
        @Body body: SyncOverpassRequestDto,
    ): ListResponse<VenueSyncResultDto>

    @GET("reviews")
    suspend fun getReviews(
        @Query("venue_id") venueId: Long? = null,
        @Query("reaction") reaction: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
    ): PageResponse<ReviewDto>

    @POST("venues/{osmId}/comments")
    suspend fun postComment(
        @Path("osmId") osmId: String,
        @Body body: CommentRequestDto,
    ): SingleResponse<CommentDto>

    @GET("user")
    suspend fun getCurrentUser(): UserDto

    @POST("geocode")
    suspend fun geocode(
        @Body body: GeocodeRequestDto,
    ): GeocodeResponseDto
}
