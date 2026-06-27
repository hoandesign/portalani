package com.portal.portalani.data

data class RelatedAnime(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val averageScore: Int? = null,
    /** e.g. "Sequel" or "Recommendation" — shown on carousel cards when set. */
    val kindLabel: String? = null,
    val isRecommendation: Boolean = kindLabel == "Recommendation",
    /** Used to order franchise entries chronologically when available. */
    val sortYear: Int? = null,
) {
  val anilistUrl: String
    get() = "https://anilist.co/anime/$id"
}

/** Minimal slide while full media loads from a related carousel pick. */
fun RelatedAnime.toPlaceholderSlide(): AnimeSlide =
    AnimeSlide(
        id = id,
        title = title,
        nativeTitle = null,
        coverUrl = coverUrl,
        bannerUrl = coverUrl,
        averageScore = averageScore,
        episodes = null,
        status = null,
        season = null,
        seasonYear = null,
        startDateYear = sortYear,
        format = null,
        studio = null,
        genres = emptyList(),
        description = null,
        siteUrl = anilistUrl,
        trailerYoutubeId = null,
    )
