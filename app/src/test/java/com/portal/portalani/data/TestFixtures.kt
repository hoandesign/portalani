package com.portal.portalani.data

/** Minimal [AnimeSlide] for filter and season unit tests. */
fun sampleSlide(
    id: Int = 1,
    format: String? = "TV",
    countryOfOrigin: String? = "JP",
    source: String? = "MANGA",
    tags: List<String> = listOf("Shounen"),
    genres: List<String> = emptyList(),
    season: String? = "SPRING",
    seasonYear: Int? = 2024,
    startDateYear: Int? = null,
): AnimeSlide =
    AnimeSlide(
        id = id,
        title = "Test Anime",
        nativeTitle = null,
        coverUrl = "https://example.com/cover.jpg",
        bannerUrl = "https://example.com/banner.jpg",
        averageScore = 80,
        episodes = 12,
        status = "RELEASING",
        season = season,
        seasonYear = seasonYear,
        startDateYear = startDateYear,
        format = format,
        countryOfOrigin = countryOfOrigin,
        source = source,
        tags = tags,
        studio = "Test Studio",
        genres = genres,
        description = "Synopsis",
        siteUrl = "https://anilist.co/anime/$id",
        trailerYoutubeId = null,
    )

/** Minimal [CalendarAiringEntry] for calendar filter tests. */
fun sampleCalendarEntry(
    scheduleId: Int = 100,
    mediaId: Int = 1,
    airingAt: Int = 1_700_000_000,
    format: String? = "TV",
    countryOfOrigin: String? = "JP",
    source: String? = "MANGA",
    tags: List<String> = listOf("Shounen"),
    genres: List<String> = emptyList(),
    listStatus: ListStatus? = null,
    averageScore: Int? = 80,
    popularity: Int? = 1000,
): CalendarAiringEntry =
    CalendarAiringEntry(
        scheduleId = scheduleId,
        mediaId = mediaId,
        englishTitle = "Test Anime",
        coverUrl = "https://example.com/cover.jpg",
        episode = 5,
        airingAt = airingAt,
        format = format,
        countryOfOrigin = countryOfOrigin,
        source = source,
        tags = tags,
        season = "SPRING",
        seasonYear = 2024,
        averageScore = averageScore,
        popularity = popularity,
        listStatus = listStatus,
        genres = genres,
    )
