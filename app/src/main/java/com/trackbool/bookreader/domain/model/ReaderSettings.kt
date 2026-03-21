package com.trackbool.bookreader.domain.model

data class ReaderSettings(
    val fontSize: Float = ReaderSettingsDefaults.FONT_SIZE_DEFAULT,
)

object ReaderSettingsDefaults {
    const val FONT_SIZE_MIN = 12f
    const val FONT_SIZE_MAX = 30f
    const val FONT_SIZE_DEFAULT = 20f
}
