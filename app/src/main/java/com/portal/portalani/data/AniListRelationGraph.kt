package com.portal.portalani.data

import org.json.JSONObject

/**
 * Walks AniList PREQUEL / SEQUEL / PARENT edges to collect a full season franchise.
 *
 * AniList only links adjacent seasons on each entry (e.g. S4 → prequel S3, sequel movie).
 * A breadth-first walk from the opened title discovers the whole chain, including branches
 * such as movie vs TV-arc sequels from the same parent.
 */
internal object AniListRelationGraph {
  private val FRANCHISE_EDGE_TYPES = setOf("PREQUEL", "SEQUEL", "PARENT")

  const val DEFAULT_MAX_FRANCHISE_NODES = 40

  fun expandFranchiseBfs(
      startId: Int,
      edgesFor: (Int) -> List<Pair<String, JSONObject>>,
      maxNodes: Int = DEFAULT_MAX_FRANCHISE_NODES,
  ): List<RelatedAnime> {
    val visited = mutableSetOf(startId)
    val queue = ArrayDeque<Int>()
    val discovered = mutableListOf<RelatedAnime>()

    queue.add(startId)
    while (queue.isNotEmpty() && discovered.size < maxNodes) {
      val current = queue.removeFirst()
      for ((relationType, node) in edgesFor(current)) {
        if (relationType !in FRANCHISE_EDGE_TYPES) continue
        val item = AniListJsonParser.relatedFromEdge(relationType, node) ?: continue
        if (!visited.add(item.id)) continue
        discovered += item
        queue.add(item.id)
      }
    }

    return discovered.sortedWith(compareBy({ it.sortYear ?: Int.MAX_VALUE }, { it.id }))
  }
}
