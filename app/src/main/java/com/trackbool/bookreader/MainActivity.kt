package com.trackbool.bookreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.trackbool.bookreader.ui.navigation.AppNavigation
import com.trackbool.bookreader.ui.screens.BookListScreen
import com.trackbool.bookreader.ui.theme.BookReaderTheme
import com.trackbool.bookreader.viewmodel.BookViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BookReaderTheme {
                AppNavigation()
            }
        }
    }
}
