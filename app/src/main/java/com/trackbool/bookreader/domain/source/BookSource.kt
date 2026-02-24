package com.trackbool.bookreader.domain.source

import java.io.InputStream

interface BookSource {
    fun openInputStream(): InputStream
    fun getFileName(): String?
}
