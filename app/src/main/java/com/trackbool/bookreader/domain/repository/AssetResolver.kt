package com.trackbool.bookreader.domain.repository

import java.io.InputStream

interface AssetResolver {
    fun resolve(path: String): InputStream?

    fun release()
}