package com.portal.portalani.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AniListJsonParserTest {
  private fun loadFixture(name: String): JSONObject {
    val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("anilist/$name")) {
      "Missing fixture: anilist/$name"
    }
    return JSONObject(stream.bufferedReader().readText())
  }

  @Test
  fun parseMediaPage_returnsSlideWithTitleFallback() {
    val page = loadFixture("media_page.json")
    val slides = AniListJsonParser.parseMediaPage(page)
    assertEquals(1, slides.size)
    val slide = slides.first()
    assertEquals(15125, slide.id)
    assertEquals("Test English Title", slide.title)
    assertEquals("テスト", slide.nativeTitle)
    assertEquals("https://example.com/cover-xl.jpg", slide.coverUrl)
    assertEquals("TV", slide.format)
    assertEquals("JP", slide.countryOfOrigin)
    assertEquals(listOf("Shounen", "School"), slide.tags)
    assertEquals("Test Studio", slide.studio)
    assertEquals("A test synopsis.", slide.description)
    assertEquals("dQw4w9WgXcQ", slide.trailerYoutubeId)
    assertTrue(slide.rankings.isNotEmpty())
    assertEquals(8, slide.nextAiringEpisode)
  }

  @Test
  fun parseMediaListPage_includesListEntryFields() {
    val page = loadFixture("media_list_page.json")
    val slides = AniListJsonParser.parseMediaListPage(page)
    assertEquals(1, slides.size)
    val slide = slides.first()
    assertEquals(20001, slide.id)
    assertEquals("List Romaji", slide.title)
    assertEquals(9001, slide.listEntryId)
    assertEquals(ListStatus.CURRENT, slide.listStatus)
    assertEquals(8.5f, slide.userScore)
    assertTrue(slide.isFavourite)
  }

  @Test
  fun parseMedia_missingCover_returnsNull() {
    val media =
        JSONObject()
            .put("id", 1)
            .put("title", JSONObject().put("romaji", "No Cover"))
    assertNull(AniListJsonParser.parseMedia(media))
  }

  @Test
  fun parseMedia_usesBannerWhenNoCover() {
    val media =
        JSONObject()
            .put("id", 2)
            .put("title", JSONObject().put("english", "Banner Only"))
            .put("bannerImage", "https://example.com/banner-only.jpg")
            .put("genres", org.json.JSONArray())
    val slide = AniListJsonParser.parseMedia(media)
    assertNotNull(slide)
    assertEquals("https://example.com/banner-only.jpg", slide!!.coverUrl)
  }

  @Test
  fun parseMediaPage_skipsMalformedNodes() {
    val page =
        JSONObject()
            .put(
                "media",
                org.json.JSONArray()
                    .put(JSONObject().put("id", 1))
                    .put(
                        JSONObject()
                            .put("id", 2)
                            .put("title", JSONObject().put("english", "Ok"))
                            .put("coverImage", JSONObject().put("large", "https://example.com/c.jpg"))
                            .put("genres", org.json.JSONArray()),
                    ),
            )
    val slides = AniListJsonParser.parseMediaPage(page)
    assertEquals(1, slides.size)
    assertEquals(2, slides.first().id)
  }

  @Test
  fun parseAiringSchedule_returnsCalendarEntry() {
    val node = loadFixture("airing_schedule_node.json")
    val entry = AniListJsonParser.parseAiringSchedule(node)
    assertNotNull(entry)
    assertEquals(50001, entry!!.scheduleId)
    assertEquals(30001, entry.mediaId)
    assertEquals("Schedule English", entry.englishTitle)
    assertEquals(6, entry.episode)
    assertEquals(1700003600, entry.airingAt)
    assertEquals(ListStatus.PLANNING, entry.listStatus)
    assertEquals(listOf("Seinen"), entry.tags)
  }

  @Test
  fun parseAiringSchedule_missingCover_returnsNull() {
    val node =
        JSONObject()
            .put("id", 1)
            .put("episode", 1)
            .put("airingAt", 100)
            .put("media", JSONObject().put("id", 9).put("title", JSONObject()))
    assertNull(AniListJsonParser.parseAiringSchedule(node))
  }

  @Test
  fun parseUserScore_normalizesLegacyScale() {
    assertEquals(8.5f, AniListJsonParser.parseUserScore(8.5))
    assertEquals(8.5f, AniListJsonParser.parseUserScore(85.0))
    assertNull(AniListJsonParser.parseUserScore(0.0))
  }

  @Test
  fun parseListStatus_invalidReturnsNull() {
    assertNull(AniListJsonParser.parseListStatus("NOT_A_STATUS"))
    assertEquals(ListStatus.CURRENT, AniListJsonParser.parseListStatus("CURRENT"))
  }

  @Test
  fun parseRelatedMedia_mergesRelationsAndRecommendations() {
    val media =
        JSONObject(
            """
            {
              "relations": {
                "edges": [
                  {
                    "relationType": "SEQUEL",
                    "node": {
                      "id": 2,
                      "type": "ANIME",
                      "title": { "english": "Sequel Show" },
                      "coverImage": { "large": "https://example.com/sequel.jpg" },
                      "averageScore": 82
                    }
                  }
                ]
              },
              "recommendations": {
                "nodes": [
                  {
                    "mediaRecommendation": {
                      "id": 3,
                      "type": "ANIME",
                      "title": { "romaji": "Reco One" },
                      "coverImage": { "large": "https://example.com/reco.jpg" },
                      "averageScore": 75
                    }
                  },
                  {
                    "mediaRecommendation": {
                      "id": 2,
                      "type": "ANIME",
                      "title": { "english": "Duplicate" },
                      "coverImage": { "large": "https://example.com/dup.jpg" }
                    }
                  }
                ]
              }
            }
            """
                .trimIndent(),
        )

    val related = AniListJsonParser.parseRelatedMedia(media, sourceId = 1)

    assertEquals(2, related.size)
    assertEquals(2, related[0].id)
    assertEquals("Sequel Show", related[0].title)
    assertEquals("Sequel", related[0].kindLabel)
    assertEquals(3, related[1].id)
    assertEquals("Reco One", related[1].title)
    assertEquals("Recommendation", related[1].kindLabel)
    assertTrue(related[1].isRecommendation)
  }

  @Test
  fun mergeRelatedMedia_ordersFranchiseBeforeRecommendations() {
    val franchise =
        listOf(
            RelatedAnime(10, "Season 1", "https://example.com/10.jpg", kindLabel = "Prequel", sortYear = 2019),
            RelatedAnime(20, "Season 2", "https://example.com/20.jpg", kindLabel = "Prequel", sortYear = 2021),
        )
    val relationEdges = listOf("PREQUEL" to node(20, "Season 2 duplicate"))
    val recommendations = listOf(node(99, "Reco"))

    val merged =
        AniListJsonParser.mergeRelatedMedia(
            sourceId = 30,
            franchiseCluster = franchise,
            relationEdges = relationEdges,
            recommendationNodes = recommendations,
        )

    assertEquals(listOf(10, 20, 99), merged.map { it.id })
    assertEquals("Recommendation", merged.last().kindLabel)
  }

  private fun node(id: Int, title: String): JSONObject =
      JSONObject(
          """
          {
            "id": $id,
            "type": "ANIME",
            "title": { "english": "$title" },
            "coverImage": { "large": "https://example.com/$id.jpg" }
          }
          """
              .trimIndent(),
      )
}
