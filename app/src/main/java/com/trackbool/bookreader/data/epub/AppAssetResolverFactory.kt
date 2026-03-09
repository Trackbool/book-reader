package com.trackbool.bookreader.data.epub

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppAssetResolverFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun create(): AppAssetResolver {
        return AppAssetResolver(context)
    }
}
