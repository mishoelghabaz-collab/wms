package com.example.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import com.example.model.*
import com.example.ui.components.ProgressiveDisclosureSection
import com.example.ui.components.SearchableCombobox
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun MaterialDetailDialog(
    item: WarehouseItem,
    availableLines: List<String>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onCopySku: (String) -> Unit,
    onAssignLine: (sku: String, line: String, reqQty: Int) -> Unit,
    onRemoveLine: (sku: String, line: String) -> Unit,
    onUpdateLineReq: (sku: String, line: String, reqQty: Int) -> Unit,
    onOpenReceive: (WarehouseItem) -> Unit,
    onOpenIssue: (WarehouseItem) -> Unit,
    onOpenRelocate: (WarehouseItem) -> Unit
) {
    var showAddLineField by remember { mutableStateOf(false) }
    var selectedNewLine by remember { mutableStateOf(availableLines.firstOrNull() ?: "Nissan") }
    var newLineReqQty by remember { mutableStateOf("100") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("material_details_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar (Touch Target 48dp on Dismiss Button - Principle 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = WarehouseStrings.materialDetailsTitle(isArabic),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                        Text(
                            text = "مصنع ${item.plantId} • مستودع ${item.warehouse}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minTouchTarget()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content with 4 Structured Sections
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ==========================================
                    // SECTION 1: Material Identification
                    // ==========================================
                    DetailSectionCard(title = WarehouseStrings.sectionIdentification(isArabic), icon = Icons.Default.Fingerprint) {
                        // SKU Block with Tabular Monospace & Dedicated Copy Button (Principle 3 & 8)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) "كود الصنف الأساسي (SKU)" else "Master Material Code (SKU)",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    TabularText(
                                        text = item.sku,
                                        color = AmberOrange,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Button(
                                    onClick = { onCopySku(item.sku) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.minTouchTarget()
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = DarkNavyBase, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = WarehouseStrings.copySku(isArabic), color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow(label = if (isArabic) "وصف الصنف" else "Description", value = item.name)
                        DetailRow(label = if (isArabic) "المصنع والمنشأة" else "Plant Facility", value = "Plant ${item.plantId}")
                        DetailRow(label = if (isArabic) "المستودع الرئيسي" else "Assigned Warehouse", value = item.warehouse)
                        DetailRow(
                            label = if (isArabic) "حالة المخزون" else "Stock Status",
                            value = when (item.status) {
                                StockStatus.NORMAL -> "طبيعي (IN STOCK)"
                                StockStatus.LOW_STOCK -> "منخفض (LOW STOCK)"
                                StockStatus.CRITICAL -> "حرج / نفد (OUT OF STOCK)"
                                StockStatus.OVERSTOCK -> "فائض (OVERSTOCK)"
                            },
                            valueColor = when (item.status) {
                                StockStatus.NORMAL -> EmeraldGreen
                                StockStatus.LOW_STOCK -> Color(0xFFFBBF24)
                                StockStatus.CRITICAL -> CrimsonRed
                                StockStatus.OVERSTOCK -> NeonCyanMuted
                            }
                        )
                    }

                    // ==========================================
                    // SECTION 2: Production Lines & Requirements
                    // ==========================================
                    DetailSectionCard(title = WarehouseStrings.sectionProduction(isArabic), icon = Icons.Default.LinearScale) {
                        Text(
                            text = if (isArabic) "خطوط الإنتاج المرتبطة بالصنف (Multi-Line Association):"
                            else "Assigned Production Lines (Multi-Line):",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Single master balance note
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isArabic) "✓ قاعدة الأمان: الرصيد الفعلي (${item.quantity} ${item.unit}) لا يتضاعف بتعيينه لأكثر من خط إنتاج."
                                else "✓ Safety Guarantee: Physical balance (${item.quantity} ${item.unit}) is never multiplied.",
                                color = EmeraldGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // List of lines with remove option & requirement display
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.productionLines.forEach { line ->
                                val req = item.lineRequirements[line] ?: 0
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = line, color = AmberOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            TabularText(
                                                text = if (req > 0) "الاحتياج المقدر: $req ${item.unit}" else "لا يوجد احتياج محدد",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (item.productionLines.size > 1) {
                                            TextButton(
                                                onClick = { onRemoveLine(item.sku, line) },
                                                colors = ButtonDefaults.textButtonColors(contentColor = CrimsonRed),
                                                modifier = Modifier.minTouchTarget(40.dp)
                                            ) {
                                                Text(text = WarehouseStrings.removeLine(isArabic), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add line with Searchable Combobox (Principle 17)
                        if (!showAddLineField) {
                            OutlinedButton(
                                onClick = { showAddLineField = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minTouchTarget()
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = WarehouseStrings.assignToLine(isArabic), color = AmberOrange, fontSize = 12.sp)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = if (isArabic) "اختر خط الإنتاج والكمية المطلوبة:" else "Select Line & Requirement Qty:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedNewLine,
                                            onValueChange = { selectedNewLine = it },
                                            modifier = Modifier.weight(1.5f),
                                            label = { Text("Line", fontSize = 10.sp) },
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = newLineReqQty,
                                            onValueChange = { newLineReqQty = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Qty", fontSize = 10.sp) },
                                            singleLine = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { showAddLineField = false },
                                            modifier = Modifier.minTouchTarget(40.dp)
                                        ) {
                                            Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextMuted)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                val qty = newLineReqQty.toIntOrNull() ?: 100
                                                if (selectedNewLine.isNotBlank()) {
                                                    onAssignLine(item.sku, selectedNewLine.trim(), qty)
                                                    showAddLineField = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                            modifier = Modifier.minTouchTarget(40.dp)
                                        ) {
                                            Text(text = if (isArabic) "تأكيد الربط" else "Confirm", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = if (isArabic) "الاستهلاك اليومي (Daily Usage)" else "Daily Usage", value = "${item.dailyUsage} ${item.unit}", isTabular = true)
                        DetailRow(label = if (isArabic) "عدد القطع بالضفيرة (Per Harness)" else "Per Harness", value = "${item.perHarness} PCS", isTabular = true)
                        DetailRow(label = if (isArabic) "مستهدف البلان (Plan Target)" else "Plan Target", value = "${item.planTarget} PCS (المنجز: ${item.planCurrent})", isTabular = true)
                    }

                    // ==========================================
                    // SECTION 3: Inventory Information & Balances
                    // ==========================================
                    DetailSectionCard(title = WarehouseStrings.sectionInventory(isArabic), icon = Icons.Default.Inventory2) {
                        DetailRow(
                            label = if (isArabic) "الرصيد الفعلي المتاح" else "Available Physical Balance",
                            value = "${item.quantity} ${item.unit}",
                            valueColor = AmberOrange,
                            isTabular = true
                        )
                        DetailRow(label = if (isArabic) "موقع الرف التخزيني" else "Bin Location", value = item.location, isTabular = true)
                        DetailRow(label = if (isArabic) "تقسيم الموقع" else "Zone/Aisle/Shelf/Bin", value = "Zone ${item.zone} • Aisle ${item.aisle} • Shelf ${item.shelf} • Bin ${item.bin}")
                        DetailRow(label = if (isArabic) "حد الأمان الأدنى (Min)" else "Min Stock Threshold", value = "${item.minStockThreshold} ${item.unit}", isTabular = true)
                        DetailRow(label = if (isArabic) "السعة القصوى (Max)" else "Max Stock Capacity", value = "${item.maxStockCapacity} ${item.unit}", isTabular = true)
                        DetailRow(label = if (isArabic) "أيام التغطية المحسوبة" else "Stock Coverage Days", value = item.coverageDays, isTabular = true)
                        DetailRow(label = if (isArabic) "آخر توقيت صرف" else "Last Dispense Time", value = item.lastDispenseTime)
                        DetailRow(label = if (isArabic) "ملاحظة آخر حركة" else "Last Transaction Note", value = item.lastDispenseNote)
                    }

                    // ==========================================
                    // SECTION 4: Additional Information & Supplier
                    // ==========================================
                    DetailSectionCard(title = WarehouseStrings.sectionAdditional(isArabic), icon = Icons.Default.Info) {
                        DetailRow(label = if (isArabic) "المورد المعتمد" else "Authorized Supplier", value = item.supplier)
                        DetailRow(label = if (isArabic) "العميل المستلم" else "Customer / Plant", value = item.customer)
                        DetailRow(label = if (isArabic) "أولوية الصنف" else "Material Priority", value = item.priority, valueColor = if (item.priority == "URGENT") CrimsonRed else MaterialTheme.colorScheme.onSurface)
                        DetailRow(label = if (isArabic) "الحد الأدنى لدفعة الطلب" else "Minimum Lot Size", value = "${item.minLot} ${item.unit}", isTabular = true)
                        DetailRow(label = if (isArabic) "حالة الصنف" else "Active State", value = if (item.isActive) "نشط (ACTIVE)" else "معطل (INACTIVE)", valueColor = if (item.isActive) EmeraldGreen else TextMuted)
                        if (item.notes.isNotBlank()) {
                            DetailRow(label = if (isArabic) "ملاحظات تشغيلية" else "Operational Notes", value = item.notes)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Quick Action Buttons with Min 48dp Touch Targets (Principle 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenReceive(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(text = WarehouseStrings.receive(isArabic), color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenIssue(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(text = WarehouseStrings.issue(isArabic), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenRelocate(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyanMuted),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(text = WarehouseStrings.transfer(isArabic), color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    isTabular: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        if (isTabular) {
            TabularText(
                text = value,
                color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        } else {
            Text(
                text = value,
                color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
