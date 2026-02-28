package com.trackbool.bookreader.domain.model

data class Cover(
    val bytes: ByteArray,
    val mimeType: String
) {
    val extension: String
        get() = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png"  -> "png"
            "image/gif"  -> "gif"
            "image/webp" -> "webp"
            else         -> "jpg"
        }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cover

        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}