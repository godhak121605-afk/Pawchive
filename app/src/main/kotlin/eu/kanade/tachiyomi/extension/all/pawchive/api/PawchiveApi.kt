package eu.kanade.tachiyomi.extension.all.pawchive.api

import eu.kanade.tachiyomi.extension.all.pawchive.network.ApiClient
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

/** Direct client for the schema-defined Pawchive API v1 endpoints. */
class PawchiveApi(
    private val client: ApiClient,
    baseUrl: String = SITE_URL,
) {
    private val baseUrl = baseUrl.toHttpUrl()

    suspend fun creators(): List<PawchiveCreatorDto> =
        client.get(endpoint("creators"), ListSerializer(PawchiveCreatorDto.serializer()), CREATOR_CACHE)

    suspend fun posts(query: String? = null, offset: Int = 0): List<PawchivePostDto> =
        client.get(
            endpoint("posts", query = query, offset = offset),
            ListSerializer(PawchivePostDto.serializer()),
            POST_CACHE,
        )

    suspend fun creatorProfile(service: String, creatorId: String): PawchiveCreatorProfileDto =
        client.get(
            endpoint(service, "user", creatorId, "profile"),
            PawchiveCreatorProfileDto.serializer(),
            DETAIL_CACHE,
        )

    suspend fun creatorPosts(
        service: String,
        creatorId: String,
        query: String? = null,
        offset: Int = 0,
    ): List<PawchivePostDto> =
        client.get(
            endpoint(service, "user", creatorId, query = query, offset = offset),
            ListSerializer(PawchivePostDto.serializer()),
            POST_CACHE,
        )

    suspend fun post(service: String, creatorId: String, postId: String): PawchivePostDto =
        client.get(
            endpoint(service, "user", creatorId, "post", postId),
            PawchivePostDto.serializer(),
            DETAIL_CACHE,
        )

    private fun endpoint(
        vararg path: String,
        query: String? = null,
        offset: Int? = null,
    ): HttpUrl = baseUrl.newBuilder()
        .addPathSegments("api/v1")
        .apply { path.forEach(::addPathSegment) }
        .apply { query?.takeIf(String::isNotBlank)?.let { addQueryParameter("q", it) } }
        .apply { offset?.let { addQueryParameter("o", it.toString()) } }
        .build()

    private companion object {
        const val SITE_URL = "https://pawchive.pw"

        val POST_CACHE: CacheControl = CacheControl.Builder()
            .maxAge(1, TimeUnit.MINUTES)
            .maxStale(5, TimeUnit.MINUTES)
            .build()
        val DETAIL_CACHE: CacheControl = CacheControl.Builder()
            .maxAge(10, TimeUnit.MINUTES)
            .maxStale(1, TimeUnit.HOURS)
            .build()
        val CREATOR_CACHE: CacheControl = CacheControl.Builder()
            .maxAge(1, TimeUnit.HOURS)
            .maxStale(12, TimeUnit.HOURS)
            .build()
    }
}
