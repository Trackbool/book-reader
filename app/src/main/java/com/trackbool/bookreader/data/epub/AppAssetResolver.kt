package com.trackbool.bookreader.data.epub

import android.content.Context
import com.trackbool.bookreader.domain.repository.AssetResolver
import java.io.InputStream

class AppAssetResolver(
    private val context: Context,
) : AssetResolver {

    override fun resolve(path: String): InputStream? = try {
        context.assets.open(path)
    } catch (e: Exception) {
        null
    }

    override fun release() {}
}
