package com.kovaleff.cheapeats

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.kovaleff.cheapeats.data.remote.ApiClient

class CheapEatsApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { ApiClient.okHttpClientForImages }
            .crossfade(true)
            .build()
}
