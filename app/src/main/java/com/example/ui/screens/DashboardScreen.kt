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
import androidx.compose.runtime.*
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
import com.example.auth.AuthDiagnosticReport
import com.example.data.WarehouseStrings
import com.example.model.*
import com.example.ui.components.CompactAuthStatusStrip
import com.example.ui.components.StaggeredItem
import com.example.ui.components.TabularText
import com.example.ui.components.charts.*
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*
import kotlin.math.abs

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
    authDiagnosticReport: AuthDiagnosticReport = AuthDiagnosticReport(),
    onRefreshAuthDiagnostics: () -> Unit = {},
    onOpenAuthDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val plantItems = items.filter { it.plantId == currentPlantId }
    val uniqueMaterialsCount = plantItems.map { it.sku }.distinct().size
    val totalStockPcs = plantItems.sumOf { it.quantity }
    val allDistinctLines = plantItems.flatMap { it.productionLines }.distinct()
    val lowStockItems = plantItems.filter { it.quantity in 1..<it.minStockThreshold }
    val outOfStockItems = plantItems.filter { it.quantity <= 0 }

    var activeDashboardView by remember { mutableStateOf(0) } // 0: All, 1: Charts & Turnover, 2: Shift Ops
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    val palette = listOf(
        NeonCyan,
        AmberOrange,
        EmeraldGreen,
        PurpleAccent,
        Color(0xFF38BDF8),
        Color(0xFFEC4899),
        Color(0xFFF97316)
    )

    val categoriesData = remember(plantItems, isArabic) {
        val grouped = plantItems.groupBy { it.resolvedCategory }
        grouped.entries.mapIndexed { index, entry ->
            val catItems = entry.value
            val totalStock = catItems.sumOf { it.quantity }
            val totalCap = catItems.sumOf { it.maxStockCapacity }
            val minThresh = catItems.sumOf { it.minStockThreshold }
            val issued = catItems.sumOf { it.issuedQuantity }
            val daily = catItems.sumOf { it.dailyUsage }
            val turnover = if (totalStock > 0) {
                ((issued.toFloat().coerceAtLeast(daily * 28f)) * 12f / totalStock.coerceAtLeast(100)).coerceIn(1.5f, 8.8f)
            } else 2.0f

            CategoryStockData(
                categoryName = entry.key,
                localizedName = getLocalizedCategoryName(entry.key, isArabic),
                totalStock = totalStock,
                totalCapacity = totalCap,
                minThreshold = minThresh,
                itemCount = catItems.size,
                issuedQuantity = issued,
                dailyUsage = daily,
                turnoverRatio = turnover,
                color = palette[index % palette.size]
            )
        }.sortedByDescending { it.totalStock }
    }

    val turnoverTrendData = remember(plantItems, transactions, isArabic) {
        val currentTotalStock = plantItems.sumOf { it.quantity }.coerceAtLeast(1000)
        val currentOutbound = transactions.filter { it.type == TransactionType.ISSUE_OUTBOUND }.sumOf { abs(it.quantityDelta) }
        val currentInbound = transactions.filter { it.type == TransactionType.RECEIVE_INBOUND }.sumOf { abs(it.quantityDelta) }
        val currentRate = if (currentTotalStock > 0) (4.2f + (currentOutbound.toFloat() / 2000f)).coerceIn(2.5f, 7.5f) else 4.5f

        listOf(
            TurnoverTimePoint("W-4", 3.8f, 3400, 3100, (currentTotalStock * 0.92).toInt()),
            TurnoverTimePoint("W-3", 4.1f, 4200, 3900, (currentTotalStock * 0.95).toInt()),
            TurnoverTimePoint("W-2", 4.5f, 4900, 4800, (currentTotalStock * 0.98).toInt()),
            TurnoverTimePoint("W-1", 4.9f, 5400, 5200, (currentTotalStock * 1.02).toInt()),
            TurnoverTimePoint(if (isArabic) "الحالي" else "Now", currentRate, currentInbound.coerceAtLeast(5100), currentOutbound.coerceAtLeast(5400), currentTotalStock)
        )
    }

    val avgTurnoverRatio = remember(categoriesData) {
        if (categoriesData.isNotEmpty()) categoriesData.map { it.turnoverRatio }.average().toFloat() else 4.6f
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact Dashboard Banner Header
        item {
            StaggeredItem(index = 0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .testTag("dashboard_header_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NEXUS WMS • LIVE • ITR ${"%.1fx".format(avgTurnoverRatio)}",
                                        color = EmeraldGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = WarehouseStrings.dashboardTitle(isArabic, currentPlantId),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberOrange.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.4f)),
                                modifier = Modifier.minTouchTarget(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentPlantId,
                                        color = AmberOrange,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = WarehouseStrings.dashboardSubtitle(isArabic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 1b. Live Auth & Gmail Status Diagnostics Strip
        item {
            CompactAuthStatusStrip(
                report = authDiagnosticReport,
                isArabic = isArabic,
                onRunCheck = onRefreshAuthDiagnostics,
                onOpenAuthOrDiagnostics = onOpenAuthDiagnostics
            )
        }

        // 2. Dense 3x2 KPI Metrics Grid
        item {
            StaggeredItem(index = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        KpiMetricCard(
                            title = WarehouseStrings.kpiTotalLines(isArabic),
                            value = "${allDistinctLines.size}",
                            sub = "Lines",
                            icon = Icons.Default.LinearScale,
                            accentColor = PurpleAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiMetricCard(
                            title = WarehouseStrings.kpiTotalPlants(isArabic),
                            value = "${plants.size}",
                            sub = "Plants",
                            icon = Icons.Default.Apartment,
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = WarehouseStrings.kpiLowStockWarning(isArabic),
                            value = "${lowStockItems.size}",
                            sub = "Low",
                            icon = Icons.Default.Warning,
                            accentColor = Color(0xFFFBBF24),
                            isAlert = lowStockItems.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = WarehouseStrings.kpiCriticalOut(isArabic),
                            value = "${outOfStockItems.size}",
                            sub = "Crit",
                            icon = Icons.Default.ErrorOutline,
                            accentColor = CrimsonRed,
                            isAlert = outOfStockItems.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2b. Compact Dashboard View Filter Chips (All / Analytics Charts / Shift Ops)
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DashboardSegmentChip(
                        label = if (isArabic) "الكل" else "Overview",
                        isSelected = activeDashboardView == 0,
                        onClick = { activeDashboardView = 0 },
                        modifier = Modifier.weight(1f).testTag("dashboard_view_all")
                    )
                    DashboardSegmentChip(
                        label = if (isArabic) "الرسوم والدوران" else "Charts & ITR",
                        isSelected = activeDashboardView == 1,
                        onClick = { activeDashboardView = 1 },
                        modifier = Modifier.weight(1.2f).testTag("dashboard_view_charts")
                    )
                    DashboardSegmentChip(
                        label = if (isArabic) "الوردية والخطوط" else "Shift Ops",
                        isSelected = activeDashboardView == 2,
                        onClick = { activeDashboardView = 2 },
                        modifier = Modifier.weight(1f).testTag("dashboard_view_ops")
                    )
                }
            }
        }

        // 3. Analytics Charts (Turnover + Stock by Category + Donut + Matrix)
        if (activeDashboardView == 0 || activeDashboardView == 1) {
            item {
                InventoryTurnoverLineChart(
                    isArabic = isArabic,
                    dataPoints = turnoverTrendData
                )
            }
            item {
                StockLevelsByCategoryBarChart(
                    isArabic = isArabic,
                    categories = categoriesData,
                    selectedCategoryName = selectedCategoryName,
                    onSelectCategory = { selectedCategoryName = it }
                )
            }
            item {
                CategoryDistributionDonutChart(
                    isArabic = isArabic,
                    categories = categoriesData,
                    selectedCategoryName = selectedCategoryName,
                    onSelectCategory = { selectedCategoryName = it }
                )
            }
            if (activeDashboardView == 1) {
                item {
                    CategoryTurnoverMatrixCard(
                        isArabic = isArabic,
                        categories = categoriesData,
                        selectedCategoryName = selectedCategoryName,
                        onSelectCategory = { selectedCategoryName = it }
                    )
                }
            }
        }

        // 4. Quick Action Hub
        if (activeDashboardView == 0 || activeDashboardView == 2) {
            item {
                StaggeredItem(index = 2) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isArabic) "إجراءات لوجستية سريعة" else "Quick Logistics Actions",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    title = if (isArabic) "الخطوط" else "Lines",
                                    icon = Icons.Default.LinearScale,
                                    color = PurpleAccent,
                                    onClick = { onSelectTab(2) },
                                    modifier = Modifier.weight(1f)
                                )
                                ActionSquareButton(
                                    title = if (isArabic) "تصدير" else "Export",
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

            // 5. Production Lines Overview
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
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { onSelectTab(2) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.minTouchTarget(40.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "عرض الكل ←" else "View All →",
                                    color = AmberOrange,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allDistinctLines) { line ->
                                val lineItems = plantItems.filter { it.productionLines.contains(line) }
                                val linePcs = lineItems.sumOf { it.quantity }
                                val lineReqs = lineItems.sumOf { it.lineRequirements[line] ?: 0 }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .width(176.dp)
                                        .clickable { onSelectTab(2) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = line,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(text = if (isArabic) "أصناف" else "SKUs", color = TextMuted, fontSize = 9.5.sp)
                                                TabularText(text = "${lineItems.size}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp)
                                            }
                                            Column {
                                                Text(text = if (isArabic) "رصيد" else "Stock", color = TextMuted, fontSize = 9.5.sp)
                                                TabularText(text = "%,d".format(linePcs), color = AmberOrange, fontSize = 12.5.sp)
                                            }
                                            if (lineReqs > 0) {
                                                Column {
                                                    Text(text = if (isArabic) "مطلوب" else "Req", color = TextMuted, fontSize = 9.5.sp)
                                                    TabularText(text = "%,d".format(lineReqs), color = NeonCyanMuted, fontSize = 12.5.sp)
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

            // 6. Urgent Stock Alerts
            if (lowStockItems.isNotEmpty() || outOfStockItems.isNotEmpty()) {
                item {
                    StaggeredItem(index = 4) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "تنبيهات نقص المخزون العاجلة" else "Critical Stock Warnings",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                (outOfStockItems + lowStockItems).take(4).forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (item.quantity <= 0) CrimsonRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (item.quantity <= 0) CrimsonRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minTouchTarget(48.dp)
                                            .clickable { onOpenItemDetails(item) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    TabularText(
                                                        text = item.sku,
                                                        color = AmberOrange,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = item.location,
                                                            color = NeonCyanMuted,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${item.name} • Min ${item.minStockThreshold}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                TabularText(
                                                    text = "${item.quantity} ${item.unit}",
                                                    color = if (item.quantity <= 0) CrimsonRed else Color(0xFFFBBF24),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.5.sp
                                                )
                                                Text(
                                                    text = if (item.quantity <= 0) (if (isArabic) "نفد" else "OUT") else (if (isArabic) "منخفض" else "LOW"),
                                                    color = if (item.quantity <= 0) CrimsonRed else Color(0xFFFBBF24),
                                                    fontSize = 9.5.sp,
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

            // 7. Recent Shift Activity Stream
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
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { onSelectTab(3) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.minTouchTarget(40.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "سجل الحركات الكامل ←" else "Full Log →",
                                    color = AmberOrange,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            transactions.take(5).forEach { trx ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minTouchTarget(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
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
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    TabularText(
                                                        text = trx.sku,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    TabularText(
                                                        text = trx.formattedTime,
                                                        color = TextMuted,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                                Text(
                                                    text = trx.notes.ifEmpty { trx.type.name },
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.5.sp,
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
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                            if (trx.canUndo) {
                                                Text(
                                                    text = if (isArabic) "تراجع ↺" else "Undo ↺",
                                                    color = AmberOrange,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { onUndoTransaction(trx.id) }
                                                        .padding(top = 2.dp)
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
}

@Composable
private fun DashboardSegmentChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isSelected) NeonCyan else Color.Transparent,
        modifier = modifier
            .minTouchTarget(36.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)) {
            Text(
                text = label,
                color = if (isSelected) DarkNavyBase else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dense Metric Card (fixed compact height 72dp) with Tabular Numbers
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
        shape = RoundedCornerShape(12.dp),
        color = if (isAlert) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAlert) accentColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                TabularText(
                    text = value,
                    color = if (isAlert) accentColor else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = sub,
                    color = TextMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Compact Action Square Button with min 48dp touch target
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(56.dp)
            .minTouchTarget(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
