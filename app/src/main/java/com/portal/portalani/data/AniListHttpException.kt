package com.portal.portalani.data

import java.io.IOException

/** HTTP failure from AniList GraphQL (e.g. expired or revoked access token). */
class AniListHttpException(
    val code: Int,
    body: String,
) : IOException("GraphQL failed ($code): $body") {
  fun isAuthFailure(): Boolean = code == 401 || code == 403
}
