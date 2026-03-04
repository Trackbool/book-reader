package com.trackbool.bookreader.ui.model

import com.trackbool.bookreader.domain.model.ChapterContent

data class ChapterView(
    val reference: String,
    val title: String?,
    val content: ChapterContent
)