package com.jzb.jichang.android

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val scale by animateFloatAsState(if (pressed && motion) 0.98f else 1f, quickMotion(if (motion) 100 else 0), label = "button-press")
    androidx.compose.material3.Button(onClick, modifier.heightIn(min = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale }, enabled && !LocalEditorSaveState.current.saving,
        colors = colors, interactionSource = interactions, contentPadding = contentPadding, content = content)
}

@Composable
internal fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.material3.TextButton(onClick, modifier.heightIn(min = 48.dp), enabled && !LocalEditorSaveState.current.saving, colors = colors, contentPadding = contentPadding, content = content)
}
