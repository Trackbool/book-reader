package com.trackbool.bookreader.ui.screens.reader.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.R

@Composable
fun ReaderProgress(
    modifier: Modifier = Modifier,
    progressPercent: Int,
    currentPage: Int? = null,
    totalPages: Int? = null
) {
    val progressText = when {
        currentPage != null && totalPages != null && totalPages > 0 -> {
            "$currentPage / $totalPages • ${progressPercent}%"
        }
        currentPage != null && totalPages != null && totalPages == 0 -> {
            stringResource(R.string.preparing_your_reading)
        }
        else -> {
            "${progressPercent}%"
        }
    }

    Text(
        text = progressText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.padding(bottom = 8.dp, top = 4.dp)
    )
}
