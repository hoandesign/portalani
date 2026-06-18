package com.portal.portalani

import android.app.Application
import com.portal.portalani.data.AniListAuth
import com.portal.portalani.data.AniListAuthPort
import com.portal.portalani.data.AniListClient
import com.portal.portalani.data.AniListClientPort
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.CalendarWeekCache
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.WeatherClient
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

internal data class MainViewModelDeps(
    val tokens: TokenStore,
    val settingsStore: SettingsStore,
    val slideCache: AnimeSlideCache,
    val calendarWeekCache: CalendarWeekCache,
    val client: AniListClientPort,
    val auth: AniListAuthPort,
    val weatherClient: WeatherClient,
    val http: OkHttpClient,
) {
  companion object {
    fun live(app: Application): MainViewModelDeps {
      val http =
          OkHttpClient.Builder()
              .callTimeout(60, TimeUnit.SECONDS)
              .readTimeout(45, TimeUnit.SECONDS)
              .build()
      val tokens = TokenStore(app)
      return MainViewModelDeps(
          tokens = tokens,
          settingsStore = SettingsStore(app),
          slideCache = AnimeSlideCache(app),
          calendarWeekCache = CalendarWeekCache(app),
          client = AniListClient(http),
          auth = AniListAuth(http, tokens),
          weatherClient = WeatherClient(http),
          http = http,
      )
    }
  }
}
