package eu.kanade.tachiyomi.extension.all.pawchive.network

import eu.kanade.tachiyomi.network.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException

/** A small coroutine client for Pawchive's read-only API. */
class ApiClient(
    private val client: OkHttpClient,
    private val requestHeaders: Headers,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun <T> get(
        url: HttpUrl,
        serializer: KSerializer<T>,
        cacheControl: CacheControl,
    ): T = try {
        val request = Request.Builder()
            .url(url)
            .headers(requestHeaders)
            .header("Accept", "application/json")
            .cacheControl(cacheControl)
            .build()

        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) throw response.toApiException()
            json.decodeFromStream(serializer, response.body.byteStream())
        }
    } catch (error: PawchiveException) {
        throw error
    } catch (error: SocketTimeoutException) {
        throw PawchiveException("Pawchive did not respond in time. Please try again.", cause = error)
    } catch (error: SerializationException) {
        throw PawchiveException("Pawchive returned an unreadable API response.", cause = error)
    } catch (error: IOException) {
        throw PawchiveException("Could not reach Pawchive. Check your connection and try again.", cause = error)
    }

    private fun okhttp3.Response.toApiException(): PawchiveException {
        if (code == 429) {
            val seconds = header("Retry-After")?.toLongOrNull()
            val suffix = seconds?.let { " Retry after $it seconds." }.orEmpty()
            return PawchiveRateLimitException("Pawchive is rate limiting requests.$suffix", seconds)
        }

        return PawchiveException("Pawchive API request failed (HTTP $code).", code)
    }
}

open class PawchiveException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : IOException(message, cause)

class PawchiveRateLimitException(
    message: String,
    val retryAfterSeconds: Long?,
) : PawchiveException(message, 429)
