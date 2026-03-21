package com.trackbool.bookreader.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.repository.ReaderSettingsRepository
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScrollReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getBookUseCase: GetBookUseCase,
    getBookContentUseCase: GetBookContentUseCase,
    updateBookProgressUseCase: UpdateBookProgressUseCase,
    readerSettingsRepository: ReaderSettingsRepository,
) : BaseReaderViewModel(
    bookId = savedStateHandle.get<Long>("bookId") ?: -1,
    getBookUseCase = getBookUseCase,
    getBookContentUseCase = getBookContentUseCase,
    updateBookProgressUseCase = updateBookProgressUseCase,
    readerSettingsRepository = readerSettingsRepository,
) {

    private val _goToProgress = MutableSharedFlow<Float>()

    val goToProgress: SharedFlow<Float> = _goToProgress.asSharedFlow()

    override fun onChaptersLoaded() {
        _isLoadingRender.value = true
        _isLoadingData.value = false
    }

    fun onProgressChanged(readingProgress: Float, chapterId: String, documentPositionData: String) {
        onChapterChanged(chapterId)
        updateProgress(readingProgress, chapterId, documentPositionData)
    }

    fun onProgressSelected(progress: Float) {
        viewModelScope.launch {
            _goToProgress.emit(progress / 100f)
        }
    }
}
