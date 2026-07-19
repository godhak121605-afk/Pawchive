package eu.kanade.tachiyomi.extension.all.pawchive.source

import eu.kanade.tachiyomi.extension.all.pawchive.api.PawchiveApi
import eu.kanade.tachiyomi.extension.all.pawchive.network.ApiClient
import eu.kanade.tachiyomi.extension.all.pawchive.network.PawchiveException
import eu.kanade.tachiyomi.extension.all.pawchive.utils.PawchiveRoutes
import eu.kanade.tachiyomi.extension.all.pawchive.utils.PawchiveTarget
import eu.kanade.tachiyomi.extension.all.pawchive.utils.renderableFiles
import eu.kanade.tachiyomi.extension.all.pawchive.utils.toPages
import eu.kanade.tachiyomi.extension.all.pawchive.utils.toSChapter
import eu.kanade.tachiyomi.extension.all.pawchive.utils.toSManga
import eu.kanade.tachiyomi.extension.all.pawchive.utils.websiteUrl
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Pawchive source using only Pawchive API v1 JSON endpoints. */
@Suppress("unused")
class PawchiveSource @JvmOverloads constructor(
    private val injectedApi: PawchiveApi? = null,
) : HttpSource() {

    override val name = "Pawchive"
    override val baseUrl = SITE_URL
    override val lang = "all"
    override val supportsLatest = true

    override val client: OkHttpClient by lazy {
        // The host client owns the disk Cache; newBuilder retains it for Pawchive requests.
        network.client.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val api: PawchiveApi by lazy {
        injectedApi ?: PawchiveApi(ApiClient(client, headers))
    }

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("User-Agent", "Tachiyomi Pawchive Extension")

    override suspend fun getPopularManga(page: Int): MangasPage {
        val creators = api.creators().sortedByDescending { it.favorited }
        return creators.page(page) { it.toSManga() }
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val posts = api.posts(offset = page.toOffset())
        return MangasPage(posts.map { it.toSManga() }, posts.size == PAGE_SIZE)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        if (query.isBlank()) return getPopularManga(page)

        // The API requires q to be at least three characters. Short creator-name searches use
        // the cached schema endpoint instead of issuing an invalid request.
        if (query.length < 3) {
            return api.creators()
                .filter { it.name.contains(query, ignoreCase = true) }
                .page(page) { it.toSManga() }
        }

        val posts = api.posts(query = query, offset = page.toOffset())
        return MangasPage(posts.map { it.toSManga() }, posts.size == PAGE_SIZE)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = when (val target = PawchiveRoutes.parse(manga.url)) {
        is PawchiveTarget.Creator -> {
            val updatedManga = if (fetchDetails) {
                api.creatorProfile(target.service, target.creatorId).toSManga()
            } else {
                manga
            }
            val updatedChapters = if (fetchChapters) {
                api.allCreatorPosts(target)
                    .filter { it.renderableFiles().isNotEmpty() }
                    .map { it.toSChapter() }
            } else {
                chapters
            }
            SMangaUpdate(updatedManga, updatedChapters)
        }

        is PawchiveTarget.Post -> {
            val post = api.post(target.service, target.creatorId, target.postId)
            val updatedChapters = if (fetchChapters && post.renderableFiles().isNotEmpty()) {
                listOf(post.toSChapter())
            } else if (fetchChapters) {
                emptyList()
            } else {
                chapters
            }
            SMangaUpdate(if (fetchDetails) post.toSManga() else manga, updatedChapters)
        }

        null -> throw PawchiveException("This Pawchive item has an invalid URL.")
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val target = PawchiveRoutes.parse(chapter.url) as? PawchiveTarget.Post
            ?: throw PawchiveException("This Pawchive chapter has an invalid URL.")
        return api.post(target.service, target.creatorId, target.postId).toPages()
    }

    override fun getMangaUrl(manga: SManga): String = PawchiveRoutes.parse(manga.url)
        ?.websiteUrl()
        ?: baseUrl

    override fun getChapterUrl(chapter: SChapter): String = PawchiveRoutes.parse(chapter.url)
        ?.websiteUrl()
        ?: baseUrl

    private suspend fun PawchiveApi.allCreatorPosts(target: PawchiveTarget.Creator) = buildList {
        var offset = 0
        do {
            val posts = creatorPosts(target.service, target.creatorId, offset = offset)
            addAll(posts)
            offset += PAGE_SIZE
        } while (posts.size == PAGE_SIZE)
    }

    private fun Int.toOffset(): Int = (coerceAtLeast(1) - 1) * PAGE_SIZE

    private fun <T> List<T>.page(page: Int, mapper: (T) -> SManga): MangasPage {
        val start = page.toOffset()
        if (start >= size) return MangasPage(emptyList(), false)
        val end = minOf(start + PAGE_SIZE, size)
        return MangasPage(subList(start, end).map(mapper), end < size)
    }

    private companion object {
        const val SITE_URL = "https://pawchive.pw"
        const val PAGE_SIZE = 50
    }
}
