package com.trackbool.bookreader.domain.repository

import java.io.InputStream

interface AssetResolver {
    fun resolve(bookPath: String, path: String): InputStream?

    fun release(bookPath: String)

    fun releaseAll()
}