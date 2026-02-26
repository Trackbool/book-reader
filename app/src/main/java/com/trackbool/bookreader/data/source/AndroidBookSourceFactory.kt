package com.trackbool.bookreader.data.source

import android.content.Context
import android.net.Uri
import com.trackbool.bookreader.domain.source.BookSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBookSourceFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BookSourceFactory {

    override fun create(uri: Uri): BookSource {
        return AndroidBookSource(context, uri)
    }
}
