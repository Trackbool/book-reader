package com.trackbool.bookreader.ui.common.model

import com.trackbool.bookreader.domain.model.ChapterContent

data class ChapterView(
    val id: String,
    val title: String?,
    val content: ChapterContent
)
