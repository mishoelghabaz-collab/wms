package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.WarehouseStrings
import com.example.ui.theme.*

@Composable
fun VoiceAssistantDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> String
) {
    var commandInput by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val sampleCommands = if (isArabic) listOf(
        "صرف 500 من 13008512",
        "توريد 1000 إلى 13008509",
        "موقع كود 13009444",
        "بحث عن 13007859"
    ) else listOf(
        "Issue 500 from 13008512",
        "Receive 1000 to 13008509",
        "Location of 13009444",
        "Search 13007859"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkNavyBase,
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("voice_assistant_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Text(
                    text = WarehouseStrings.voiceAssistantTitle(isArabic),
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                // Pulsing Mic Visualizer
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = if (isListening) 0.2f * pulseSize else 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                            .clickable {
                                isListening = !isListening
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = DarkNavyBase,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = if (isListening) WarehouseStrings.voiceListening(isArabic) else "انقر للتحدث أو اختر أمراً",
                    color = EmeraldGreenLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Voice Response display
                if (responseMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavySurfaceHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = responseMessage ?: "",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Quick Command Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isArabic) "أوامر سريعة بنقرة واحدة:" else "One-tap voice commands:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    sampleCommands.forEach { cmd ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkNavySurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    commandInput = cmd
                                    responseMessage = onExecuteCommand(cmd)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cmd,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Manual input field
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "اكتب أمراً صوتياً يدوياً..." else "Type voice command...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (commandInput.isNotBlank()) {
                                responseMessage = onExecuteCommand(commandInput)
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = EmeraldGreen)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = DarkNavyCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
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
