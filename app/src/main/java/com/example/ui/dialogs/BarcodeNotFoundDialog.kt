package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun BarcodeNotFoundDialog(
    barcode: String,
    factoryId: String,
    factoryName: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAddItemToCurrentFactory: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberOrange),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("barcode_not_found_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Warning Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberOrange.copy(alpha = 0.2f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Not Found",
                            tint = AmberOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Scanned Barcode badge
                TabularText(
                    text = barcode,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )

                // Requirement 3.2 Display Message
                Text(
                    text = if (isArabic)
                        "هذا الكود / الصنف غير مسجل في $factoryName [$factoryId]."
                    else
                        "This code/item is not registered in $factoryName [$factoryId].",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isArabic)
                        "يتم تطبيق العزل التام بين المصانع لمنع اختلاط الأصناف وخطوط الإنتاج. يمكنك تسجيل الصنف فورياً في هذا المصنع:"
                    else
                        "Strict factory isolation is active. You can register this item directly into the active factory:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Requirement 3.3 Dynamic Action Button: "Add Item to Current Factory"
                    Button(
                        onClick = onAddItemToCurrentFactory,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minTouchTarget()
                            .testTag("btn_add_item_to_factory")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkNavyBase)
                            Text(
                                text = if (isArabic) "إضافة الصنف إلى $factoryName" else "Add Item to $factoryName",
                                color = DarkNavyBase,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minTouchTarget()
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء وإعادة المسح" else "Cancel & Scan Again",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
