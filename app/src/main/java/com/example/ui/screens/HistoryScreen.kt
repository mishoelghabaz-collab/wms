package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WarehouseStrings
import com.example.model.InventoryTransaction
import com.example.model.TransactionType
import com.example.ui.theme.*

@Composable
fun HistoryScreen(
    isArabic: Boolean,
    currentPlantId: String,
    transactions: List<InventoryTransaction>,
    historyFilter: TransactionType?,
    historySearch: String,
    onFilterChange: (TransactionType?) -> Unit,
    onSearchChange: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportExcel: () -> Unit,
    onUndoTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plantTrx = transactions.filter { it.plantId == currentPlantId }
    val filteredTrx = plantTrx.filter { trx ->
        val matchesType = (historyFilter == null || trx.type == historyFilter)
        val matchesQuery = historySearch.isEmpty() ||
                trx.sku.contains(historySearch, ignoreCase = true) ||
                trx.fromLocation.contains(historySearch, ignoreCase = true) ||
                trx.toLocation.contains(historySearch, ignoreCase = true) ||
                trx.operator.contains(historySearch, ignoreCase = true)
        matchesType && matchesQuery
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .testTag("history_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Header Title & Subtitle + CSV Export Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // CSV Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkNavySurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onExportCsv() }
                        .testTag("export_csv_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = WarehouseStrings.exportCsv(isArabic),
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title Column
                Column(
                    horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(
                        text = WarehouseStrings.historyTitle(isArabic, currentPlantId),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = WarehouseStrings.historySubtitle(isArabic, currentPlantId),
                        fontSize = 10.sp,
                        color = NeonCyanMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 2. Search Field
        item {
            OutlinedTextField(
                value = historySearch,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = WarehouseStrings.searchHistoryPlaceholder(isArabic),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan)
                },
                trailingIcon = {
                    if (historySearch.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkNavyCardBorder,
                    focusedContainerColor = DarkNavyCard,
                    unfocusedContainerColor = DarkNavyCard,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_field")
            )
        }

        // 3. Filter Chips: All, Inbound (+), Return (+), Issue (-), Relocate
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAll = historyFilter == null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAll) AmberOrange else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onFilterChange(null) }
                            .testTag("history_filter_all")
                    ) {
                        Text(
                            text = WarehouseStrings.allFilter(isArabic),
                            color = if (isAll) DarkNavyBase else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                item {
                    val isSelected = historyFilter == TransactionType.RECEIVE_INBOUND
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EmeraldGreen else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onFilterChange(TransactionType.RECEIVE_INBOUND) }
                            .testTag("history_filter_receive")
                    ) {
                        Text(
                            text = WarehouseStrings.receiveFilter(isArabic),
                            color = if (isSelected) DarkNavyBase else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item {
                    val isSelected = historyFilter == TransactionType.RETURN_INBOUND
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonCyan else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onFilterChange(TransactionType.RETURN_INBOUND) }
                            .testTag("history_filter_return")
                    ) {
                        Text(
                            text = WarehouseStrings.returnFilter(isArabic),
                            color = if (isSelected) DarkNavyBase else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item {
                    val isSelected = historyFilter == TransactionType.ISSUE_OUTBOUND
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CoralRed else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onFilterChange(TransactionType.ISSUE_OUTBOUND) }
                            .testTag("history_filter_issue")
                    ) {
                        Text(
                            text = WarehouseStrings.issueFilter(isArabic),
                            color = if (isSelected) Color.White else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item {
                    val isSelected = historyFilter == TransactionType.RELOCATE_BIN
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AmberOrange else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onFilterChange(TransactionType.RELOCATE_BIN) }
                            .testTag("history_filter_relocate")
                    ) {
                        Text(
                            text = WarehouseStrings.transferFilter(isArabic),
                            color = if (isSelected) DarkNavyBase else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 4. Excel Export Action Banner
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportExcel() }
                    .testTag("history_export_excel_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Excel",
                        tint = DarkNavyBase,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = WarehouseStrings.exportExcel(isArabic, filteredTrx.size),
                        color = DarkNavyBase,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Transaction Cards
        items(filteredTrx, key = { it.id }) { trx ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trx_card_${trx.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: SKU, Quantity transition, Type badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Undo button
                        if (trx.canUndo) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CoralRed.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clickable { onUndoTransaction(trx.id) }
                                    .testTag("undo_trx_${trx.id}")
                            ) {
                                Text(
                                    text = WarehouseStrings.undo(isArabic),
                                    color = CoralRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "[تم التراجع]",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Quantity delta & transition
                        Text(
                            text = "${trx.previousQty} → ${trx.newQty} PCS",
                            color = when (trx.type) {
                                TransactionType.RECEIVE_INBOUND -> EmeraldGreenLight
                                TransactionType.ISSUE_OUTBOUND -> CoralRed
                                else -> NeonCyan
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // 8-digit SKU
                        Text(
                            text = trx.sku,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Row 2: Location transition (From -> To)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (trx.type) {
                                TransactionType.RECEIVE_INBOUND -> EmeraldGreen.copy(alpha = 0.2f)
                                TransactionType.ISSUE_OUTBOUND -> CoralRed.copy(alpha = 0.2f)
                                TransactionType.RELOCATE_BIN -> AmberOrange.copy(alpha = 0.2f)
                                else -> PurpleAccent.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when (trx.type) {
                                    TransactionType.RECEIVE_INBOUND -> "توريد (+)"
                                    TransactionType.ISSUE_OUTBOUND -> "صرف (-)"
                                    TransactionType.RETURN_INBOUND -> "مرتجع (+)"
                                    TransactionType.RELOCATE_BIN -> "نقل موقع"
                                    TransactionType.INTER_PLANT_TRANSFER -> "نقل مصانع"
                                    TransactionType.PLAN_UPDATE -> "تحديث بلان"
                                    TransactionType.CYCLE_COUNT_AUDIT -> "تسوية جرد (±)"
                                    TransactionType.MATERIAL_EDIT -> "تعديل صنف"
                                    TransactionType.LINE_ASSIGNMENT_UPDATE -> "خطوط الإنتاج"
                                },
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "${trx.fromLocation} → ${trx.toLocation}",
                            color = AmberOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Row 3: Operator & Notes & Timestamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "بواسطة: ${trx.operator} • ${trx.formattedTime}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        if (trx.notes.isNotEmpty()) {
                            Text(
                                text = trx.notes,
                                color = NeonCyanMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
