package com.trackbool.bookreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun OptionsDialog(
    title: String,
    options: List<OptionItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    OptionItemRow(
                        option = option,
                        onClick = {
                            option.onClick()
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun OptionItemRow(
    option: OptionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (option.icon != null) {
            androidx.compose.material3.Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.iconTint ?: MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = option.textColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

data class OptionItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val iconTint: androidx.compose.ui.graphics.Color? = null,
    val textColor: androidx.compose.ui.graphics.Color? = null
)
