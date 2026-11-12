package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WarehouseStrings
import com.example.model.*
import com.example.ui.components.StaggeredItem
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    isArabic: Boolean,
    currentPlantId: String,
    items: List<WarehouseItem>,
    transactions: List<InventoryTransaction>,
    plants: List<Plant>,
    onSelectTab: (Int) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenQuickAdd: () -> Unit,
    onOpenItemDetails: (WarehouseItem) -> Unit,
    onUndoTransaction: (String) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plantItems = items.filter { it.plantId == currentPlantId }
    val uniqueMaterialsCount = plantItems.map { it.sku }.distinct().size
    val totalStockPcs = plantItems.sumOf { it.quantity }
    val allDistinctLines = plantItems.flatMap { it.productionLines }.distinct()
    val lowStockItems = plantItems.filter { it.quantity in 1..<it.minStockThreshold }
    val outOfStockItems = plantItems.filter { it.quantity <= 0 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dashboard Banner Header (Staggered Item 0)
        item {
            StaggeredItem(index = 0) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .testTag("dashboard_header_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NEXUS WMS • ENTERPRISE LIVE",
                                        color = EmeraldGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = WarehouseStrings.dashboardTitle(isArabic, currentPlantId),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Plant Badge with Optical Center
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AmberOrange.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.4f)),
                                modifier = Modifier.minTouchTarget(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentPlantId,
                                        color = AmberOrange,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = WarehouseStrings.dashboardSubtitle(isArabic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 2. KPI Metrics Grid with Tabular Numbers & Layout Shift Prevention (Staggered Item 1)
        item {
            StaggeredItem(index = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = WarehouseStrings.kpiTotalUnique(isArabic),
                            value = "$uniqueMaterialsCount",
                            sub = "SKUs",
                            icon = Icons.Default.Category,
                            accentColor = NeonCyanMuted,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = WarehouseStrings.kpiTotalStockPcs(isArabic),
                            value = "%,d".format(totalStockPcs),
                            sub = "PCS",
                            icon = Icons.Default.Inventory2,
                            accentColor = AmberOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = WarehouseStrings.kpiTotalLines(isArabic),
                            value = "${allDistinctLines.size}",
                            sub = "Lines",
                            icon = Icons.Default.LinearScale,
                            accentColor = PurpleAccent,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = WarehouseStrings.kpiTotalPlants(isArabic),
                            value = "${plants.size}",
                            sub = "Plants",
                            icon = Icons.Default.Apartment,
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = WarehouseStrings.kpiLowStockWarning(isArabic),
                            value = "${lowStockItems.size}",
                            sub = "Warning",
                            icon = Icons.Default.Warning,
                            accentColor = Color(0xFFFBBF24),
                            isAlert = lowStockItems.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = WarehouseStrings.kpiCriticalOut(isArabic),
                            value = "${outOfStockItems.size}",
                            sub = "Critical",
                            icon = Icons.Default.ErrorOutline,
                            accentColor = CrimsonRed,
                            isAlert = outOfStockItems.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Quick Action Hub with Min 48dp Touch Targets (Staggered Item 2)
        item {
            StaggeredItem(index = 2) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isArabic) "إجراءات لوجستية سريعة" else "Quick Logistics Actions",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionSquareButton(
                                title = if (isArabic) "الماسح" else "Scanner",
                                icon = Icons.Default.QrCodeScanner,
                                color = NeonCyanMuted,
                                onClick = onOpenScanner,
                                modifier = Modifier.weight(1f)
                            )
                            ActionSquareButton(
                                title = if (isArabic) "صنف جديد" else "Add Item",
                                icon = Icons.Default.AddBox,
                                color = AmberOrange,
                                onClick = onOpenQuickAdd,
                                modifier = Modifier.weight(1f)
                            )
                            ActionSquareButton(
                                title = if (isArabic) "خطوط الإنتاج" else "Lines",
                                icon = Icons.Default.LinearScale,
                                color = PurpleAccent,
                                onClick = { onSelectTab(2) },
                                modifier = Modifier.weight(1f)
                            )
                            ActionSquareButton(
                                title = if (isArabic) "تصدير CSV" else "Export",
                                icon = Icons.Default.FileDownload,
                                color = EmeraldGreen,
                                onClick = onExportCsv,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Production Lines Overview (Staggered Item 3)
        item {
            StaggeredItem(index = 3) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "خطوط الإنتاج وحصص المواد" else "Production Lines Overview",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { onSelectTab(2) },
                            modifier = Modifier.minTouchTarget(44.dp)
                        ) {
                            Text(
                                text = if (isArabic) "عرض الكل ←" else "View All →",
                                color = AmberOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allDistinctLines) { line ->
                            val lineItems = plantItems.filter { it.productionLines.contains(line) }
                            val linePcs = lineItems.sumOf { it.quantity }
                            val lineReqs = lineItems.sumOf { it.lineRequirements[line] ?: 0 }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .width(210.dp)
                                    .clickable { onSelectTab(2) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = line,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = if (isArabic) "أصناف" else "Items", color = TextMuted, fontSize = 10.sp)
                                            TabularText(text = "${lineItems.size}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                        }
                                        Column {
                                            Text(text = if (isArabic) "مخزون فعلي" else "Stock", color = TextMuted, fontSize = 10.sp)
                                            TabularText(text = "%,d".format(linePcs), color = AmberOrange, fontSize = 14.sp)
                                        }
                                        if (lineReqs > 0) {
                                            Column {
                                                Text(text = if (isArabic) "مطلوب" else "Req", color = TextMuted, fontSize = 10.sp)
                                                TabularText(text = "%,d".format(lineReqs), color = NeonCyanMuted, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Urgent Stock Alerts (Staggered Item 4)
        if (lowStockItems.isNotEmpty() || outOfStockItems.isNotEmpty()) {
            item {
                StaggeredItem(index = 4) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تنبيهات نقص المخزون العاجلة" else "Critical Stock Warnings",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (outOfStockItems + lowStockItems).take(4).forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.quantity <= 0) CrimsonRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (item.quantity <= 0) CrimsonRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minTouchTarget()
                                        .clickable { onOpenItemDetails(item) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TabularText(
                                                    text = item.sku,
                                                    color = AmberOrange,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = item.location,
                                                        color = NeonCyanMuted,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.name,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.linesDisplay} • حد الأمان: ${item.minStockThreshold} PCS",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            TabularText(
                                                text = "${item.quantity} ${item.unit}",
                                                color = if (item.quantity <= 0) CrimsonRed else Color(0xFFFBBF24),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = if (item.quantity <= 0) "نفد الرصيد" else "تحت الحد",
                                                color = if (item.quantity <= 0) CrimsonRed else Color(0xFFFBBF24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Shift Activity Stream (Staggered Item 5)
        item {
            StaggeredItem(index = 5) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "آخر حركات الوردية المباشرة" else "Recent Shift Activity",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { onSelectTab(3) },
                            modifier = Modifier.minTouchTarget(44.dp)
                        ) {
                            Text(
                                text = if (isArabic) "سجل الحركات الكامل ←" else "Full Log →",
                                color = AmberOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        transactions.take(5).forEach { trx ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minTouchTarget()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (trx.type) {
                                                        TransactionType.RECEIVE_INBOUND -> EmeraldGreen.copy(alpha = 0.2f)
                                                        TransactionType.ISSUE_OUTBOUND -> CrimsonRed.copy(alpha = 0.2f)
                                                        TransactionType.INTER_PLANT_TRANSFER -> PurpleAccent.copy(alpha = 0.2f)
                                                        else -> AmberOrange.copy(alpha = 0.2f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (trx.type) {
                                                    TransactionType.RECEIVE_INBOUND -> Icons.Default.Add
                                                    TransactionType.ISSUE_OUTBOUND -> Icons.Default.Remove
                                                    TransactionType.INTER_PLANT_TRANSFER -> Icons.Default.SwapHoriz
                                                    else -> Icons.Default.Sync
                                                },
                                                contentDescription = null,
                                                tint = when (trx.type) {
                                                    TransactionType.RECEIVE_INBOUND -> EmeraldGreen
                                                    TransactionType.ISSUE_OUTBOUND -> CrimsonRed
                                                    TransactionType.INTER_PLANT_TRANSFER -> PurpleAccent
                                                    else -> AmberOrange
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TabularText(
                                                    text = trx.sku,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                TabularText(
                                                    text = trx.formattedTime,
                                                    color = TextMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                            Text(
                                                text = trx.notes.ifEmpty { trx.type.name },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val deltaPrefix = if (trx.quantityDelta > 0) "+${trx.quantityDelta}" else "${trx.quantityDelta}"
                                        if (trx.quantityDelta != 0) {
                                            TabularText(
                                                text = "$deltaPrefix PCS",
                                                color = if (trx.quantityDelta > 0) EmeraldGreen else CrimsonRed,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        if (trx.canUndo) {
                                            Text(
                                                text = "تراجع ↺",
                                                color = AmberOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable { onUndoTransaction(trx.id) }
                                                    .padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric Card with Layout Shift Prevention (fixed height 96dp - Principle 4)
 * and Tabular Numbers (Principle 3)
 */
@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    accentColor: Color,
    isAlert: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isAlert) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAlert) accentColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(96.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                TabularText(
                    text = value,
                    color = if (isAlert) accentColor else MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sub,
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Action Square Button with min 48dp touch target (Principle 8) & Optical Center (Principle 2)
 */
@Composable
private fun ActionSquareButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(72.dp)
            .minTouchTarget()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
