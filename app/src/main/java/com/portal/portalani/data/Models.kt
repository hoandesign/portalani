package com.portal.portalani.data

enum class ListStatus(val apiValue: String) {
  CURRENT("CURRENT"),
  PLANNING("PLANNING"),
  COMPLETED("COMPLETED"),
  PAUSED("PAUSED"),
  DROPPED("DROPPED"),
  REPEATING("REPEATING"),
}

data class AnimeSlide(
    val id: Int,
    val title: String,
    val nativeTitle: String?,
    /** Portrait cover art — shown on the left. */
    val coverUrl: String,
    /** Wide banner — blurred parallax background. */
    val bannerUrl: String,
    val averageScore: Int?,
    val episodes: Int?,
    val status: String?,
    val seasonYear: Int?,
    val format: String?,
    val studio: String?,
    val genres: List<String>,
    val description: String?,
    val ratedRankAllTime: Int?,
    val popularRankAllTime: Int?,
    val siteUrl: String,
    val trailerYoutubeId: String?,
    /** Authenticated viewer fields — null when signed out or unknown. */
    val listEntryId: Int? = null,
    val listStatus: ListStatus? = null,
    /** User score on a 0–10 scale; null means not rated. */
    val userScore: Float? = null,
    val isFavourite: Boolean = false,
) {
  val anilistUrl: String
    get() = siteUrl.ifBlank { "https://anilist.co/anime/$id" }

  val isOnList: Boolean
    get() = listEntryId != null && listStatus != null

  fun withUserState(
      listEntryId: Int? = this.listEntryId,
      listStatus: ListStatus? = this.listStatus,
      userScore: Float? = this.userScore,
      isFavourite: Boolean = this.isFavourite,
  ): AnimeSlide =
      copy(
          listEntryId = listEntryId,
          listStatus = listStatus,
          userScore = userScore,
          isFavourite = isFavourite,
      )
}

data class AppSettings(
    val shuffle: Boolean = true,
    val intervalMs: Long = 12_000L,
    val sourceMode: SourceMode = SourceMode.LIBRARY,
    val listStatus: ListStatus = ListStatus.CURRENT,
    val formatFilter: FormatFilter = FormatFilter.ALL,
    val librarySort: LibrarySort = LibrarySort.POPULARITY,
    val seasonKey: String = SeasonSelection.ANY_KEY,
    val powerMode: PowerMode = PowerMode.ALWAYS_ON,
    val idleSleepMinutes: Int = PowerPolicy.DEFAULT_IDLE_SLEEP_MINUTES,
    val sleepStartMinutes: Int = PowerPolicy.DEFAULT_SLEEP_START_MINUTES,
    val sleepEndMinutes: Int = PowerPolicy.DEFAULT_SLEEP_END_MINUTES,
) {
  fun libraryFilters(): LibraryFilters =
      LibraryFilters(
          format = formatFilter,
          sort = librarySort,
          seasonKey = seasonKey,
      )

  fun cacheKey(): String =
      when (sourceMode) {
        SourceMode.PERSONAL -> "personal_${listStatus.name}"
        SourceMode.LIBRARY -> "library_${formatFilter.name}_${librarySort.name}_$seasonKey"
      }
}

data class ViewerProfile(
    val id: Int,
    val name: String,
)
