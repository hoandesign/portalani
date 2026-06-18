package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.json.JSONObject

class AnimeSlideCacheTest {
  @Test
  fun roundTrip_preservesCoreFields() {
    val original =
        sampleSlide(id = 42, season = "FALL", seasonYear = 2023).copy(
            nativeTitle = "ネイティブ",
            tags = listOf("Shounen", "Action"),
            rankings =
                listOf(
                    MediaRanking(5, RankType.RATED, RankScope.ALL_TIME),
                    MediaRanking(12, RankType.POPULAR, RankScope.YEAR, year = 2023),
                ),
            listEntryId = 900,
            listStatus = ListStatus.CURRENT,
            userScore = 8.5f,
            isFavourite = true,
            nextAiringEpisode = 7,
            nextAiringAt = 1_700_000_000,
            trailerYoutubeId = "abc123",
        )
    val restored = original.toCacheJson().toAnimeSlideOrNull()
    assertNotNull(restored)
    assertEquals(original.id, restored!!.id)
    assertEquals(original.title, restored.title)
    assertEquals(original.nativeTitle, restored.nativeTitle)
    assertEquals(original.coverUrl, restored.coverUrl)
    assertEquals(original.listEntryId, restored.listEntryId)
    assertEquals(original.listStatus, restored.listStatus)
    assertEquals(original.userScore, restored.userScore)
    assertEquals(original.isFavourite, restored.isFavourite)
    assertEquals(original.rankings.size, restored.rankings.size)
    assertEquals(original.rankings.first().rank, restored.rankings.first().rank)
  }

  @Test
  fun roundTrip_userScoreLegacyScale_dividesByTen() {
    val json =
        sampleSlide()
            .toCacheJson()
            .put("userScore", 85.0)
    val restored = json.toAnimeSlideOrNull()
    assertEquals(8.5f, restored?.userScore)
  }

  @Test
  fun roundTrip_nullOptionalFields() {
    val original =
        sampleSlide().copy(
            averageScore = null,
            episodes = null,
            studio = null,
            description = null,
            listEntryId = null,
            listStatus = null,
            userScore = null,
            trailerYoutubeId = null,
        )
    val restored = original.toCacheJson().toAnimeSlideOrNull()
    assertNotNull(restored)
    assertNull(restored!!.averageScore)
    assertNull(restored.listStatus)
  }

  @Test
  fun parse_missingCover_returnsNull() {
    val json = sampleSlide().toCacheJson().put("coverUrl", "")
    assertNull(json.toAnimeSlideOrNull())
  }

  @Test
  fun parse_legacyRatedRankingInt() {
    val json =
        JSONObject()
            .put("id", 1)
            .put("title", "Legacy")
            .put("coverUrl", "https://example.com/cover.jpg")
            .put("bannerUrl", "https://example.com/banner.jpg")
            .put("genres", org.json.JSONArray())
            .put("ratedRankAllTime", 3)
            .put("siteUrl", "https://anilist.co/anime/1")
    val slide = json.toAnimeSlideOrNull()
    assertNotNull(slide)
    assertEquals(1, slide!!.rankings.size)
    assertEquals(3, slide.rankings.first().rank)
    assertEquals(RankType.RATED, slide.rankings.first().type)
  }

  @Test
  fun roundTrip_genresPreserved() {
    val original = sampleSlide(genres = listOf("Action", "Fantasy"))
    val restored = original.toCacheJson().toAnimeSlideOrNull()
    assertEquals(listOf("Action", "Fantasy"), restored?.genres)
  }
}
