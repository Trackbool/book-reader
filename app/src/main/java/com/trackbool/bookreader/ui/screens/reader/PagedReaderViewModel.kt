package com.trackbool.bookreader.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.repository.ReaderSettingsRepository
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PagedReaderViewModel @Inject constructor(
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
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _goToPage = MutableSharedFlow<Int>()
    val goToPage: SharedFlow<Int> = _goToPage.asSharedFlow()

    override fun onChaptersLoaded() {
        _isLoadingRender.value = true
        _isLoadingData.value = false
    }

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }

    fun onTotalPagesCalculated(total: Int) {
        _totalPages.value = total
    }

    fun onProgressChanged(readingProgress: Float, chapterId: String, documentPositionData: String) {
        onChapterChanged(chapterId)
        updateProgress(readingProgress, chapterId, documentPositionData)
    }

    fun onPageSelected(page: Int) {
        viewModelScope.launch {
            _goToPage.emit(page)
        }
    }
}
