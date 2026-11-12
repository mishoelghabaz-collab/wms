package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WarehouseStrings
import com.example.model.WarehouseItem
import com.example.ui.theme.*

@Composable
fun SingleShotScannerDialog(
    isArabic: Boolean,
    items: List<WarehouseItem>,
    onDismiss: () -> Unit,
    onSelectItem: (WarehouseItem) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }

    // Laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val matchedItems = items.filter {
        manualInput.isEmpty() ||
                it.sku.contains(manualInput, ignoreCase = true) ||
                it.location.contains(manualInput, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkNavyBase,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("single_shot_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title & QR Icon (Screenshot 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = WarehouseStrings.scannerTitle(isArabic),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = WarehouseStrings.scannerSubtitle(isArabic),
                    color = NeonCyanMuted,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Viewfinder Box with Laser Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkNavyCard)
                        .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                ) {
                    // Camera Reticle View
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Inner targeting rectangle
                        val boxLeft = w * 0.1f
                        val boxTop = h * 0.15f
                        val boxRight = w * 0.9f
                        val boxBottom = h * 0.85f

                        // Corner brackets
                        val bracketLen = 24.dp.toPx()
                        val strokeW = 3.dp.toPx()

                        // Top-left
                        drawLine(NeonCyan, Offset(boxLeft, boxTop), Offset(boxLeft + bracketLen, boxTop), strokeW)
                        drawLine(NeonCyan, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + bracketLen), strokeW)

                        // Top-right
                        drawLine(NeonCyan, Offset(boxRight, boxTop), Offset(boxRight - bracketLen, boxTop), strokeW)
                        drawLine(NeonCyan, Offset(boxRight, boxTop), Offset(boxRight, boxTop + bracketLen), strokeW)

                        // Bottom-left
                        drawLine(NeonCyan, Offset(boxLeft, boxBottom), Offset(boxLeft + bracketLen, boxBottom), strokeW)
                        drawLine(NeonCyan, Offset(boxLeft, boxBottom), Offset(boxLeft, boxBottom - bracketLen), strokeW)

                        // Bottom-right
                        drawLine(NeonCyan, Offset(boxRight, boxBottom), Offset(boxRight - bracketLen, boxBottom), strokeW)
                        drawLine(NeonCyan, Offset(boxRight, boxBottom), Offset(boxRight, boxBottom - bracketLen), strokeW)

                        // Animated scanning laser line (red to blue gradient)
                        val lineY = h * laserY
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(CoralRed, CoralRed, NeonCyan, NeonCyan)
                            ),
                            start = Offset(boxLeft, lineY),
                            end = Offset(boxRight, lineY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Viewfinder pill header
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkNavyBase.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = WarehouseStrings.scannerMode(isArabic),
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = AmberOrange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Input Field: "امسح بالكاميرا أو أدخل كود 8 أرقام / موقع"
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = {
                        Text(
                            text = WarehouseStrings.scannerPrompt(isArabic),
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (manualInput.isNotEmpty()) {
                            IconButton(onClick = { manualInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkNavyCardBorder,
                        focusedContainerColor = DarkNavyCard,
                        unfocusedContainerColor = DarkNavyCard,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_manual_input")
                )

                // Quick Match Items List (Screenshot 1)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchedItems) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkNavyCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectItem(item)
                                    onDismiss()
                                }
                                .testTag("scan_result_${item.sku}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Quantity in PCS
                                    Text(
                                        text = "PCS ${item.quantity}",
                                        color = EmeraldGreenLight,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // IN STOCK Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.7f))
                                    ) {
                                        Text(
                                            text = "IN\nSTOCK",
                                            color = EmeraldGreenLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            lineHeight = 10.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // SKU & Location Pin
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.location,
                                                color = NeonCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = CoralRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Text(
                                            text = item.sku,
                                            color = TextWhite,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = WarehouseStrings.clickToLock(isArabic),
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Dismiss Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_scanner_btn")
                ) {
                    Text(
                        text = WarehouseStrings.close(isArabic),
                        color = NeonCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
