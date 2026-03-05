package com.trackbool.bookreader.data.epub

import com.trackbool.bookreader.domain.repository.FileManager
import javax.inject.Inject

class EpubAssetResolverFactory @Inject constructor(
    private val fileManager: FileManager,
) {
    fun create(bookRelativePath: String): EpubAssetResolver {
        return EpubAssetResolver(fileManager, bookRelativePath)
    }
}
