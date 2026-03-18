package com.trackbool.bookreader.ui.screens.reader.components.epub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.trackbool.bookreader.di.EpubFactoryEntryPoint
import com.trackbool.bookreader.domain.repository.AssetResolver
import dagger.hilt.android.EntryPointAccessors

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

@Composable
internal fun rememberAppAssetResolver(): AssetResolver {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors
            .fromApplication(context.applicationContext, EpubFactoryEntryPoint::class.java)
            .appAssetResolverFactory()
            .create()
    }
}
