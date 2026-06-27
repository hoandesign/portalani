package com.portal.portalani.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniListRelationGraphTest {
  @Test
  fun expandFranchiseBfs_collectsAdjacentSeasonGraphFromLatestEntry() {
    val edgesById =
        mapOf(
            166240 to
                listOf(
                    edge("PREQUEL", 145139, "Swordsmith Village Arc", 2023),
                    edge("SEQUEL", 178788, "Infinity Castle", 2025),
                ),
            145139 to
                listOf(
                    edge("PREQUEL", 142329, "Entertainment District Arc", 2021),
                    edge("SEQUEL", 166240, "Hashira Training Arc", 2024),
                ),
            142329 to
                listOf(
                    edge("PREQUEL", 129874, "Mugen Train Arc", 2021),
                    edge("SEQUEL", 145139, "Swordsmith Village Arc", 2023),
                ),
            129874 to
                listOf(
                    edge("PREQUEL", 101922, "Season 1", 2019),
                    edge("SEQUEL", 142329, "Entertainment District Arc", 2021),
                ),
            101922 to
                listOf(
                    edge("SEQUEL", 112151, "Mugen Train Movie", 2020),
                    edge("SEQUEL", 129874, "Mugen Train Arc", 2021),
                ),
            112151 to listOf(edge("PREQUEL", 101922, "Season 1", 2019)),
            178788 to listOf(edge("PREQUEL", 166240, "Hashira Training Arc", 2024)),
        )

    val franchise =
        AniListRelationGraph.expandFranchiseBfs(
            startId = 166240,
            edgesFor = { mediaId -> edgesById[mediaId].orEmpty() },
        )

    assertEquals(6, franchise.size)
    assertEquals(
        listOf(101922, 112151, 129874, 142329, 145139, 178788),
        franchise.map { it.id },
    )
    assertTrue(franchise.all { it.kindLabel == "Prequel" || it.kindLabel == "Sequel" })
  }

  @Test
  fun mergeRelatedMedia_listsFranchiseBeforeSideStoriesBeforeRecommendations() {
    val franchise =
        listOf(
            RelatedAnime(182255, "Season 2", "https://example.com/2.jpg", kindLabel = "Sequel", sortYear = 2026),
        )
    val sideStory = edge("SIDE_STORY", 170068, "Mini story", 2023)
    val recommendation =
        JSONObject(
            """
            {
              "id": 99,
              "type": "ANIME",
              "title": { "english": "Reco" },
              "coverImage": { "large": "https://example.com/reco.jpg" }
            }
            """
                .trimIndent(),
        )

    val merged =
        AniListJsonParser.mergeRelatedMedia(
            sourceId = 154587,
            franchiseCluster = franchise,
            relationEdges = listOf(sideStory),
            recommendationNodes = listOf(recommendation),
        )

    assertEquals(3, merged.size)
    assertEquals(182255, merged[0].id)
    assertEquals(170068, merged[1].id)
    assertEquals("Side Story", merged[1].kindLabel)
    assertEquals(99, merged[2].id)
    assertEquals("Recommendation", merged[2].kindLabel)
  }

  private fun edge(type: String, id: Int, title: String, year: Int): Pair<String, JSONObject> =
      type to
          JSONObject(
              """
              {
                "id": $id,
                "type": "ANIME",
                "title": { "english": "$title" },
                "startDate": { "year": $year },
                "coverImage": { "large": "https://example.com/$id.jpg" }
              }
              """
                  .trimIndent(),
          )
}
