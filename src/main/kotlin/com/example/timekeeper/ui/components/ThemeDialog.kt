package com.example.timekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timekeeper.data.ThemeMode

/**
 * Choose how the app is themed. "System" follows Windows' light/dark setting;
 * the other two pin it regardless.
 */
@Composable
fun ThemeDialog(
    current: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    OutlinedButton(
                        onClick = { onSelect(mode) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(mode.label() + if (mode == current) "  ✓" else "")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "Follow Windows"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
