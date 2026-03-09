package com.trackbool.bookreader.di

import com.trackbool.bookreader.data.epub.AppAssetResolverFactory
import com.trackbool.bookreader.data.epub.EpubAssetResolverFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EpubFactoryEntryPoint {
    fun epubAssetResolverFactory(): EpubAssetResolverFactory
    fun appAssetResolverFactory(): AppAssetResolverFactory
}
