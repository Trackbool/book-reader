package com.trackbool.bookreader.ui.components.epub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.trackbool.bookreader.data.epub.EpubAssetResolverFactory
import com.trackbool.bookreader.domain.repository.AssetResolver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
internal fun rememberEpubAssetResolver(filePath: String): AssetResolver {
    val context = LocalContext.current
    return remember(filePath) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, EpubFactoryEntryPoint::class.java)
            .epubAssetResolverFactory()
            .create(filePath)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EpubFactoryEntryPoint {
    fun epubAssetResolverFactory(): EpubAssetResolverFactory
}