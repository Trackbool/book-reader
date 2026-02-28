package com.trackbool.bookreader.domain.model

data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val cover: Cover? = null
)