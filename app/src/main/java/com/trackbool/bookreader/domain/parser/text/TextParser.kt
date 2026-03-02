package com.trackbool.bookreader.domain.parser.text

interface TextParser {
    suspend fun parse(text: String): List<ReaderText>
}
