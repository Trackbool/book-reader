package com.trackbool.bookreader.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.domain.model.ReaderSettings
import com.trackbool.bookreader.domain.repository.ReaderSettingsRepository
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import com.trackbool.bookreader.ui.common.model.ChapterView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BaseReaderViewModel(
    protected val bookId: Long,
    protected val getBookUseCase: GetBookUseCase,
    protected val getBookContentUseCase: GetBookContentUseCase,
    protected val updateBookProgressUseCase: UpdateBookProgressUseCase,
    protected val readerSettingsRepository: ReaderSettingsRepository,
) : ViewModel() {

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterView>>(emptyList())
    val chapters: StateFlow<List<ChapterView>> = _chapters.asStateFlow()

    private val _currentChapter = MutableStateFlow<ChapterView?>(null)
    val currentChapter: StateFlow<ChapterView?> = _currentChapter.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    protected val _isLoadingData = MutableStateFlow(true)

    protected val _isLoadingRender = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = combine(_isLoadingData, _isLoadingRender) { data, render ->
        data || render
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val readerSettings: StateFlow<ReaderSettings> = readerSettingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderSettings())

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _isLoadingData.value = true
            _hasError.value = false
            try {
                val book = getBookUseCase(bookId).first()
                _book.value = book
                loadContent(book)
            } catch (_: Exception) {
                _hasError.value = true
                _isLoadingData.value = false
            }
        }
    }

    protected suspend fun loadContent(book: Book) {
        val chapters = getBookContentUseCase(book)
            ?.chapters
            ?.mapIndexed { index, chapter ->
                ChapterView(
                    title = chapter.metadata.title.orEmpty(),
                    id = chapter.metadata.id,
                    content = chapterContentFor(book, chapter.content, index),
                )
            }
            .orEmpty()

        _chapters.value = chapters

        if (chapters.isNotEmpty()) {
            val initialChapterId = book.currentChapterId
            _currentChapter.value = if (initialChapterId != null) {
                chapters.find { it.id == initialChapterId } ?: chapters.first()
            } else {
                chapters.first()
            }
            onChaptersLoaded()
        } else {
            _hasError.value = true
            _isLoadingData.value = false
        }
    }

    protected fun onChaptersLoaded() {
        _isLoadingRender.value = true
        _isLoadingData.value = false
    }

    fun onProgressChanged(readingProgress: Float, chapterId: String, documentPositionData: String) {
        onChapterChanged(chapterId)
        updateProgress(readingProgress, chapterId, documentPositionData)
    }

    fun onContentReady() {
        _isLoadingRender.value = false
    }

    fun onChapterChanged(chapterId: String) {
        if (chapterId.isNotEmpty()) {
            _currentChapter.value = _chapters.value.find { it.id == chapterId }
        }
    }

    protected fun updateProgress(readingProgress: Float, chapterId: String, documentPositionData: String) {
        val book = _book.value ?: return

        viewModelScope.launch {
            val result = updateBookProgressUseCase(
                book = book,
                readingProgress = readingProgress,
                currentChapterId = chapterId.ifEmpty { null },
                documentPositionData = documentPositionData
            )
            result.onSuccess {
                _book.value = it
            }
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            readerSettingsRepository.updateFontSize(size.toFloat())
        }
    }

    protected fun chapterContentFor(
        book: Book,
        rawContent: String,
        chapterIndex: Int,
    ): ChapterContent = when (book.fileType) {
        BookFileType.EPUB -> ChapterContent.Html(rawContent)
        BookFileType.PDF -> ChapterContent.Pdf(
            filePath = book.filePath,
            pageRange = chapterIndex..chapterIndex,
        )
        BookFileType.NONE -> ChapterContent.Html(rawContent)
    }
}
