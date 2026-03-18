package com.trackbool.bookreader.ui.screens.reader.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderBottomBar(
    controlsVisible: Boolean,
    showControls: Boolean,
    controls: @Composable () -> Unit,
    progress: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = controlsVisible && showControls,
        label = "BottomBarTransition",
        modifier = modifier.fillMaxWidth()
    ) { showControlsContent ->
        if (showControlsContent) {
            controls()
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                progress()
            }
        }
    }
}
