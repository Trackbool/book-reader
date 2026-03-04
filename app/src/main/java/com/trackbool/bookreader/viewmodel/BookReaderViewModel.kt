package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.ui.model.ChapterView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBookUseCase: GetBookUseCase,
    private val getBookContentUseCase: GetBookContentUseCase,
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterView>>(emptyList())
    val chapters: StateFlow<List<ChapterView>> = _chapters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _isLoading.value = true
            getBookUseCase(bookId).collectLatest { book ->
                _book.value = book
                loadContent(book)
            }
        }
    }

    private fun loadContent(book: Book) {
        viewModelScope.launch {
            _isLoading.value = true

            val content = getBookContentUseCase(book)
            _chapters.value = content?.chapters
                ?.mapIndexed { index, chapter ->
                    ChapterView(
                        title = chapter.metadata.title.orEmpty(),
                        reference = chapter.metadata.reference,
                        content = chapterContentFor(book, chapter.content, index),
                    )
                }
                .orEmpty()

            _isLoading.value = false
        }
    }

    private fun chapterContentFor(
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