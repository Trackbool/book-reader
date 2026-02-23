package com.trackbool.bookreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.trackbool.bookreader.data.BookDatabase
import com.trackbool.bookreader.data.BookRepository
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
        val repository = BookRepository(database.bookDao())
        val factory = BookViewModelFactory(repository)
        bookViewModel = ViewModelProvider(this, factory)[BookViewModel::class.java]

        setContent {
            BookReaderTheme {
                val books by bookViewModel.books.collectAsState()
                BookListScreen(books = books)
            }
        }
    }
}
