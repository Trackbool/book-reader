package com.trackbool.bookreader.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.theme.BookReaderTheme

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
            ) {
                BookCoverImage(
                    coverUrl = book.coverUrl,
                    title = book.title,
                    modifier = Modifier.fillMaxSize()
                )

                MoreBookOptionsButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BookInfo(
                title = book.title,
                author = book.author,
                fileType = book.fileType,
                modifier = Modifier.weight(0.35f)
            )
        }
    }
}

@Composable
private fun BookCoverImage(
    coverUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNotEmpty()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.book_cover, title),
                placeholder = painterResource(R.drawable.book_placeholder),
                error = painterResource(R.drawable.book_placeholder),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.book_placeholder),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun MoreBookOptionsButton(onClick: () -> Unit,
                                  modifier: Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BookInfo(
    title: String,
    author: String,
    fileType: BookFileType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (author.isNotEmpty()) {
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (fileType != BookFileType.NONE) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = fileType.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardPreview() {
    BookReaderTheme {
        BookCard(
            book = Book(
                id = 1,
                title = "Clean Architecture",
                author = "Robert C. Martin",
                coverUrl = "",
                fileType = BookFileType.PDF,
                filePath = "",
                currentPage = 0,
                totalPages = 0
            ),
            onClick = {},
            onMoreClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 120)
@Composable
private fun BookCardSmallPreview() {
    BookReaderTheme {
        BookCard(
            book = Book(
                id = 2,
                title = "Very Long Title That Should Truncate Properly",
                author = "Author Name",
                coverUrl = "",
                fileType = BookFileType.EPUB,
                filePath = "",
                currentPage = 0,
                totalPages = 0
            ),
            onClick = {},
            onMoreClick = {}
        )
    }
}