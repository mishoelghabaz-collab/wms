package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WarehouseStrings
import com.example.model.*
import com.example.ui.components.SearchableCombobox
import com.example.ui.components.StaggeredItem
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun ProductionLinesScreen(
    isArabic: Boolean,
    currentPlantId: String,
    items: List<WarehouseItem>,
    customLines: List<String>,
    onOpenMaterialDetails: (WarehouseItem) -> Unit,
    onCreateProductionLine: (String) -> Unit,
    onAssignLineToMaterial: (sku: String, line: String, requirementQty: Int) -> Unit,
    onRemoveLineFromMaterial: (sku: String, line: String) -> Unit,
    onCopySku: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plantItems = items.filter { it.plantId == currentPlantId }
    val allDistinctLines = (customLines + plantItems.flatMap { it.productionLines }).distinct()

    var selectedLine by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddLineDialog by remember { mutableStateOf(false) }
    var newLineName by remember { mutableStateOf("") }
    var showLineCombobox by remember { mutableStateOf(false) }

    val filteredItems = remember(plantItems, selectedLine, searchQuery) {
        plantItems.filter { item ->
            val matchesLine = selectedLine == null || item.productionLines.contains(selectedLine)
            val matchesSearch = searchQuery.isBlank() ||
                    item.sku.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.productionLines.any { it.contains(searchQuery.trim(), ignoreCase = true) }
            matchesLine && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header Card (Staggered Item 0)
        item {
            StaggeredItem(index = 0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .testTag("production_lines_header")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = WarehouseStrings.productionLinesTitle(isArabic),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = WarehouseStrings.productionLinesSubtitle(isArabic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Button(
                                onClick = { showAddLineDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .minTouchTarget(44.dp)
                                    .testTag("add_production_line_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = DarkNavyBase, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "خط جديد" else "New Line",
                                    color = DarkNavyBase,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search within lines & Combobox Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("production_line_search_input"),
                                placeholder = {
                                    Text(WarehouseStrings.searchLinesPlaceholder(isArabic), color = TextMuted, fontSize = 12.sp)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Quick Combobox Picker Button (Principle 17)
                            IconButton(
                                onClick = { showLineCombobox = true },
                                modifier = Modifier
                                    .minTouchTarget()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Line Combobox", tint = AmberOrange)
                            }
                        }
                    }
                }
            }
        }

        // 2. Production Lines Selector Cards (Staggered Item 1)
        item {
            StaggeredItem(index = 1) {
                Column {
                    Text(
                        text = if (isArabic) "اختر خط الإنتاج لمعاينة احتياجاته:" else "Select Production Line:",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "ALL LINES" Chip
                        item {
                            val isAllSelected = selectedLine == null
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAllSelected) AmberOrange else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAllSelected) AmberOrange else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .minTouchTarget(44.dp)
                                    .clickable { selectedLine = null }
                                    .testTag("line_chip_all")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = WarehouseStrings.allLinesOption(isArabic),
                                        color = if (isAllSelected) DarkNavyBase else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAllSelected) DarkNavyBase.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        TabularText(
                                            text = "${plantItems.size}",
                                            color = if (isAllSelected) DarkNavyBase else AmberOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Individual Lines Chips with Tabular Digits
                        items(allDistinctLines) { line ->
                            val isSelected = selectedLine == line
                            val count = plantItems.count { it.productionLines.contains(line) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AmberOrange else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AmberOrange else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .minTouchTarget(44.dp)
                                    .clickable { selectedLine = if (isSelected) null else line }
                                    .testTag("line_chip_$line")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = line,
                                        color = if (isSelected) DarkNavyBase else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) DarkNavyBase.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        TabularText(
                                            text = "$count",
                                            color = if (isSelected) DarkNavyBase else NeonCyanMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
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

        // 3. Line Summary Banner (Staggered Item 2)
        item {
            StaggeredItem(index = 2) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedLine == null) {
                                if (isArabic) "عرض كافة الأصناف بجميع خطوط الإنتاج (${filteredItems.size} صنف)"
                                else "Displaying all materials across all lines (${filteredItems.size} items)"
                            } else {
                                if (isArabic) "خط الإنتاج المحدد: $selectedLine (${filteredItems.size} صنف)"
                                else "Selected Line: $selectedLine (${filteredItems.size} items)"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "مصنع $currentPlantId",
                            color = AmberOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Materials List (Staggered Items)
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد أصناف مرتبطة بهذا الفلتر" else "No materials found for this filter",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            itemsIndexed(filteredItems, key = { _, it -> "${it.plantId}_${it.sku}" }) { idx, item ->
                StaggeredItem(index = idx) {
                    ProductionLineMaterialCard(
                        item = item,
                        currentSelectedLine = selectedLine,
                        isArabic = isArabic,
                        onOpenDetails = { onOpenMaterialDetails(item) },
                        onCopySku = onCopySku,
                        onRemoveLine = { line -> onRemoveLineFromMaterial(item.sku, line) }
                    )
                }
            }
        }
    }

    // Line Combobox Dialog (Principle 17)
    if (showLineCombobox) {
        AlertDialog(
            onDismissRequest = { showLineCombobox = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (isArabic) "تصفية خطوط الإنتاج (Search Combobox):" else "Production Line Search Combobox:",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                SearchableCombobox(
                    title = if (isArabic) "خط الإنتاج" else "Production Line",
                    placeholder = if (isArabic) "ابحث واختر خط الإنتاج..." else "Search & select line...",
                    items = allDistinctLines,
                    selectedItem = selectedLine,
                    itemLabel = { it },
                    itemSecondaryLabel = { line ->
                        val count = plantItems.count { it.productionLines.contains(line) }
                        "${count} ${if (isArabic) "صنف" else "items"}"
                    },
                    onItemSelected = { line ->
                        selectedLine = line
                        showLineCombobox = false
                    },
                    isArabic = isArabic
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showLineCombobox = false },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "إغلاق" else "Close", color = TextMuted)
                }
            }
        )
    }

    // Add Production Line Dialog
    if (showAddLineDialog) {
        AlertDialog(
            onDismissRequest = { showAddLineDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = WarehouseStrings.addNewLinePrompt(isArabic),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "أدخل اسم خط الإنتاج (مثل: Nissan, CDPO, Volvo LFT, Line 3):"
                        else "Enter line name (e.g. Nissan, CDPO, Volvo LFT, Line 3):",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newLineName,
                        onValueChange = { newLineName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Line Name", color = TextMuted) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLineName.isNotBlank()) {
                            onCreateProductionLine(newLineName.trim())
                            showAddLineDialog = false
                            newLineName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "حفظ الخط" else "Save Line", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddLineDialog = false },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ProductionLineMaterialCard(
    item: WarehouseItem,
    currentSelectedLine: String?,
    isArabic: Boolean,
    onOpenDetails: () -> Unit,
    onCopySku: (String) -> Unit,
    onRemoveLine: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: SKU with Copy Button & Location Chip & Available Qty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-contrast prominent SKU
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TabularText(
                                text = item.sku,
                                color = AmberOrange,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onCopySku(item.sku) },
                                modifier = Modifier
                                    .minTouchTarget(32.dp)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy SKU",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.location,
                            color = NeonCyanMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Balance Badge with Tabular Numbers (Principle 3)
                Column(horizontalAlignment = Alignment.End) {
                    TabularText(
                        text = "${item.quantity} ${item.unit}",
                        color = if (item.quantity <= 0) CrimsonRed else if (item.quantity < item.minStockThreshold) Color(0xFFFBBF24) else EmeraldGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isArabic) "رصيد فعلي" else "Physical Stock",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Description
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Multi-line badges & Line Requirements
            Text(
                text = if (isArabic) "خطوط الإنتاج المرتبطة بالصنف:" else "Assigned Production Lines:",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.productionLines.forEach { line ->
                    val isHighlighted = currentSelectedLine == line
                    val req = item.lineRequirements[line]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHighlighted) AmberOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHighlighted) AmberOrange else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = line,
                                color = if (isHighlighted) AmberOrange else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (req != null && req > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                TabularText(
                                    text = "($req PCS)",
                                    color = NeonCyanMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Single Master Record Guarantee note & Details Action (with Min 48dp Touch Target)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "✓ سجل مخزني موحد (الرصيد غير مضاعف)" else "✓ Single master balance",
                    color = EmeraldGreen,
                    fontSize = 11.sp
                )

                TextButton(
                    onClick = onOpenDetails,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.minTouchTarget(44.dp)
                ) {
                    Text(
                        text = if (isArabic) "إدارة الخطوط والمواصفات ←" else "Manage Lines ←",
                        color = AmberOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
