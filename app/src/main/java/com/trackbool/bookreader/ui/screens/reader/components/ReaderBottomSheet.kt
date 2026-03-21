package com.trackbool.bookreader.ui.screens.reader.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show) return

    var fontSize by remember { mutableFloatStateOf(20f) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = {
                Text(
                    text = stringResource(R.string.settings_font_size),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                Text("${fontSize.toInt()} pt")

                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..30f
                )
            }
        )
    }
}