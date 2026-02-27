package com.trackbool.bookreader.domain.model

data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val coverBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentMetadata

        if (title != other.title) return false
        if (author != other.author) return false
        if (description != other.description) return false
        if (!coverBytes.contentEquals(other.coverBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (coverBytes?.contentHashCode() ?: 0)
        return result
    }
}
