package com.opportunity.app.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Section 3.2 "Natural Input Over Forms": the primary capture affordance. Free text goes to
 * CaptureParser (AI layer); user reviews/edits the structured result afterward.
 */
@Composable
fun QuickAddBar(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isParsing: Boolean = false,
    placeholder: String = "Add anything you could, should, or want to do…",
    onVoiceClick: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )
        if (onVoiceClick != null) {
            IconButton(onClick = onVoiceClick) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice capture")
            }
        }
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSubmit(text)
                    text = ""
                }
            }
        ) {
            if (isParsing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Send, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
