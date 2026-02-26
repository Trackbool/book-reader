package com.trackbool.bookreader.data.source

import android.net.Uri
import com.trackbool.bookreader.domain.source.BookSource

interface BookSourceFactory {
    fun create(uri: Uri): BookSource
}
