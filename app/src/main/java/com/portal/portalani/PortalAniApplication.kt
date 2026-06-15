package com.portal.portalani

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PortalAniApplication : Application(), ImageLoaderFactory {
  override fun newImageLoader(): ImageLoader =
      ImageLoader.Builder(this)
          .okHttpClient(
              OkHttpClient.Builder()
                  .callTimeout(60, TimeUnit.SECONDS)
                  .readTimeout(45, TimeUnit.SECONDS)
                  .build(),
          )
          .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
          }
          .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil_image_cache"))
                .maxSizePercent(0.05)
                .build()
          }
          .crossfade(true)
          .build()
}
