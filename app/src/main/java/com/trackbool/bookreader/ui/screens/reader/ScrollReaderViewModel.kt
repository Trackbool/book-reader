package com.trackbool.bookreader.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScrollReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getBookUseCase: GetBookUseCase,
    getBookContentUseCase: GetBookContentUseCase,
    updateBookProgressUseCase: UpdateBookProgressUseCase,
) : BaseReaderViewModel(
    bookId = savedStateHandle.get<Long>("bookId") ?: -1,
    getBookUseCase = getBookUseCase,
    getBookContentUseCase = getBookContentUseCase,
    updateBookProgressUseCase = updateBookProgressUseCase,
) {
    override fun onChaptersLoaded() {
        _isLoadingRender.value = true
        _isLoadingData.value = false
    }

    fun onProgressChanged(readingProgress: Float, chapterId: String, documentPositionData: String) {
        onChapterChanged(chapterId)
        updateProgress(readingProgress, chapterId, documentPositionData)
    }
}
