package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.WarehouseItem
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*
import com.example.viewmodel.AuditMode

@Composable
fun InventoryAuditDialog(
    isArabic: Boolean,
    currentFactoryId: String,
    currentFactoryName: String,
    items: List<WarehouseItem>,
    auditMode: AuditMode,
    selectedRackId: String,
    physicalCounts: Map<String, Int>,
    isReconciling: Boolean,
    onDismiss: () -> Unit,
    onSelectMode: (AuditMode) -> Unit,
    onSelectRack: (String) -> Unit,
    onUpdatePhysicalCount: (sku: String, count: Int) -> Unit,
    onReconcile: (notes: String) -> Unit
) {
    var notesText by remember { mutableStateOf("") }
    val availableRacks = remember(items, currentFactoryId) {
        items.filter { it.plantId == currentFactoryId }.map { it.location }.distinct().sorted()
    }

    val itemsInScope = remember(items, currentFactoryId, auditMode, selectedRackId) {
        if (auditMode == AuditMode.RACK_LEVEL) {
            items.filter { it.plantId == currentFactoryId && it.location == selectedRackId }
        } else {
            items.filter { it.plantId == currentFactoryId }
        }
    }

    // Calculations
    val totalSystemCount = itemsInScope.sumOf { it.quantity }
    val totalPhysicalCount = itemsInScope.sumOf { physicalCounts[it.sku] ?: it.quantity }
    val netVariance = totalPhysicalCount - totalSystemCount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("inventory_audit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "Audit",
                            tint = EmeraldGreenLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (isArabic) "نظام الجرد وتدقيق المخزون (WMS Audit)" else "Inventory Audit & Stocktaking",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "المصنع الحالي: $currentFactoryName [$currentFactoryId]" else "Active Factory: $currentFactoryName [$currentFactoryId]",
                                color = AmberOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Mode Selector Tabs (Mode A vs Mode B)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Mode A: Rack-Level Audit
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (auditMode == AuditMode.RACK_LEVEL) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (auditMode == AuditMode.RACK_LEVEL) NeonCyan else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                            .clickable { onSelectMode(AuditMode.RACK_LEVEL) }
                            .testTag("audit_tab_rack_level")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "جرد على مستوى الرف" else "Rack-Level Audit",
                                color = if (auditMode == AuditMode.RACK_LEVEL) NeonCyan else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isArabic) "Mode A • رف محدد" else "Mode A • Single Rack",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Mode B: Full Factory Audit
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (auditMode == AuditMode.FULL_FACTORY) PurpleAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (auditMode == AuditMode.FULL_FACTORY) PurpleAccent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                            .clickable { onSelectMode(AuditMode.FULL_FACTORY) }
                            .testTag("audit_tab_full_factory")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "جرد شامل للمصنع" else "Full Factory Audit",
                                color = if (auditMode == AuditMode.FULL_FACTORY) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isArabic) "Mode B • كافة الرفوف" else "Mode B • All Racks",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Rack Selector (Only for Mode A)
                AnimatedVisibility(visible = auditMode == AuditMode.RACK_LEVEL) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isArabic) "اختر الرف المستهدف للجرد (Rack / Bin):" else "Select Target Rack:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableRacks.take(5).forEach { rack ->
                                val isSelected = rack == selectedRackId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberOrange.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AmberOrange else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minTouchTarget(38.dp)
                                        .clickable { onSelectRack(rack) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rack,
                                            color = if (isSelected) AmberOrange else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Variance KPI Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isArabic) "الأصناف" else "Items", color = TextMuted, fontSize = 10.sp)
                            TabularText(
                                text = "${itemsInScope.size}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isArabic) "رصيد النظام" else "System Qty", color = TextMuted, fontSize = 10.sp)
                            TabularText(
                                text = "$totalSystemCount",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isArabic) "العد الفعلي" else "Physical Qty", color = TextMuted, fontSize = 10.sp)
                            TabularText(
                                text = "$totalPhysicalCount",
                                color = EmeraldGreenLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isArabic) "صافي الفارق" else "Net Variance", color = TextMuted, fontSize = 10.sp)
                            TabularText(
                                text = if (netVariance >= 0) "+$netVariance" else "$netVariance",
                                color = if (netVariance == 0) NeonCyan else if (netVariance > 0) EmeraldGreenLight else CoralRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Items Audit Verification Table
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(itemsInScope, key = { it.sku }) { item ->
                        val currentPhysical = physicalCounts[item.sku] ?: item.quantity
                        val variance = currentPhysical - item.quantity

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (variance != 0) (if (variance > 0) EmeraldGreenLight else CoralRed) else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: SKU and Location
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TabularText(
                                            text = item.sku,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AmberOrange.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = item.location,
                                                color = AmberOrange,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.name,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${if (isArabic) "رصيد النظام:" else "System:"} ${item.quantity} ${item.unit}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                // Middle: Physical Count Steppers
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onUpdatePhysicalCount(item.sku, (currentPhysical - 10).coerceAtLeast(0)) },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    ) {
                                        Text("-10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    }

                                    IconButton(
                                        onClick = { onUpdatePhysicalCount(item.sku, (currentPhysical - 1).coerceAtLeast(0)) },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = CoralRed, modifier = Modifier.size(14.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TabularText(
                                                text = "$currentPhysical",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onUpdatePhysicalCount(item.sku, currentPhysical + 1) },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = EmeraldGreenLight, modifier = Modifier.size(14.dp))
                                    }

                                    IconButton(
                                        onClick = { onUpdatePhysicalCount(item.sku, currentPhysical + 10) },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    ) {
                                        Text("+10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGreenLight)
                                    }
                                }

                                // Right: Variance Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        variance == 0 -> NeonCyan.copy(alpha = 0.15f)
                                        variance > 0 -> EmeraldGreen.copy(alpha = 0.2f)
                                        else -> CoralRed.copy(alpha = 0.2f)
                                    },
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            variance == 0 -> "مطابق ✓"
                                            variance > 0 -> "+$variance فائض"
                                            else -> "$variance عجز"
                                        },
                                        color = when {
                                            variance == 0 -> NeonCyan
                                            variance > 0 -> EmeraldGreenLight
                                            else -> CoralRed
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text(if (isArabic) "ملاحظات محضر الجرد والتسوية..." else "Audit reconciliation notes...", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Single-Click "Reconcile & Update Stock" Button
                Button(
                    onClick = { onReconcile(notesText) },
                    enabled = !isReconciling && itemsInScope.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minTouchTarget()
                        .testTag("btn_reconcile_audit")
                ) {
                    if (isReconciling) {
                        CircularProgressIndicator(
                            color = DarkNavyBase,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "جاري تحديث المخزون وحفظ المحضر..." else "Reconciling stock...",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = DarkNavyBase,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "اعتماد وتسوية الجرد السحابي (Reconcile & Update Stock)" else "Reconcile & Update Stock",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
