package eu.kanade.tachiyomi.extension.all.pawchive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** Models for the Pawchive API v1 endpoints used by this extension. */
@Serializable
data class PawchiveCreatorDto(
    val id: String,
    val name: String,
    val service: String,
    val favorited: Int = 0,
    val indexed: JsonPrimitive? = null,
    val updated: JsonPrimitive? = null,
)

@Serializable
data class PawchiveCreatorProfileDto(
    val id: String,
    val name: String,
    val service: String,
    @SerialName("public_id") val publicId: String? = null,
    val indexed: String? = null,
    val updated: String? = null,
)

@Serializable
data class PawchivePostDto(
    val id: String,
    val user: String,
    val service: String,
    val title: String? = null,
    val content: String? = null,
    @SerialName("shared_file") val sharedFile: Boolean? = null,
    val added: String? = null,
    val published: String? = null,
    val edited: String? = null,
    val file: PawchiveFileDto? = null,
    val attachments: List<PawchiveFileDto>? = null,
    val next: String? = null,
    val prev: String? = null,
)

@Serializable
data class PawchiveFileDto(
    val name: String? = null,
    val path: String? = null,
)
