package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NotebookViewModel
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreDialog(
    viewModel: NotebookViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var jsonText by remember { mutableStateOf("") }
    var isConfirmingRestore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        jsonText = viewModel.exportJson()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پشتیبان‌گیری و بازیابی", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "این متن شامل تمام داده‌های دفتر شماست. برای نگهداری نسخهٔ پشتیبان، متن را کپی کنید. برای بازیابی، متن پشتیبان قبلی را اینجا بچسبانید و دکمه بازیابی را بزنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    placeholder = { Text("متن JSON داده‌ها…") }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Notebook Backup", jsonText)
                    clipboard.setPrimaryClip(clip)
                    viewModel.showToast("متن پشتیبان در کلیپ‌بورد کپی شد")
                }) {
                    Text("کپی متن")
                }

                Button(
                    onClick = {
                        if (!isConfirmingRestore) {
                            isConfirmingRestore = true
                        } else {
                            scope.launch {
                                val success = viewModel.importJson(jsonText)
                                if (success) {
                                    onDismiss()
                                }
                                isConfirmingRestore = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConfirmingRestore) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isConfirmingRestore) "تأیید جایگزینی؟" else "جایگزینی با این متن")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}
