package com.trackbool.bookreader.ui.screens.booklist.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trackbool.bookreader.R

@Composable
fun BookListFab(
    supportedMimeTypes: List<String>,
    onImportBooks: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImportBooks(uris)
        }
    }

    FloatingActionButton(
        onClick = {
            filePickerLauncher.launch(supportedMimeTypes.toTypedArray())
        },
        modifier = modifier
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.import_book))
    }
}
