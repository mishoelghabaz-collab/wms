package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.auth.AuthDiagnosticVerdict
import com.example.data.WarehouseStrings
import com.example.model.Plant
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun WarehouseTopBar(
    isArabic: Boolean,
    isDarkMode: Boolean,
    currentPlantId: String,
    plants: List<Plant>,
    currentUserRole: UserRole,
    authVerdict: AuthDiagnosticVerdict = AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSelectPlant: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenOcrScanner: () -> Unit,
    onOpenAudit: () -> Unit = {},
    onOpenGmail: () -> Unit = {},
    onOpenAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPlantCombobox by remember { mutableStateOf(false) }

    val authDotColor = when (authVerdict) {
        AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> EmeraldGreen
        AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> NeonCyanMuted
        AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> AmberOrange
        AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> CoralRed
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warehouse_top_bar"),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Scanner, OCR, Audit & Gmail quick icons (Compact 48dp Touch Targets)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("top_qr_scanner_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode / QR",
                            tint = NeonCyanMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Blueprint OCR Scanner Icon
                    IconButton(
                        onClick = onOpenOcrScanner,
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("top_ocr_scanner_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Blueprint OCR",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Inventory Audit & Stocktaking Icon
                    IconButton(
                        onClick = onOpenAudit,
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("top_audit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "Inventory Audit",
                            tint = PurpleAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Gmail Warehouse Alert Dispatcher Icon
                    IconButton(
                        onClick = onOpenGmail,
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("top_gmail_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Gmail Alerts",
                            tint = CoralRed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Right cluster: Theme + Language + Plant + Auth/Role Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Light Mode" else "Dark Mode",
                            tint = if (isDarkMode) GoldYellow else PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .clickable { onToggleLanguage() }
                            .testTag("lang_toggle_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "EN" else "عربي",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberOrange.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .clickable { showPlantCombobox = true }
                            .testTag("plant_selector_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = currentPlantId,
                                color = AmberOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Plant",
                                tint = AmberOrange,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Auth & Role Profile Badge with live Auth/Gmail status dot
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, authDotColor.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .minTouchTarget(44.dp)
                            .clickable { onOpenAuth() }
                            .testTag("user_role_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(authDotColor)
                            )
                            Text(
                                text = when (currentUserRole) {
                                    UserRole.ADMIN -> WarehouseStrings.roleAdmin(isArabic)
                                    UserRole.SUPERVISOR -> WarehouseStrings.roleSupervisor(isArabic)
                                    UserRole.SCANNER_TECH -> WarehouseStrings.roleScanner(isArabic)
                                    UserRole.VIEWER -> WarehouseStrings.roleViewer(isArabic)
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        }
    }

    // Searchable Combobox for Plant Selection (Principle 17)
    if (showPlantCombobox) {
        AlertDialog(
            onDismissRequest = { showPlantCombobox = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (isArabic) "اختر المنشأة / المصنع (Factory Combobox):" else "Select Plant / Facility:",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                SearchableCombobox(
                    title = if (isArabic) "قائمة المصانع المتاحة" else "Available Plants",
                    placeholder = if (isArabic) "اختر المصنع النشط" else "Select active plant",
                    items = plants,
                    selectedItem = plants.find { it.id == currentPlantId },
                    itemLabel = { "${it.id} — ${if (isArabic) it.nameAr else it.nameEn}" },
                    itemSecondaryLabel = { if (it.id == currentPlantId) (if (isArabic) "المصنع الحالي النشط ✓" else "Currently Active ✓") else "" },
                    onItemSelected = {
                        onSelectPlant(it.id)
                        showPlantCombobox = false
                    },
                    isArabic = isArabic
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showPlantCombobox = false },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "إغلاق" else "Close", color = TextMuted)
                }
            }
        )
    }
}
