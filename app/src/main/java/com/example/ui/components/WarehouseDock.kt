package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WarehouseStrings
import com.example.ui.theme.*

@Composable
fun WarehouseFloatingActionBar(
    isArabic: Boolean,
    onOpenQuickAdd: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Orange '+' Quick Add button (Min 48dp Touch Target - Principle 8 & Optical Center - Principle 2)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AmberOrange,
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onOpenQuickAdd() }
                .testTag("floating_quick_add_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add",
                    tint = DarkNavyBase,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "إضافة" else "Add",
                    color = DarkNavyBase,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Cyan camera scan button (Primary Action with 48dp+ touch target)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NeonCyanMuted,
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1.5f)
                .heightIn(min = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onOpenScanner() }
                .testTag("floating_scanner_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Scanner",
                    tint = DarkNavyBase,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "مسح باركود" else "Scan SKU",
                    color = DarkNavyBase,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }

        // Green voice assistant button (48dp+ touch target)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EmeraldGreen,
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1.2f)
                .heightIn(min = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onOpenVoice() }
                .testTag("floating_voice_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    tint = DarkNavyBase,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isArabic) "صوتي" else "Voice",
                    color = DarkNavyBase,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun WarehouseBottomNav(
    isArabic: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("warehouse_bottom_nav"),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(0, WarehouseStrings.navDashboard(isArabic), Icons.Default.Dashboard),
                Triple(1, WarehouseStrings.navInventory(isArabic), Icons.Default.Inventory2),
                Triple(2, WarehouseStrings.navProductionLines(isArabic), Icons.Default.LinearScale),
                Triple(3, WarehouseStrings.navHistory(isArabic), Icons.Default.History),
                Triple(4, WarehouseStrings.navManagement(isArabic), Icons.Default.AdminPanelSettings)
            )

            tabs.forEach { (index, title, icon) ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .minTouchTarget(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onSelectTab(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("nav_tab_$index")
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) AmberOrange.copy(alpha = 0.22f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) AmberOrange else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AmberOrange else TextMuted
                    )
                }
            }
        }
    }
}
