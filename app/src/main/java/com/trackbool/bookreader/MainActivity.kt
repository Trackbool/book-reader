package com.trackbool.bookreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.trackbool.bookreader.data.local.BookDatabase
import com.trackbool.bookreader.data.local.FileManager
import com.trackbool.bookreader.data.repository.BookRepositoryImpl
import com.trackbool.bookreader.domain.usecase.AddBookUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBookUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import com.trackbool.bookreader.ui.BookListScreen
import com.trackbool.bookreader.ui.theme.BookReaderTheme
import com.trackbool.bookreader.viewmodel.BookViewModel
import com.trackbool.bookreader.viewmodel.BookViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var bookViewModel: BookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = BookDatabase.getDatabase(applicationContext)
        val repository = BookRepositoryImpl(database.bookDao())
        val fileManager = FileManager(applicationContext)

        val getAllBooksUseCase = GetAllBooksUseCase(repository)
        val addBookUseCase = AddBookUseCase(repository)
        val updateBookProgressUseCase = UpdateBookProgressUseCase(repository)
        val deleteBookUseCase = DeleteBookUseCase(repository)
        val importBookUseCase = ImportBookUseCase(repository, fileManager)

        val factory = BookViewModelFactory(
            getAllBooksUseCase,
            addBookUseCase,
            updateBookProgressUseCase,
            deleteBookUseCase,
            importBookUseCase
        )
        bookViewModel = ViewModelProvider(this, factory)[BookViewModel::class.java]

        setContent {
            BookReaderTheme {
                val books by bookViewModel.books.collectAsState()
                val importState by bookViewModel.importState.collectAsState()
                BookListScreen(
                    books = books,
                    viewModel = bookViewModel,
                    importState = importState,
                    onResetImportState = { bookViewModel.resetImportState() }
                )
            }
        }
    }
}
