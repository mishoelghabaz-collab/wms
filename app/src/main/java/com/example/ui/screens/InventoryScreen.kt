package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.components.ProgressiveDisclosureSection
import com.example.ui.components.SearchableCombobox
import com.example.ui.components.StaggeredItem
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun InventoryScreen(
    isArabic: Boolean,
    currentPlantId: String,
    currentUserRole: UserRole,
    items: List<WarehouseItem>,
    selectedSkus: Set<String>,
    searchQuery: String,
    viewMode: InventoryViewMode,
    selectedLinesFilter: Set<String>,
    statusFilter: StockStatus?,
    savedFilters: List<SavedFilter>,
    activeSavedFilter: SavedFilter?,
    customLines: List<String>,
    alertBanner: String?,
    onDismissAlert: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleSelectSku: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onExportExcel: () -> Unit,
    onOpenOperation: (WarehouseItem, String) -> Unit,
    onDeleteItem: (WarehouseItem) -> Unit,
    onSetViewMode: (InventoryViewMode) -> Unit,
    onToggleLineFilter: (String) -> Unit,
    onClearLineFilter: () -> Unit,
    onApplySavedFilter: (SavedFilter) -> Unit,
    onSaveCurrentFilter: (String) -> Unit,
    onDeleteSavedFilter: (String) -> Unit,
    onOpenMaterialDetails: (WarehouseItem) -> Unit,
    onCopySku: (String) -> Unit,
    onBulkAssignLine: (List<String>, String) -> Unit,
    onBulkToggleActive: (List<String>, Boolean) -> Unit,
    onBulkSetPriority: (List<String>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plantItems = items.filter { it.plantId == currentPlantId }
    val allDistinctLines = (customLines + plantItems.flatMap { it.productionLines }).distinct()

    val filteredItems = remember(plantItems, selectedLinesFilter, statusFilter, searchQuery) {
        plantItems.filter { item ->
            val matchesLines = selectedLinesFilter.isEmpty() || item.productionLines.any { selectedLinesFilter.contains(it) }
            val matchesStatus = statusFilter == null || item.status == statusFilter
            val cleanQuery = searchQuery.trim()
            val matchesSearch = cleanQuery.isEmpty() ||
                    item.sku.contains(cleanQuery, ignoreCase = true) ||
                    item.name.contains(cleanQuery, ignoreCase = true) ||
                    item.location.contains(cleanQuery, ignoreCase = true) ||
                    item.productionLines.any { it.contains(cleanQuery, ignoreCase = true) } ||
                    item.supplier.contains(cleanQuery, ignoreCase = true) ||
                    item.customer.contains(cleanQuery, ignoreCase = true)
            matchesLines && matchesStatus && matchesSearch
        }
    }

    val canOperate = currentUserRole != UserRole.VIEWER
    val canManage = currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.SUPERVISOR

    var showSaveFilterDialog by remember { mutableStateOf(false) }
    var newFilterName by remember { mutableStateOf("") }
    var showBulkLineDialog by remember { mutableStateOf(false) }
    var bulkLineTarget by remember { mutableStateOf(allDistinctLines.firstOrNull() ?: "Volvo LFT") }
    var showLineCombobox by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
            .testTag("inventory_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 0. Real-time Search Bar (Filters by Material Name, SKU, Location, Line in real-time)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .testTag("inventory_search_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_input"),
                        placeholder = {
                            Text(
                                text = if (isArabic) "البحث باسم المادة أو كود الصنف SKU..."
                                else "Search by item name or SKU...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (isArabic) "بحث" else "Search",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .minTouchTarget(48.dp)
                                        .testTag("inventory_search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (searchQuery.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "نتائج البحث عن: \"$searchQuery\""
                                else "Results for: \"$searchQuery\"",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.12f),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "${filteredItems.size} مادة مطابقة"
                                    else "${filteredItems.size} items matched",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Alert Banner
        if (alertBanner != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyanMuted.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_plant_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = alertBanner,
                            color = NeonCyanMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onDismissAlert,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.minTouchTarget(36.dp)
                        ) {
                            Text(
                                text = WarehouseStrings.close(isArabic),
                                color = NeonCyanMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Viewer Mode Notice
        if (!canOperate) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberOrange.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isArabic) "وضع المشاهدة (Viewer): العمليات المخزنية والتعديل مقيدة"
                            else "Viewer Mode: Operations and modifications restricted",
                            color = AmberOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Saved Filters Bar & View Mode Toggle (Phase 7 & Phase 10)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = WarehouseStrings.savedViews(isArabic),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // View Mode Switcher with Min 48dp Touch Targets
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                ViewToggleButton(
                                    label = WarehouseStrings.viewCards(isArabic),
                                    icon = Icons.Default.ViewAgenda,
                                    isSelected = viewMode == InventoryViewMode.CARD,
                                    onClick = { onSetViewMode(InventoryViewMode.CARD) }
                                )
                                ViewToggleButton(
                                    label = WarehouseStrings.viewTable(isArabic),
                                    icon = Icons.Default.TableChart,
                                    isSelected = viewMode == InventoryViewMode.TABLE,
                                    onClick = { onSetViewMode(InventoryViewMode.TABLE) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Saved Filter Chips Carousel
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(savedFilters) { filter ->
                            val isActive = activeSavedFilter?.id == filter.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) AmberOrange else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isActive) AmberOrange else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .minTouchTarget(38.dp)
                                    .clickable { onApplySavedFilter(filter) }
                                    .testTag("saved_filter_${filter.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = filter.name,
                                        color = if (isActive) DarkNavyBase else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (!filter.isSystemPreset) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = if (isActive) DarkNavyBase else TextMuted,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { onDeleteSavedFilter(filter.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Save current view button
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .minTouchTarget(38.dp)
                                    .clickable { showSaveFilterDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = WarehouseStrings.saveCurrentView(isArabic), color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Multi-Line Filtering Section Prominently Above Inventory List with Searchable Combobox (Phase 6 & Principle 17)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .testTag("production_line_filter_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = WarehouseStrings.filterByLine(isArabic),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Quick Combobox trigger icon
                            IconButton(
                                onClick = { showLineCombobox = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Line Combobox", tint = AmberOrange, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (selectedLinesFilter.isNotEmpty()) {
                            Text(
                                text = if (isArabic) "مسح الفلتر" else "Clear Filter",
                                color = CrimsonRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onClearLineFilter() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allDistinctLines) { line ->
                            val isSelected = selectedLinesFilter.contains(line)
                            val count = plantItems.count { it.productionLines.contains(line) }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleLineFilter(line) },
                                label = {
                                    Text(
                                        text = "$line ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = AmberOrange,
                                    selectedLabelColor = DarkNavyBase
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) AmberOrange else MaterialTheme.colorScheme.outline,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.minTouchTarget(38.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Bulk Action Bar (when selectedSkus.isNotEmpty()) (Phase 12)
        if (selectedSkus.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = WarehouseStrings.selectedCount(isArabic, selectedSkus.size),
                                color = AmberOrange,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = WarehouseStrings.clearSelection(isArabic),
                                color = CrimsonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onClearSelection() }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { showBulkLineDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minTouchTarget(44.dp)
                            ) {
                                Text(WarehouseStrings.bulkAssignLines(isArabic), color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onBulkToggleActive(selectedSkus.toList(), true) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minTouchTarget(44.dp)
                            ) {
                                Text(if (isArabic) "تفعيل" else "Activate", color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onBulkSetPriority(selectedSkus.toList(), "URGENT") },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minTouchTarget(44.dp)
                            ) {
                                Text(if (isArabic) "عاجل" else "Urgent", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Select All & Excel Export Controls Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .minTouchTarget(44.dp)
                        .clickable { onClearSelection() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = CrimsonRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .minTouchTarget(44.dp)
                        .clickable { onSelectAll(filteredItems.map { it.sku }) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = WarehouseStrings.selectAll(isArabic, filteredItems.size),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .minTouchTarget(44.dp)
                        .clickable { onExportExcel() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = WarehouseStrings.exportCsv(isArabic),
                            color = EmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Main Items Display: Compact Table View OR Material Card View with Stagger & Progressive Disclosure
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد نتائج تطابق معايير البحث والفلترة" else "No items match search/filter criteria",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (viewMode == InventoryViewMode.TABLE) {
            // COMPACT TABLE VIEW
            item {
                CompactTableView(
                    items = filteredItems,
                    selectedSkus = selectedSkus,
                    isArabic = isArabic,
                    onToggleSelect = onToggleSelectSku,
                    onOpenDetails = onOpenMaterialDetails,
                    onCopySku = onCopySku,
                    onReceive = { onOpenOperation(it, "RECEIVE") },
                    onIssue = { onOpenOperation(it, "ISSUE") }
                )
            }
        } else {
            // MATERIAL CARD VIEW (Staggered items with Progressive Disclosure)
            itemsIndexed(filteredItems, key = { _, item -> "${item.plantId}_${item.sku}" }) { index, item ->
                StaggeredItem(index = index) {
                    MaterialCardItem(
                        item = item,
                        isSelected = selectedSkus.contains(item.sku),
                        canOperate = canOperate,
                        canManage = canManage,
                        isArabic = isArabic,
                        onToggleSelect = { onToggleSelectSku(item.sku) },
                        onOpenDetails = { onOpenMaterialDetails(item) },
                        onCopySku = onCopySku,
                        onReceive = { onOpenOperation(item, "RECEIVE") },
                        onIssue = { onOpenOperation(item, "ISSUE") },
                        onTransfer = { onOpenOperation(item, "TRANSFER") },
                        onInterPlant = { onOpenOperation(item, "INTER_PLANT") },
                        onPlan = { onOpenOperation(item, "PLAN") },
                        onDelete = { onDeleteItem(item) }
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
                    text = if (isArabic) "بحث وتصفية خط الإنتاج (Combobox):" else "Search & Filter Production Line:",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                SearchableCombobox(
                    title = if (isArabic) "خط الإنتاج المطلوب" else "Target Production Line",
                    placeholder = if (isArabic) "اختر خط الإنتاج للفلترة" else "Select line to filter",
                    items = allDistinctLines,
                    selectedItem = selectedLinesFilter.firstOrNull(),
                    itemLabel = { it },
                    itemSecondaryLabel = { line ->
                        val count = plantItems.count { it.productionLines.contains(line) }
                        if (isArabic) "$count صنف مرتبط" else "$count assigned materials"
                    },
                    onItemSelected = { line ->
                        onToggleLineFilter(line)
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

    // Save Filter Dialog
    if (showSaveFilterDialog) {
        AlertDialog(
            onDismissRequest = { showSaveFilterDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = WarehouseStrings.saveCurrentView(isArabic), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "أدخل اسماً لحفظ إعدادات الفلتر الحالية (مثل: خطوط نيسان، نواقص الوردية):"
                        else "Enter a name to save the current filter view:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFilterName,
                        onValueChange = { newFilterName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Filter Name", color = TextMuted) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFilterName.isNotBlank()) {
                            onSaveCurrentFilter(newFilterName.trim())
                            showSaveFilterDialog = false
                            newFilterName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "حفظ الفلتر" else "Save View", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveFilterDialog = false },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextMuted)
                }
            }
        )
    }

    // Bulk Assign Line Dialog
    if (showBulkLineDialog) {
        AlertDialog(
            onDismissRequest = { showBulkLineDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = if (isArabic) "ربط الأصناف المحددة بخط إنتاج" else "Assign Selected Items to Line", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "سيتم ربط ${selectedSkus.size} صنف بخط الإنتاج المختار دون مضاعفة أرصدتها الفعلية:"
                        else "Assign ${selectedSkus.size} items to selected line without duplicating physical inventory:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bulkLineTarget,
                        onValueChange = { bulkLineTarget = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Production Line", color = TextMuted) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bulkLineTarget.isNotBlank()) {
                            onBulkAssignLine(selectedSkus.toList(), bulkLineTarget.trim())
                            showBulkLineDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "تأكيد الربط الجماعي" else "Confirm Assignment", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBulkLineDialog = false },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ViewToggleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) AmberOrange else Color.Transparent,
        modifier = Modifier
            .minTouchTarget(36.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) DarkNavyBase else TextMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) DarkNavyBase else TextMuted,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CompactTableView(
    items: List<WarehouseItem>,
    selectedSkus: Set<String>,
    isArabic: Boolean,
    onToggleSelect: (String) -> Unit,
    onOpenDetails: (WarehouseItem) -> Unit,
    onCopySku: (String) -> Unit,
    onReceive: (WarehouseItem) -> Unit,
    onIssue: (WarehouseItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compact_table_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SKU", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                Text(text = if (isArabic) "الموقع" else "Loc", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                Text(text = if (isArabic) "خطوط الإنتاج" else "Lines", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
                Text(text = if (isArabic) "الرصيد الفعلي" else "Stock", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                Text(text = if (isArabic) "الحالة" else "Status", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                Text(text = if (isArabic) "إجراء سريع" else "Actions", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Table Rows with Tabular Numbers (Principle 3) & Touch Targets (Principle 8)
            items.forEach { item ->
                val isSelected = selectedSkus.contains(item.sku)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) AmberOrange.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { onOpenDetails(item) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SKU with copy
                    Row(
                        modifier = Modifier.width(130.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabularText(
                            text = item.sku,
                            color = AmberOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onCopySku(item.sku) },
                            modifier = Modifier
                                .minTouchTarget(32.dp)
                                .size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(13.dp))
                        }
                    }

                    // Location
                    Text(
                        text = item.location,
                        color = NeonCyanMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.width(70.dp)
                    )

                    // Lines
                    Text(
                        text = item.linesDisplay,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(150.dp)
                    )

                    // Stock (Tabular)
                    TabularText(
                        text = "${item.quantity} ${item.unit}",
                        color = if (item.quantity <= 0) CrimsonRed else if (item.quantity < item.minStockThreshold) Color(0xFFFBBF24) else EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.width(90.dp)
                    )

                    // Status
                    Text(
                        text = item.status.name,
                        color = when (item.status) {
                            StockStatus.NORMAL -> EmeraldGreen
                            StockStatus.LOW_STOCK -> Color(0xFFFBBF24)
                            StockStatus.CRITICAL -> CrimsonRed
                            StockStatus.OVERSTOCK -> NeonCyanMuted
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(90.dp)
                    )

                    // Quick Actions with Min 48dp Touch Targets
                    Row(
                        modifier = Modifier.width(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onReceive(item) },
                            modifier = Modifier
                                .minTouchTarget(38.dp)
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldGreen.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Receive", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onIssue(item) },
                            modifier = Modifier
                                .minTouchTarget(38.dp)
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CrimsonRed.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Issue", tint = CrimsonRed, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onOpenDetails(item) },
                            modifier = Modifier
                                .minTouchTarget(38.dp)
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberOrange.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Details", tint = AmberOrange, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }
    }
}

/**
 * Material Card with Progressive Disclosure (Principle 19),
 * Tabular Numbers (Principle 3), and Touch Targets (Principle 8)
 */
@Composable
private fun MaterialCardItem(
    item: WarehouseItem,
    isSelected: Boolean,
    canOperate: Boolean,
    canManage: Boolean,
    isArabic: Boolean,
    onToggleSelect: () -> Unit,
    onOpenDetails: () -> Unit,
    onCopySku: (String) -> Unit,
    onReceive: () -> Unit,
    onIssue: () -> Unit,
    onTransfer: () -> Unit,
    onInterPlant: () -> Unit,
    onPlan: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isSelected) AmberOrange else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
            .clickable { onOpenDetails() }
            .testTag("item_card_${item.sku}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: SKU Chip with Monospace 18sp & Dedicated Copy Button + Status + Available Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SKU + Location Tag with Min 48dp Touch Area
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Selection Checkbox
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AmberOrange,
                            checkmarkColor = DarkNavyBase,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .minTouchTarget(38.dp)
                            .size(38.dp)
                            .testTag("checkbox_${item.sku}")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Redesigned Material Code with Tabular Formatting (Principle 3)
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.testTag("sku_text_${item.sku}")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onCopySku(item.sku) },
                                modifier = Modifier
                                    .minTouchTarget(32.dp)
                                    .size(32.dp)
                                    .testTag("copy_btn_${item.sku}")
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

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.location,
                            color = NeonCyanMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }

                // Balance with explicit unit and Tabular digits
                Column(horizontalAlignment = Alignment.End) {
                    TabularText(
                        text = "${item.quantity} ${item.unit}",
                        color = when (item.status) {
                            StockStatus.NORMAL -> EmeraldGreen
                            StockStatus.LOW_STOCK -> Color(0xFFFBBF24)
                            StockStatus.CRITICAL -> CrimsonRed
                            StockStatus.OVERSTOCK -> NeonCyanMuted
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = when (item.status) {
                            StockStatus.NORMAL -> "IN STOCK"
                            StockStatus.LOW_STOCK -> "LOW STOCK"
                            StockStatus.CRITICAL -> "CRITICAL"
                            StockStatus.OVERSTOCK -> "OVERSTOCK"
                        },
                        color = when (item.status) {
                            StockStatus.NORMAL -> EmeraldGreen
                            StockStatus.LOW_STOCK -> Color(0xFFFBBF24)
                            StockStatus.CRITICAL -> CrimsonRed
                            StockStatus.OVERSTOCK -> NeonCyanMuted
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Row 2: Part Name Description (Widow-prevented with clear line height)
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Row 3: Production Lines Chips (Multi-line display e.g. CDPO + Nissan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LinearScale,
                    contentDescription = null,
                    tint = NeonCyanMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.linesDisplay,
                    color = NeonCyanMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 4: Key Metrics Badges (Tabular text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoTag(label = "Daily", value = "${item.dailyUsage}", modifier = Modifier.weight(1f))
                InfoTag(label = "Harness", value = "${item.perHarness} PCS", modifier = Modifier.weight(1f))
                InfoTag(label = "Plan", value = "${item.planTarget}/${item.planCurrent}", modifier = Modifier.weight(1.3f))
                InfoTag(label = "Coverage", value = item.coverageDays, modifier = Modifier.weight(1.1f))
            }

            // Row 5: Progressive Disclosure Section (Principle 19)
            ProgressiveDisclosureSection(
                title = if (isArabic) "مواصفات المورد والأولوية (المزيد)" else "Supplier & Priority Specs",
                icon = Icons.Default.Tune
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = if (isArabic) "المورد:" else "Supplier:", color = TextMuted, fontSize = 11.sp)
                        Text(text = item.supplier, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = if (isArabic) "الأولوية:" else "Priority:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = item.priority,
                            color = if (item.priority == "URGENT") CrimsonRed else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Min Lot: ${item.minLot} ${item.unit}", color = TextMuted, fontSize = 11.sp)
                    Text(text = item.lastDispenseTime, color = TextMuted, fontSize = 10.sp)
                }
            }

            // Row 6: Action Buttons with min 48dp Touch Targets (+ Inbound, - Issue, Transfer ⇄, Details)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onReceive,
                    enabled = canOperate,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minTouchTarget()
                        .testTag("action_receive_${item.sku}")
                ) {
                    Text(WarehouseStrings.receive(isArabic), color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                Button(
                    onClick = onIssue,
                    enabled = canOperate,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minTouchTarget()
                        .testTag("action_issue_${item.sku}")
                ) {
                    Text(WarehouseStrings.issue(isArabic), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                Button(
                    onClick = onTransfer,
                    enabled = canOperate,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minTouchTarget()
                        .testTag("action_transfer_${item.sku}")
                ) {
                    Text(WarehouseStrings.transfer(isArabic), color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .minTouchTarget()
                ) {
                    Text(if (isArabic) "التفاصيل" else "Details", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = TextMuted, fontSize = 9.sp)
            TabularText(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
