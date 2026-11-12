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
import com.example.model.BinLocation
import com.example.model.WarehouseItem
import com.example.ui.theme.*

@Composable
fun LocationsScreen(
    isArabic: Boolean,
    currentPlantId: String,
    bins: List<BinLocation>,
    items: List<WarehouseItem>,
    selectedZone: String?,
    onSelectZone: (String?) -> Unit,
    onAddNewLocation: () -> Unit,
    onOpenOperation: (WarehouseItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plantBins = bins.filter { it.plantId == currentPlantId }
    val plantItems = items.filter { it.plantId == currentPlantId }
    val zones = listOf("A", "B", "C", "G", "H", "K", "L")

    val filteredBins = if (selectedZone == null) {
        plantBins
    } else {
        plantBins.filter { it.zone.equals(selectedZone, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .testTag("locations_screen"),
        contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header Title & Subtitle
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = WarehouseStrings.locationsTitle(isArabic, currentPlantId),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = WarehouseStrings.locationsSubtitle(isArabic),
                    fontSize = 12.sp,
                    color = NeonCyanMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. New Location Button
        item {
            Button(
                onClick = onAddNewLocation,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_location_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = DarkNavyBase,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = WarehouseStrings.newLocation(isArabic),
                    color = DarkNavyBase,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // 3. Zone Filters Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAll = selectedZone == null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAll) NeonCyanMuted else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectZone(null) }
                            .testTag("zone_filter_all")
                    ) {
                        Text(
                            text = WarehouseStrings.allZones(isArabic, plantBins.map { it.zone }.distinct().size),
                            color = if (isAll) DarkNavyBase else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                items(zones) { zone ->
                    val isSelected = selectedZone == zone
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonCyanMuted else DarkNavySurfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectZone(zone) }
                            .testTag("zone_filter_$zone")
                    ) {
                        Text(
                            text = WarehouseStrings.zoneLabel(isArabic, zone),
                            color = if (isSelected) DarkNavyBase else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 4. Bin Cards
        items(filteredBins, key = { it.code }) { bin ->
            val binItems = plantItems.filter { it.location.equals(bin.code, ignoreCase = true) }
            val totalPcs = binItems.sumOf { it.quantity }
            val capacityPct = (totalPcs.toFloat() / bin.maxCapacityPcs.toFloat()).coerceIn(0f, 1f)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkNavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bin_card_${bin.code}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Bin Code, Zone Badge, Total Shelf PCS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total PCS on Shelf
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkNavySurfaceVariant
                        ) {
                            Text(
                                text = "PCS %,d".format(totalPcs),
                                color = EmeraldGreenLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Zone Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberOrange.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = WarehouseStrings.zoneLabel(isArabic, bin.zone),
                                color = AmberOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Code
                        Text(
                            text = bin.code,
                            color = NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Subtitle: Plant & Stored items count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = WarehouseStrings.storedCodesCount(isArabic, currentPlantId, binItems.size),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "السعة: %d%%".format((capacityPct * 100).toInt()),
                            color = if (capacityPct > 0.9f) CoralRed else NeonCyanMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Capacity Linear Progress Bar
                    LinearProgressIndicator(
                        progress = { capacityPct },
                        color = if (capacityPct > 0.9f) CoralRed else NeonCyan,
                        trackColor = DarkNavySurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    // Stored items list inside this bin
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (binItems.isEmpty()) {
                            Text(
                                text = if (isArabic) "الرف فارغ حالياً - متاح للإيداع الذكي (Putaway)" else "Bin is currently empty - Available for Putaway",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            binItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkNavySurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Action buttons (Transfer & Issue)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Transfer
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AmberOrange,
                                                modifier = Modifier
                                                    .clickable { onOpenOperation(item, "TRANSFER") }
                                            ) {
                                                Text(
                                                    text = WarehouseStrings.transfer(isArabic),
                                                    color = DarkNavyBase,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            // Issue
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = CoralRed,
                                                modifier = Modifier
                                                    .clickable { onOpenOperation(item, "ISSUE") }
                                            ) {
                                                Text(
                                                    text = WarehouseStrings.issue(isArabic),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        // PCS Qty
                                        Text(
                                            text = "PCS %,d".format(item.quantity),
                                            color = EmeraldGreenLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        // SKU Code
                                        Text(
                                            text = item.sku,
                                            color = TextWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
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
