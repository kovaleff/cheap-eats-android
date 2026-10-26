package com.kovaleff.cheapeats.data.api

import com.kovaleff.cheapeats.BuildConfig

object ApiConfig {
    /** Корень веб-приложения — для абсолютных ссылок на картинки (/storage/...) */
    val appBaseUrl: String = BuildConfig.APP_BASE_URL

    /** Базовый URL REST API (с завершающим слэшем) */
    val apiBaseUrl: String = BuildConfig.API_BASE_URL

    const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"

    const val USER_AGENT = "CheapEatsAndroid/1.0 (map of cheap eateries; OSM-based)"
}
