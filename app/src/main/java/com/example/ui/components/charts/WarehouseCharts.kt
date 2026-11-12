package com.example.ui.components.charts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Data model for category stock analytics
 */
data class CategoryStockData(
    val categoryName: String,
    val localizedName: String,
    val totalStock: Int,
    val totalCapacity: Int,
    val minThreshold: Int,
    val itemCount: Int,
    val issuedQuantity: Int,
    val dailyUsage: Int,
    val turnoverRatio: Float,
    val color: Color
)

/**
 * Data model for turnover trend data points
 */
data class TurnoverTimePoint(
    val label: String,
    val turnoverRate: Float,
    val inboundQty: Int,
    val outboundQty: Int,
    val avgStockQty: Int
)

/**
 * Interactive Recharts-style Area & Line Chart for Inventory Turnover
 */
@Composable
fun InventoryTurnoverLineChart(
    isArabic: Boolean,
    dataPoints: List<TurnoverTimePoint>,
    targetBenchmark: Float = 4.5f,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxTurnover = max(targetBenchmark * 1.4f, (dataPoints.maxOfOrNull { it.turnoverRate } ?: 5f) * 1.25f)
    val minTurnover = 0f

    // Animated progress for line drawing
    val transitionProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("inventory_turnover_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with metrics and benchmark badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "معدل دوران المخزون (Turnover Ratio)" else "Inventory Turnover Ratio (ITR)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isArabic) "معدل استهلاك الرصيد الفعلي مقارنة بالمستهدف" else "Stock velocity & consumption vs 4.5x benchmark",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isArabic) "الهدف: ${targetBenchmark}x" else "Target: ${targetBenchmark}x",
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Chart Canvas (Area + Line + Dots + Benchmark + Scrubber)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                                    selectedIndex = index
                                }
                            )
                        }
                        .pointerInput(dataPoints) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                                    selectedIndex = index
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                    val index = ((change.position.x + stepX / 2) / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                                    selectedIndex = index
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 24.dp.toPx()
                    val chartHeight = height - paddingBottom
                    val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

                    // 1. Draw subtle horizontal grid lines (0x, 2x, 4x, 6x)
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = chartHeight * (1f - i.toFloat() / gridLines)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.18f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // 2. Draw Target Benchmark line (Emerald dashed)
                    val benchmarkY = chartHeight * (1f - (targetBenchmark - minTurnover) / (maxTurnover - minTurnover))
                    drawLine(
                        color = EmeraldGreen.copy(alpha = 0.6f),
                        start = Offset(0f, benchmarkY),
                        end = Offset(width, benchmarkY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // 3. Compute spline path points
                    val points = dataPoints.mapIndexed { index, dp ->
                        val x = index * stepX
                        val normalizedVal = (dp.turnoverRate - minTurnover) / (maxTurnover - minTurnover)
                        val y = chartHeight * (1f - normalizedVal * transitionProgress.value)
                        Offset(x, y)
                    }

                    if (points.size > 1) {
                        // Smooth cubic bezier spline
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                            }
                        }

                        // Fill under area with linear gradient
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, chartHeight)
                            lineTo(points.first().x, chartHeight)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.35f),
                                    NeonCyan.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Draw main line curve
                        drawPath(
                            path = strokePath,
                            color = NeonCyan,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 4. Draw data point circles & selected scrubber highlight
                        points.forEachIndexed { index, pt ->
                            val isSelected = selectedIndex == index
                            if (isSelected) {
                                // Scrubber vertical line
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.5f),
                                    start = Offset(pt.x, 0f),
                                    end = Offset(pt.x, chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                // Halo circle
                                drawCircle(
                                    color = NeonCyan.copy(alpha = 0.3f),
                                    radius = 12.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = NeonCyan,
                                    radius = 6.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = pt
                                )
                            } else {
                                drawCircle(
                                    color = Color(0xFF1E293B),
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = NeonCyan,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }

            // X-Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEachIndexed { index, dp ->
                    val isSelected = selectedIndex == index
                    Text(
                        text = dp.label,
                        color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { selectedIndex = index }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Interactive Tooltip Card on Selected Point (Recharts-style tooltip)
            AnimatedVisibility(
                visible = selectedIndex != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedIndex?.let { idx ->
                    val point = dataPoints.getOrNull(idx) ?: return@let
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "الفترة: ${point.label}" else "Period: ${point.label}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isArabic)
                                        "صرف: %,d PCS • توريد: %,d PCS".format(point.outboundQty, point.inboundQty)
                                    else
                                        "Issued: %,d • Inbound: %,d PCS".format(point.outboundQty, point.inboundQty),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TabularText(
                                        text = "%.2fx".format(point.turnoverRate),
                                        color = if (point.turnoverRate >= targetBenchmark) EmeraldGreen else AmberOrange,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (point.turnoverRate >= targetBenchmark) Icons.Default.TrendingUp else Icons.Default.TrendingFlat,
                                        contentDescription = null,
                                        tint = if (point.turnoverRate >= targetBenchmark) EmeraldGreen else AmberOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (point.turnoverRate >= targetBenchmark)
                                        (if (isArabic) "معدل دوران ممتاز" else "High Velocity")
                                    else
                                        (if (isArabic) "معدل دوران متوسط" else "Moderate Velocity"),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Recharts-style Bar Chart: Stock Levels by Category
 */
@Composable
fun StockLevelsByCategoryBarChart(
    isArabic: Boolean,
    categories: List<CategoryStockData>,
    selectedCategoryName: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val maxCapacity = max(1000, categories.maxOfOrNull { max(it.totalStock, it.totalCapacity) } ?: 5000)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("stock_by_category_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AmberOrange)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "مستويات المخزون حسب الفئات" else "Stock Levels by Category",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isArabic) "الرصيد الفعلي (PCS) والحدود التخزينية لكل فئة" else "Physical stock (PCS) & capacity utilization",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                if (selectedCategoryName != null) {
                    TextButton(
                        onClick = { onSelectCategory(null) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.minTouchTarget(36.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء التحديد ✕" else "Reset ✕",
                            color = AmberOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bars List (Horizontal Recharts Bar Chart for warehouse tablets & phones)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryName == cat.categoryName
                    val stockRatio = (cat.totalStock.toFloat() / maxCapacity).coerceIn(0f, 1f)
                    val thresholdRatio = (cat.minThreshold.toFloat() / maxCapacity).coerceIn(0f, 1f)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, cat.color.copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCategory(if (isSelected) null else cat.categoryName)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                            // Category Label & Values Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) cat.localizedName else cat.categoryName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${cat.itemCount} ${if (isArabic) "أصناف" else "SKUs"})",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TabularText(
                                        text = "%,d".format(cat.totalStock),
                                        color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " PCS",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Dual Bar Canvas (Capacity Track + Minimum Threshold Marker + Physical Stock Bar)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // Background Track (Max Capacity)
                                    drawRoundRect(
                                        color = Color.Gray.copy(alpha = 0.15f),
                                        size = Size(w, h),
                                        cornerRadius = CornerRadius(h / 2, h / 2)
                                    )

                                    // Physical Stock Bar
                                    val barWidth = w * stockRatio
                                    if (barWidth > 0f) {
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    cat.color.copy(alpha = 0.75f),
                                                    cat.color
                                                )
                                            ),
                                            size = Size(barWidth, h),
                                            cornerRadius = CornerRadius(h / 2, h / 2)
                                        )
                                    }

                                    // Minimum Threshold Indicator Marker (Red vertical notch)
                                    val thresholdX = w * thresholdRatio
                                    if (thresholdX in 0f..w) {
                                        drawLine(
                                            color = CrimsonRed,
                                            start = Offset(thresholdX, -2.dp.toPx()),
                                            end = Offset(thresholdX, h + 2.dp.toPx()),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 6.dp)
                            .background(NeonCyan, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "المخزون الفعلي" else "Actual Stock",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(2.dp, 10.dp)
                            .background(CrimsonRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "حد الأمان الأدنى (Min)" else "Min Safety Threshold",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 6.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "السعة القصوى" else "Max Capacity",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Interactive Donut / Radial Chart: Category Distribution
 */
@Composable
fun CategoryDistributionDonutChart(
    isArabic: Boolean,
    categories: List<CategoryStockData>,
    selectedCategoryName: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val totalStock = categories.sumOf { it.totalStock }.coerceAtLeast(1)
    val selectedCategory = categories.firstOrNull { it.categoryName == selectedCategoryName }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("category_distribution_donut_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "توزيع الرصيد النسبي بين الفئات" else "Category Stock Distribution",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart with Center KPI Hole
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        val arcRadius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        var startAngle = -90f

                        categories.forEach { cat ->
                            val sweepAngle = (cat.totalStock.toFloat() / totalStock) * 360f
                            val isSelected = selectedCategoryName == cat.categoryName
                            val effectiveStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                            drawArc(
                                color = cat.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle.coerceAtLeast(2f) - 2f, // 2deg gap
                                useCenter = false,
                                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                                size = Size(arcRadius * 2, arcRadius * 2),
                                style = Stroke(width = effectiveStroke, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center KPI text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TabularText(
                            text = if (selectedCategory != null) "%,d".format(selectedCategory.totalStock) else "%,d".format(totalStock),
                            color = selectedCategory?.color ?: MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (selectedCategory != null) (if (isArabic) "قطعة محددة" else "PCS Selected") else (if (isArabic) "إجمالي القطع" else "Total PCS"),
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Legend List with Percentages
                Column(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(5).forEach { cat ->
                        val isSelected = selectedCategoryName == cat.categoryName
                        val percentage = (cat.totalStock.toFloat() / totalStock * 100).roundToInt()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    onSelectCategory(if (isSelected) null else cat.categoryName)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(cat.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) cat.localizedName else cat.categoryName,
                                color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 110.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$percentage%",
                                color = TextMuted,
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

/**
 * Category Turnover Velocity Matrix & Health Card
 */
@Composable
fun CategoryTurnoverMatrixCard(
    isArabic: Boolean,
    categories: List<CategoryStockData>,
    selectedCategoryName: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("category_turnover_matrix_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "مصفوفة سرعة الدوران وصحة المخزون" else "Turnover Velocity & Inventory Health",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${categories.size} ${if (isArabic) "فئات" else "Categories"}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryName == cat.categoryName
                    val isHighVelocity = cat.turnoverRatio >= 4.5f
                    val isLowVelocity = cat.turnoverRatio < 2.5f

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory(if (isSelected) null else cat.categoryName) }
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
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) cat.localizedName else cat.categoryName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isArabic)
                                            "${cat.itemCount} أصناف • رصيد: %,d PCS".format(cat.totalStock)
                                        else
                                            "${cat.itemCount} SKUs • Stock: %,d PCS".format(cat.totalStock),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TabularText(
                                        text = "%.1fx".format(cat.turnoverRatio),
                                        color = when {
                                            isHighVelocity -> EmeraldGreen
                                            isLowVelocity -> AmberOrange
                                            else -> NeonCyan
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ yr",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when {
                                        isHighVelocity -> EmeraldGreen.copy(alpha = 0.15f)
                                        isLowVelocity -> AmberOrange.copy(alpha = 0.15f)
                                        else -> NeonCyan.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            isHighVelocity -> if (isArabic) "حركة سريعة" else "Fast Moving"
                                            isLowVelocity -> if (isArabic) "حركة بطيئة" else "Slow Moving"
                                            else -> if (isArabic) "حركة متوازنة" else "Balanced"
                                        },
                                        color = when {
                                            isHighVelocity -> EmeraldGreen
                                            isLowVelocity -> AmberOrange
                                            else -> NeonCyan
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

