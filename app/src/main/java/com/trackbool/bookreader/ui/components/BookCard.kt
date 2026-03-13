package com.trackbool.bookreader.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.theme.BookReaderTheme
import java.io.File

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    BookCoverImage(
                        coverUrl = book.coverPath,
                        title = book.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                    )

                    BookProgressTag(
                        book.progressPercent,
                        modifier = Modifier
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                            .align(Alignment.TopStart)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                BookInfo(
                    title = book.title,
                    author = book.author,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (isSelectionMode) {
                SelectionIndicator(
                    isSelected = isSelected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                )
            } else {
                MoreBookOptionsButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
    val context = LocalContext.current
    val imageUri = remember(coverUrl) {
        Uri.fromFile(File(context.filesDir, coverUrl)).toString()
    }

    if (coverUrl.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .size(240, 360)
                .build(),
            contentDescription = stringResource(R.string.book_cover, title),
            placeholder = painterResource(R.drawable.book_placeholder),
            error = painterResource(R.drawable.book_placeholder),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(R.drawable.book_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
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
    }
}

@Composable
private fun BookProgressTag(progressPercentage: Int, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.alpha(0.95f)
    ) {
        Text(
            text = "$progressPercentage%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
                coverPath = "",
                fileType = BookFileType.PDF,
                filePath = "",
                readingProgress = 0f
            ),
            onClick = {},
            onMoreClick = {},
            onLongClick = {},
            isSelected = false,
            isSelectionMode = false
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
                coverPath = "",
                fileType = BookFileType.EPUB,
                filePath = "",
                readingProgress = 0f
            ),
            onClick = {},
            onMoreClick = {},
            onLongClick = {},
            isSelected = false,
            isSelectionMode = false
        )
    }
}