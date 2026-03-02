package com.trackbool.bookreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    book: Book,
    content: BookContent?,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
            content != null -> {
                BookContent(
                    content = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se pudo cargar el contenido")
                }
            }
        }
    }
}

@Composable
private fun BookContent(
    content: BookContent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = content.chapters.joinToString("\n\n") { it.content },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
