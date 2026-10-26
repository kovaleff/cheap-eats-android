package com.kovaleff.cheapeats.data.api

import com.kovaleff.cheapeats.data.model.OverpassResponseDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface OverpassApiService {

    @GET
    suspend fun interpret(@Url url: String, @Query("data") query: String): OverpassResponseDto
}
