package eu.kanade.tachiyomi.extension.all.pawchive.utils

import eu.kanade.tachiyomi.extension.all.pawchive.api.PawchiveCreatorDto
import eu.kanade.tachiyomi.extension.all.pawchive.api.PawchiveCreatorProfileDto
import eu.kanade.tachiyomi.extension.all.pawchive.api.PawchiveFileDto
import eu.kanade.tachiyomi.extension.all.pawchive.api.PawchivePostDto
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface PawchiveTarget {
    data class Creator(val service: String, val creatorId: String) : PawchiveTarget
    data class Post(val service: String, val creatorId: String, val postId: String) : PawchiveTarget
}

object PawchiveRoutes {
    fun creator(service: String, creatorId: String): String = "/creator/$service/$creatorId"

    fun post(service: String, creatorId: String, postId: String): String =
        "/post/$service/$creatorId/$postId"

    fun parse(url: String): PawchiveTarget? {
        val segments = url.trim('/').split('/').filter(String::isNotBlank)
        return when {
            segments.size == 3 && segments[0] == "creator" -> PawchiveTarget.Creator(segments[1], segments[2])
            segments.size == 4 && segments[0] == "post" -> PawchiveTarget.Post(segments[1], segments[2], segments[3])
            else -> null
        }
    }
}

fun PawchiveCreatorDto.toSManga(): SManga = creatorManga(service, id, name, favorited)

fun PawchiveCreatorProfileDto.toSManga(): SManga = creatorManga(service, id, name)

private fun creatorManga(service: String, id: String, name: String, favorited: Int = 0): SManga = SManga.create().apply {
    url = PawchiveRoutes.creator(service, id)
    title = name
    author = service.displayName()
    thumbnail_url = creatorIconUrl(service, id)
    description = buildString {
        append("Creator on ")
        append(service.displayName())
        append(".\nPawchive creator ID: ")
        append(id)
        if (favorited > 0) append("\nFavorites: $favorited")
    }
    initialized = true
}

fun PawchivePostDto.toSManga(): SManga = SManga.create().apply {
    url = PawchiveRoutes.post(service, user, id)
    title = title.orEmpty().ifBlank { "Pawchive post $id" }
    author = "${service.displayName()} · $user"
    thumbnail_url = renderableFiles().firstOrNull()?.thumbnailUrl()
    description = content?.stripHtml()?.ifBlank { null }
        ?: "Post $id from creator $user on ${service.displayName()}."
    initialized = true
}

fun PawchivePostDto.toSChapter(): SChapter = SChapter.create().apply {
    url = PawchiveRoutes.post(service, user, id)
    name = title.orEmpty().ifBlank { "Post $id" }
    chapter_number = -2f
    scanlator = service.displayName()
    date_upload = (published ?: added ?: edited).toPawchiveMillis(service)
}

fun PawchivePostDto.toPages(): List<Page> = renderableFiles().mapIndexed { index, file ->
    Page(index, imageUrl = file.originalUrl())
}

fun PawchivePostDto.renderableFiles(): List<PawchiveFileDto> =
    (listOfNotNull(file) + attachments.orEmpty())
        .filter(PawchiveFileDto::isRenderableImage)
        .distinctBy(PawchiveFileDto::path)

fun PawchiveFileDto.isRenderableImage(): Boolean = extension() in IMAGE_EXTENSIONS

fun PawchiveFileDto.originalUrl(): String? = assetUrl(FILE_HOST, this, includeFileName = true)

fun PawchiveFileDto.thumbnailUrl(): String? = assetUrl(IMAGE_HOST, this, thumbnail = true)

private fun assetUrl(
    host: String,
    file: PawchiveFileDto,
    thumbnail: Boolean = false,
    includeFileName: Boolean = false,
): String? = file.path?.trim()?.takeIf(String::isNotEmpty)?.let { path ->
    HttpUrl.Builder()
        .scheme("https")
        .host(host)
        .apply { if (thumbnail) addPathSegment("thumbnail") }
        .addPathSegment("data")
        .addPathSegments(path.trimStart('/'))
        .apply { if (includeFileName) file.name?.let { addQueryParameter("f", it) } }
        .build()
        .toString()
}

fun PawchiveTarget.websiteUrl(): String = when (this) {
    is PawchiveTarget.Creator -> "$SITE_URL/$service/user/$creatorId"
    is PawchiveTarget.Post -> "$SITE_URL/$service/user/$creatorId/post/$postId"
}

private fun PawchiveFileDto.extension(): String = (name ?: path).orEmpty()
    .substringBefore('?')
    .substringAfterLast('.', missingDelimiterValue = "")
    .lowercase(Locale.ROOT)

private fun String.displayName(): String = when (this) {
    "fanbox" -> "Pixiv Fanbox"
    else -> replaceFirstChar { it.titlecase(Locale.ROOT) }
}

private fun String.stripHtml(): String = replace(HTML_TAG, " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String?.toPawchiveMillis(service: String): Long {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return 0L

    val fractionTrimmed = raw.replace(FRACTION, ".${'$'}1")
    val zoned = if (ZONE.matches(fractionTrimmed)) {
        fractionTrimmed
    } else {
        fractionTrimmed + if (service == "fanbox") "+09:00" else "Z"
    }

    return DATE_PATTERNS.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.ROOT).apply { isLenient = false }.parse(zoned)?.time
        }.getOrNull()
    } ?: 0L
}

private fun creatorIconUrl(service: String, id: String): String = "$SITE_URL/icons/$service/$id"

private const val SITE_URL = "https://pawchive.pw"
private const val FILE_HOST = "file.pawchive.pw"
private const val IMAGE_HOST = "img.pawchive.pw"
private val IMAGE_EXTENSIONS = setOf("avif", "bmp", "gif", "heic", "heif", "jpeg", "jpg", "png", "webp")
private val HTML_TAG = Regex("<[^>]+>")
private val FRACTION = Regex("\\.(\\d{3})\\d+")
private val ZONE = Regex(".*(?:Z|[+-]\\d{2}:\\d{2})${'$'}")
private val DATE_PATTERNS = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
)
