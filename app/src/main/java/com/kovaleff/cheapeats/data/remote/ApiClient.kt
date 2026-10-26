package com.kovaleff.cheapeats.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kovaleff.cheapeats.BuildConfig
import com.kovaleff.cheapeats.data.api.ApiConfig
import com.kovaleff.cheapeats.data.api.CheapEatsApiService
import com.kovaleff.cheapeats.data.api.NominatimApiService
import com.kovaleff.cheapeats.data.api.OverpassApiService
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Имя dev-сервера cheap-eats известно только в /etc/hosts хоста.
     * 10.0.2.2 — алиас хоста только из эмулятора; с реального устройства
     * (тот же Wi-Fi) хост доступен по LAN-адресу из BuildConfig. Отдаём оба:
     * OkHttp сам перебирает адреса, пока один не подключится. SNI и проверка
     * сертификата остаются на оригинальном имени из URL.
     */
    private val devDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            if (hostname == "cheap-eats") {
                val lanIp = InetAddress.getByName(BuildConfig.DEV_HOST_LAN_IP)
                val emulatorLoopback = InetAddress.getByAddress(byteArrayOf(10, 0, 2, 2))
                listOf(lanIp, emulatorLoopback)
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
    }

    private val okHttp: OkHttpClient by lazy {
        buildClient()
    }

    /** Клиент для Coil (картинки) — тот же DNS-обход dev-сервера. */
    val okHttpClientForImages: OkHttpClient by lazy {
        buildClient(HttpLoggingInterceptor.Level.NONE)
    }

    private fun buildClient(logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = logLevel
        }
        return OkHttpClient.Builder()
            .dns(devDns)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", ApiConfig.USER_AGENT)
                        .header("Accept", "application/json")
                        .build()
                )
            }
            .addInterceptor(logging)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val cheapEatsApi: CheapEatsApiService by lazy {
        retrofit(ApiConfig.apiBaseUrl).create(CheapEatsApiService::class.java)
    }

    val overpassApi: OverpassApiService by lazy {
        retrofit(ApiConfig.OVERPASS_BASE_URL).create(OverpassApiService::class.java)
    }

    val nominatimApi: NominatimApiService by lazy {
        retrofit(ApiConfig.NOMINATIM_BASE_URL).create(NominatimApiService::class.java)
    }
}
