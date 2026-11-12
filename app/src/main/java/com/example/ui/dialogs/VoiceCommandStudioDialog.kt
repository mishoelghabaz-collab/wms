package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WarehouseStrings
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun VoiceCommandStudioDialog(
    isArabic: Boolean,
    currentPlantId: String,
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> String
) {
    var isRecording by remember { mutableStateOf(true) }
    var commandText by remember { mutableStateOf("إيشو 20 بيس من كود 13008512") }
    var executionResult by remember { mutableStateOf<String?>(null) }
    var isCheatSheetExpanded by remember { mutableStateOf(true) }

    // Pulsing recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "voice_recording")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("voice_command_studio_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header (Screenshot 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎙 المساعد الصوتي التفاعلي / Voice Command Studio",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Live Recording Container (Red Box Border matching screenshot)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CoralRed.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header with Live Recording Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CoralRed.copy(alpha = pulseAlpha))
                                )
                            }
                            Text(
                                text = if (isRecording) "🎙 جاري التسجيل الحي... اضغط إيقاف عند الانتهاء" else "تم إيقاف التسجيل - يمكنك التعديل أدناه",
                                color = if (isRecording) CoralRed else NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Big Red Stop Button (or Start Button)
                        Button(
                            onClick = {
                                isRecording = !isRecording
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) CoralRed else EmeraldGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(46.dp)
                                .testTag("voice_stop_toggle_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isRecording) "إيقاف / Stop ⏹" else "بدء التسجيل / Record 🎙",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Subtitle
                        Text(
                            text = "سجل صوتك ثم عدل النص أو الأرقام بحرية قبل الموافقة والتنفيذ في [$currentPlantId]",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Voice Command Syntax Cheat-Sheet (Screenshot 1)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title & Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCheatSheetExpanded = !isCheatSheetExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📖 دليل الأوامر الصوتية / Voice Command Syntax Cheat-Sheet 📖",
                                    color = AmberOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isCheatSheetExpanded) "▲ إخفاء الدليل" else "▼ إظهار الدليل",
                                color = NeonCyanMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedVisibility(visible = isCheatSheetExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "اضغط على أي صيغة لإدراجها في صندوق النص وتعديل الكود أو الكمية بحرية قبل التنفيذ:",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )

                                // 1. Issue Template
                                SyntaxCategoryBox(
                                    title = "- صرف (Issue):",
                                    titleColor = CoralRed,
                                    arabicText = "\"إيشو 20 بيس من كود 13007655\"",
                                    englishText = "\"Issue 20 pcs from 13007655\"",
                                    onSelect = { commandText = it }
                                )

                                // 2. Receive Template
                                SyntaxCategoryBox(
                                    title = "+ توريد (Receive):",
                                    titleColor = EmeraldGreenLight,
                                    arabicText = "\"ريسيف 50 بيس كود 13007655 في لوكيشن A0101\"",
                                    englishText = "\"Receive 50 pcs 13007655 at A0101\"",
                                    onSelect = { commandText = it }
                                )

                                // 3. Move Bin Template
                                SyntaxCategoryBox(
                                    title = "🔁 نقل لوكيشن داخلي (Move Bin):",
                                    titleColor = AmberOrange,
                                    arabicText = "\"انقل 15 بيس كود 13007655 للوكيشن B0501\"",
                                    englishText = "\"Move 15 pcs of 13007655 to B0501\"",
                                    onSelect = { commandText = it }
                                )
                            }
                        }
                    }
                }

                // Command Text Input with Instant Editability
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("الأمر الصوتي المكتشف (قابل للتعديل اليدوي)", color = NeonCyanMuted, fontSize = 12.sp) },
                    trailingIcon = {
                        if (commandText.isNotEmpty()) {
                            IconButton(onClick = { commandText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkNavyCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_command_input_field")
                )

                // Result feedback if executed
                if (executionResult != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavySurfaceHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenLight, modifier = Modifier.size(18.dp))
                            Text(
                                text = executionResult ?: "",
                                color = EmeraldGreenLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Execute Command Button
                Button(
                    onClick = {
                        if (commandText.isNotBlank()) {
                            executionResult = onExecuteCommand(commandText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("execute_voice_command_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = DarkNavyBase, modifier = Modifier.size(18.dp))
                        Text(
                            text = "🚀 تأكيد وتنفيذ الأمر الصوتي في [$currentPlantId]",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minTouchTarget()
                        .testTag("close_voice_studio_btn")
                ) {
                    Text(
                        text = WarehouseStrings.close(isArabic),
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SyntaxCategoryBox(
    title: String,
    titleColor: Color,
    arabicText: String,
    englishText: String,
    onSelect: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Arabic pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .minTouchTarget()
                        .clickable { onSelect(arabicText.replace("\"", "")) }
                ) {
                    Text(
                        text = arabicText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // English pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .minTouchTarget()
                        .clickable { onSelect(englishText.replace("\"", "")) }
                ) {
                    Text(
                        text = englishText,
                        color = AmberOrange,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
