package com.kovaleff.cheapeats.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
    ): List<NominatimPlaceDto>
}

data class NominatimPlaceDto(
    val lat: String,
    val lon: String,
    @kotlinx.serialization.SerialName("display_name") val displayName: String? = null,
)
