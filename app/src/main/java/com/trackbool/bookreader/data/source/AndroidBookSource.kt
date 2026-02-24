package com.trackbool.bookreader.data.source

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.trackbool.bookreader.domain.source.BookSource
import java.io.InputStream

class AndroidBookSource(
    private val context: Context,
    private val uri: Uri
) : BookSource {

    override fun openInputStream(): InputStream {
        return context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open file")
    }

    override fun getFileName(): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}
