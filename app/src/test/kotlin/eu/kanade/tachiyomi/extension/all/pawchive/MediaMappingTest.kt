package eu.kanade.tachiyomi.extension.all.pawchive

import eu.kanade.tachiyomi.extension.all.pawchive.api.PawchivePostDto
import eu.kanade.tachiyomi.extension.all.pawchive.utils.originalUrl
import eu.kanade.tachiyomi.extension.all.pawchive.utils.renderableFiles
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaMappingTest {
    @Test
    fun `only renderable images receive direct file urls`() {
        val post = JSON.decodeFromString<PawchivePostDto>(
            """
            {
              "id": "42",
              "user": "creator",
              "service": "fanbox",
              "file": { "name": "cover image.jpeg", "path": "/ab/cd/cover.jpeg" },
              "attachments": [
                { "name": "movie.mp4", "path": "/ab/cd/movie.mp4" },
                { "name": "gallery.png", "path": "/ab/cd/gallery.png" }
              ]
            }
            """.trimIndent(),
        )

        val images = post.renderableFiles()

        assertEquals(2, images.size)
        assertTrue(images.all { it.originalUrl()?.startsWith("https://file.pawchive.pw/data/") == true })
        assertEquals(
            "https://file.pawchive.pw/data/ab/cd/cover.jpeg?f=cover%20image.jpeg",
            images.first().originalUrl(),
        )
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
