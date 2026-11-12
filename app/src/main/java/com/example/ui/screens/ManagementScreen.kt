package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthDiagnosticReport
import com.example.data.WarehouseStrings
import com.example.model.Plant
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.AuthDiagnosticsPanel
import com.example.ui.theme.*

@Composable
fun ManagementScreen(
    isArabic: Boolean,
    currentPlantId: String,
    plants: List<Plant>,
    users: List<UserAccount>,
    onSwitchPlant: (String) -> Unit,
    onAddNewPlant: (String, String, String) -> Unit,
    onDeletePlant: (String) -> Unit,
    onUpdateUserRole: (String, UserRole) -> Unit,
    onToggleUserLock: (String) -> Unit,
    authDiagnosticReport: AuthDiagnosticReport = AuthDiagnosticReport(),
    onRunAuthDiagnostics: () -> Unit = {},
    onCopyDiagnosticReport: (String) -> Unit = {},
    onOpenAuthLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var newPlantCode by remember { mutableStateOf("") }
    var newPlantEn by remember { mutableStateOf("") }
    var newPlantAr by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .testTag("management_screen"),
        contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ================= SECTION 0: ADMIN & AUTH DIAGNOSTICS =================
        item {
            // Root Admin Active Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkNavySurfaceHigh,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("root_admin_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Shield",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = WarehouseStrings.adminRootActive(isArabic),
                        color = EmeraldGreenLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Visible Auth Status & Gmail Diagnostics Panel (Debug / Pre-Deployment Verification)
        item {
            AuthDiagnosticsPanel(
                report = authDiagnosticReport,
                isArabic = isArabic,
                onRunDiagnostics = onRunAuthDiagnostics,
                onCopyReport = onCopyDiagnosticReport,
                onOpenLogin = onOpenAuthLogin
            )
        }

        // ================= SECTION 1: USERS & SCANNERS =================
        item {
            Text(
                text = WarehouseStrings.usersSectionTitle(isArabic),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Users Cards
        items(users, key = { it.id }) { user ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_card_${user.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top: Actions, Role Badge, Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onToggleUserLock(user.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (user.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock",
                                    tint = if (user.isLocked) CoralRed else EmeraldGreen,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberOrange.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = user.role.name,
                                color = AmberOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = user.name,
                            color = TextWhite,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Device info
                    Text(
                        text = user.device,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Role switch chips (Admin, Supervisor, Viewer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAdmin = user.role == UserRole.ADMIN
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAdmin) AmberOrange else DarkNavySurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateUserRole(user.id, UserRole.ADMIN) }
                        ) {
                            Text(
                                text = WarehouseStrings.roleAdmin(isArabic),
                                color = if (isAdmin) DarkNavyBase else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        val isSup = user.role == UserRole.SUPERVISOR
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSup) NeonCyanMuted else DarkNavySurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateUserRole(user.id, UserRole.SUPERVISOR) }
                        ) {
                            Text(
                                text = WarehouseStrings.roleSupervisor(isArabic),
                                color = if (isSup) DarkNavyBase else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        val isViewer = user.role == UserRole.VIEWER
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isViewer) TextWhite else DarkNavySurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateUserRole(user.id, UserRole.VIEWER) }
                        ) {
                            Text(
                                text = WarehouseStrings.roleViewer(isArabic),
                                color = if (isViewer) DarkNavyBase else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ================= SECTION 2: DYNAMIC PLANTS MANAGEMENT =================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Factory,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = WarehouseStrings.plantSectionTitle(isArabic),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = WarehouseStrings.plantSectionSubtitle(isArabic),
                    fontSize = 11.sp,
                    color = AmberOrange,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Plants List Cards
        items(plants, key = { it.id }) { plant ->
            val isActive = plant.id == currentPlantId
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkNavyCard,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isActive) EmeraldGreen else DarkNavyCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plant_card_${plant.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Actions: Delete & Activate button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isActive) {
                            IconButton(
                                onClick = { onDeletePlant(plant.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = { onSwitchPlant(plant.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyanMuted),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = WarehouseStrings.activate(isArabic),
                                    color = DarkNavyBase,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreen.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.8f))
                            ) {
                                Text(
                                    text = WarehouseStrings.active(isArabic),
                                    color = EmeraldGreenLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Plant ID & Names
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = plant.id,
                            color = if (isActive) EmeraldGreenLight else TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isArabic) plant.nameAr else plant.nameEn,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Add New Plant Form
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_plant_form")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = WarehouseStrings.addNewPlant(isArabic),
                        color = AmberOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPlantCode,
                            onValueChange = { newPlantCode = it },
                            placeholder = { Text(WarehouseStrings.plantCodePlaceholder(isArabic), color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = newPlantEn,
                            onValueChange = { newPlantEn = it },
                            placeholder = { Text(WarehouseStrings.plantEnPlaceholder(isArabic), color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    OutlinedTextField(
                        value = newPlantAr,
                        onValueChange = { newPlantAr = it },
                        placeholder = { Text(WarehouseStrings.plantArPlaceholder(isArabic), color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkNavyCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (newPlantCode.isNotBlank()) {
                                onAddNewPlant(newPlantCode, newPlantEn, newPlantAr)
                                newPlantCode = ""
                                newPlantEn = ""
                                newPlantAr = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_new_plant_btn")
                    ) {
                        Text(
                            text = if (isArabic) "+ إضافة المصنع إلى الشبكة" else "+ Add Plant to Network",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
