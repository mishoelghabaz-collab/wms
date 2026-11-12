package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WarehouseStrings
import com.example.model.Plant
import com.example.model.StockStatus
import com.example.model.WarehouseItem
import com.example.ui.theme.*
import com.example.viewmodel.ScannerControlState

enum class ScannerOpTab {
    ADD_STOCK,
    ISSUE_STOCK,
    CHANGE_LOCATION,
    INTER_FACTORY_TRANSFER,
    VIEW_HISTORY
}

@Composable
fun AdvancedBarcodeScannerDialog(
    isArabic: Boolean,
    scannerState: ScannerControlState,
    scannedItem: WarehouseItem?,
    scannedBarcode: String,
    isTransferInFlight: Boolean,
    lastTransferSuccess: String?,
    availablePlants: List<Plant>,
    allCatalogItems: List<WarehouseItem>,
    onDismiss: () -> Unit,
    onStartScan: () -> Unit,
    onPauseScan: () -> Unit,
    onResumeScan: () -> Unit,
    onFinishSession: () -> Unit,
    onScanAgain: () -> Unit,
    onBarcodeScanned: (String) -> Boolean,
    onSelectQuickItem: (WarehouseItem) -> Unit,
    onAddQuantity: (qty: Int, destFactory: String, destLocation: String, notes: String) -> Unit,
    onIssueQuantity: (qty: Int, destinationDept: String, notes: String) -> Unit,
    onChangeLocation: (newLocation: String, notes: String) -> Unit,
    onInterFactoryTransfer: (
        sourceFactory: String,
        destFactory: String,
        transferQty: Int,
        isFullTransfer: Boolean,
        destLocation: String,
        destWarehouse: String,
        notes: String
    ) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(ScannerOpTab.INTER_FACTORY_TRANSFER) }

    // Operation Form States
    var addQtyText by remember { mutableStateOf("500") }
    var addLocationText by remember { mutableStateOf("") }
    var addNotesText by remember { mutableStateOf("") }

    var issueQtyText by remember { mutableStateOf("100") }
    var issueDeptText by remember { mutableStateOf("Line: Volvo LFT") }
    var issueNotesText by remember { mutableStateOf("") }

    var newLocationInput by remember { mutableStateOf("B0501") }
    var locationNotesText by remember { mutableStateOf("") }

    // Inter-Factory Transfer States
    var targetPlantId by remember {
        mutableStateOf(
            availablePlants.firstOrNull { it.id != (scannedItem?.plantId ?: "P7") }?.id ?: "P2"
        )
    }
    var isFullTransfer by remember { mutableStateOf(false) }
    var partialQtyText by remember { mutableStateOf("150") }
    var destLocationText by remember { mutableStateOf("A0102") }
    var destWarehouseText by remember { mutableStateOf("WH-01") }
    var transferNotesText by remember { mutableStateOf("") }
    var showTransferConfirmation by remember { mutableStateOf(false) }

    // Laser Animation (only animates when actively scanning)
    val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Sync input fields when scannedItem updates
    LaunchedEffect(scannedItem) {
        if (scannedItem != null) {
            newLocationInput = scannedItem.location
            destLocationText = scannedItem.location
            targetPlantId = availablePlants.firstOrNull { it.id != scannedItem.plantId }?.id ?: "P2"
            partialQtyText = (scannedItem.quantity / 2).coerceAtLeast(1).toString()
            showTransferConfirmation = false
        }
    }

    Dialog(
        onDismissRequest = onFinishSession,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkNavyBase,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("advanced_barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ==========================================
                // 1. Scanner Top Control Bar (SCAN, PAUSE, RESUME, FINISH)
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (scannerState) {
                            ScannerControlState.SCANNING -> EmeraldGreen.copy(alpha = 0.2f)
                            ScannerControlState.PAUSED -> AmberOrange.copy(alpha = 0.2f)
                            ScannerControlState.COMPLETED -> PurpleAccent.copy(alpha = 0.2f)
                            else -> DarkNavySurfaceVariant
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (scannerState) {
                                ScannerControlState.SCANNING -> EmeraldGreen
                                ScannerControlState.PAUSED -> AmberOrange
                                ScannerControlState.COMPLETED -> PurpleAccent
                                else -> DarkNavyCardBorder
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (scannerState) {
                                            ScannerControlState.SCANNING -> EmeraldGreen
                                            ScannerControlState.PAUSED -> AmberOrange
                                            ScannerControlState.COMPLETED -> PurpleAccent
                                            else -> TextMuted
                                        }
                                    )
                            )
                            Text(
                                text = when (scannerState) {
                                    ScannerControlState.SCANNING -> if (isArabic) "جاري المسح (SCANNING)" else "SCANNING..."
                                    ScannerControlState.PAUSED -> if (isArabic) "متوقف مؤقتاً (PAUSED)" else "PAUSED"
                                    ScannerControlState.COMPLETED -> if (isArabic) "اكتملت الحركة (DONE)" else "COMPLETED"
                                    else -> "IDLE"
                                },
                                color = when (scannerState) {
                                    ScannerControlState.SCANNING -> EmeraldGreenLight
                                    ScannerControlState.PAUSED -> AmberOrange
                                    ScannerControlState.COMPLETED -> PurpleAccent
                                    else -> TextMuted
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Scanner Action Buttons (SCAN, PAUSE, RESUME, FINISH)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (scannerState == ScannerControlState.SCANNING) {
                            FilledTonalButton(
                                onClick = onPauseScan,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = AmberOrange.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_pause_scan")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AmberOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PAUSE", fontSize = 10.sp, color = AmberOrange, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onResumeScan,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldGreen.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_resume_scan")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = EmeraldGreenLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (scannerState == ScannerControlState.IDLE) "SCAN" else "RESUME", fontSize = 10.sp, color = EmeraldGreenLight, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onFinishSession,
                            modifier = Modifier.size(32.dp).testTag("btn_finish_scan")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Finish", tint = TextMuted)
                        }
                    }
                }

                // ==========================================
                // 2. Camera Viewfinder or Compact Status Header
                // ==========================================
                if (scannedItem == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkNavyCard)
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val boxLeft = w * 0.12f
                            val boxTop = h * 0.15f
                            val boxRight = w * 0.88f
                            val boxBottom = h * 0.85f

                            val bracket = 20.dp.toPx()
                            val stroke = 3.dp.toPx()

                            // Reticle corners
                            drawLine(NeonCyan, Offset(boxLeft, boxTop), Offset(boxLeft + bracket, boxTop), stroke)
                            drawLine(NeonCyan, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + bracket), stroke)
                            drawLine(NeonCyan, Offset(boxRight, boxTop), Offset(boxRight - bracket, boxTop), stroke)
                            drawLine(NeonCyan, Offset(boxRight, boxTop), Offset(boxRight, boxTop + bracket), stroke)
                            drawLine(NeonCyan, Offset(boxLeft, boxBottom), Offset(boxLeft + bracket, boxBottom), stroke)
                            drawLine(NeonCyan, Offset(boxLeft, boxBottom), Offset(boxLeft, boxBottom - bracket), stroke)
                            drawLine(NeonCyan, Offset(boxRight, boxBottom), Offset(boxRight - bracket, boxBottom), stroke)
                            drawLine(NeonCyan, Offset(boxRight, boxBottom), Offset(boxRight - bracket, boxBottom), stroke)

                            // Active laser line
                            if (scannerState == ScannerControlState.SCANNING) {
                                val lineY = h * laserY
                                drawLine(
                                    brush = Brush.horizontalGradient(listOf(CoralRed, NeonCyan, CoralRed)),
                                    start = Offset(boxLeft, lineY),
                                    end = Offset(boxRight, lineY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkNavyBase.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (scannerState == ScannerControlState.SCANNING) "وجّه الكاميرا نحو باركود 8 أرقام (Single-Shot Lock)" else "المسح متوقف مؤقتاً",
                                color = TextWhite,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Barcode input / Search field
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = {
                            manualInput = it
                            if (it.trim().length >= 8) {
                                val success = onBarcodeScanned(it.trim())
                                if (success) manualInput = ""
                            }
                        },
                        placeholder = { Text("امسح أو اكتب كود المادة (مثال: 13008512)", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = NeonCyan) },
                        trailingIcon = {
                            if (manualInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    onBarcodeScanned(manualInput.trim())
                                    manualInput = ""
                                }) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Submit", tint = EmeraldGreenLight)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("barcode_scanner_input")
                    )

                    // Quick-Pick Catalog Items for fast warehouse testing
                    Text("أصناف المستودع المتاحة للمسح الفوري:", color = TextMuted, fontSize = 11.sp)
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filterList = allCatalogItems.filter { manualInput.isEmpty() || it.sku.contains(manualInput) }
                        items(filterList) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkNavyCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectQuickItem(item)
                                    }
                                    .testTag("quick_item_${item.sku}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.sku, color = TextWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                        Text(item.name, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${item.quantity} ${item.unit}", color = EmeraldGreenLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AmberOrange.copy(alpha = 0.2f)
                                        ) {
                                            Text(item.location, color = AmberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // 3. Scanned Material Information Card (Requirement 2)
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkNavyCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth().testTag("scanned_material_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: SKU, Status Badge, Location
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenLight, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = scannedItem.sku,
                                        color = TextWhite,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (scannedItem.status) {
                                        StockStatus.NORMAL -> EmeraldGreen.copy(alpha = 0.2f)
                                        StockStatus.LOW_STOCK -> AmberOrange.copy(alpha = 0.2f)
                                        StockStatus.CRITICAL -> CoralRed.copy(alpha = 0.2f)
                                        StockStatus.OVERSTOCK -> PurpleAccent.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = scannedItem.status.name,
                                        color = when (scannedItem.status) {
                                            StockStatus.NORMAL -> EmeraldGreenLight
                                            StockStatus.LOW_STOCK -> AmberOrange
                                            StockStatus.CRITICAL -> CoralRed
                                            StockStatus.OVERSTOCK -> PurpleAccent
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Description
                            Text(
                                text = scannedItem.name,
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Specs Grid: Factory, Warehouse, Location, Available Qty, Reserved, Issued, UOM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SpecPill("المصنع", scannedItem.plantId, NeonCyan, Modifier.weight(1f))
                                SpecPill("المخزن", scannedItem.warehouse, TextWhite, Modifier.weight(1f))
                                SpecPill("الموقع", scannedItem.location, AmberOrange, Modifier.weight(1f))
                                SpecPill("المتاح", "${scannedItem.quantity} ${scannedItem.unit}", EmeraldGreenLight, Modifier.weight(1.3f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SpecPill("محجوز للبلان", "${scannedItem.reservedQuantity} ${scannedItem.unit}", TextMuted, Modifier.weight(1f))
                                SpecPill("إجمالي المنصرف", "${scannedItem.issuedQuantity} ${scannedItem.unit}", CoralRed, Modifier.weight(1f))
                                SpecPill("الخط", scannedItem.line, PurpleAccent, Modifier.weight(1f))
                            }
                        }
                    }

                    // Success Feedback Alert Banner (if completed)
                    if (lastTransferSuccess != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = EmeraldGreenLight, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = lastTransferSuccess,
                                        color = EmeraldGreenLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 4. Operation Selector Tabs
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OpTabButton("🏭 تحويل مصانع", ScannerOpTab.INTER_FACTORY_TRANSFER, selectedTab, Modifier.weight(1.2f)) { selectedTab = it; showTransferConfirmation = false }
                        OpTabButton("+ توريد", ScannerOpTab.ADD_STOCK, selectedTab, Modifier.weight(1f)) { selectedTab = it; showTransferConfirmation = false }
                        OpTabButton("- صرف", ScannerOpTab.ISSUE_STOCK, selectedTab, Modifier.weight(1f)) { selectedTab = it; showTransferConfirmation = false }
                        OpTabButton("⇄ موقع", ScannerOpTab.CHANGE_LOCATION, selectedTab, Modifier.weight(0.9f)) { selectedTab = it; showTransferConfirmation = false }
                    }

                    // ==========================================
                    // 5. Operation Content & Workflow Forms
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedTab) {
                            // ----------------------------------------------------
                            // Operation A: Inter-Factory Transfer (Requirements 6, 7, 8)
                            // ----------------------------------------------------
                            ScannerOpTab.INTER_FACTORY_TRANSFER -> {
                                Text("إجراء تحويل بين المصانع (Inter-Factory Transfer):", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                // Source & Destination Selection Card
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkNavyCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Source (Read-only)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("مصنع المصدر (Source):", color = TextMuted, fontSize = 12.sp)
                                            Text("مصنع ${scannedItem.plantId} (${scannedItem.warehouse} • ${scannedItem.location})", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Divider(color = DarkNavyCardBorder)

                                        // Destination Factory Picker
                                        Text("اختر مصنع الوجهة (Destination Factory):", color = TextMuted, fontSize = 11.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            availablePlants.filter { it.id != scannedItem.plantId }.forEach { plant ->
                                                val isSelected = targetPlantId == plant.id
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkNavySurfaceVariant,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else DarkNavyCardBorder),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { targetPlantId = plant.id }
                                                        .testTag("target_plant_${plant.id}")
                                                ) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(plant.id, color = if (isSelected) NeonCyan else TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                                        Text(plant.nameAr, color = TextMuted, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // Destination Location Input
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(
                                                value = destLocationText,
                                                onValueChange = { destLocationText = it },
                                                label = { Text("رف الوجهة بالمصنع الجديد", fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = destWarehouseText,
                                                onValueChange = { destWarehouseText = it },
                                                label = { Text("مخزن الوجهة", fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // Transfer Mode Toggle: Full vs Partial
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isFullTransfer) EmeraldGreen.copy(alpha = 0.2f) else DarkNavySurfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isFullTransfer) EmeraldGreen else DarkNavyCardBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { isFullTransfer = true }
                                                    .testTag("tab_full_transfer")
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("تحويل كامل الكمية", color = if (isFullTransfer) EmeraldGreenLight else TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("${scannedItem.quantity} ${scannedItem.unit}", color = TextMuted, fontSize = 10.sp)
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (!isFullTransfer) NeonCyan.copy(alpha = 0.2f) else DarkNavySurfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (!isFullTransfer) NeonCyan else DarkNavyCardBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { isFullTransfer = false }
                                                    .testTag("tab_partial_transfer")
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("تحويل كمية جزئية", color = if (!isFullTransfer) NeonCyan else TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("تحديد الكمية", color = TextMuted, fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        // Partial Quantity Input
                                        if (!isFullTransfer) {
                                            OutlinedTextField(
                                                value = partialQtyText,
                                                onValueChange = { partialQtyText = it },
                                                label = { Text("الكمية الجزئية المراد تحويلها (${scannedItem.unit})", fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("transfer_partial_qty_input")
                                            )
                                        }

                                        OutlinedTextField(
                                            value = transferNotesText,
                                            onValueChange = { transferNotesText = it },
                                            label = { Text("ملاحظات أمر النقل / بوليصة الشحن الداخلي", fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // ----------------------------------------------------
                                // Step 8: Confirmation Screen Review Card (Mandatory Requirement 7)
                                // ----------------------------------------------------
                                val currentTransferQty = if (isFullTransfer) scannedItem.quantity else (partialQtyText.toIntOrNull() ?: 0)
                                val remainingSource = scannedItem.quantity - currentTransferQty
                                val destExistingItem = allCatalogItems.find { it.sku == scannedItem.sku && it.plantId == targetPlantId }
                                val expectedDest = (destExistingItem?.quantity ?: 0) + currentTransferQty

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PurpleAccent.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent),
                                    modifier = Modifier.fillMaxWidth().testTag("transfer_summary_card")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("📋 ملخص واعتماد التحويل النهائي:", color = PurpleAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("مسار التحويل:", color = TextMuted, fontSize = 11.sp)
                                            Text("مصنع ${scannedItem.plantId} ➔ مصنع $targetPlantId", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الكمية المحولة:", color = TextMuted, fontSize = 11.sp)
                                            Text("$currentTransferQty ${scannedItem.unit}", color = AmberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("المتبقي بمصنع ${scannedItem.plantId}:", color = TextMuted, fontSize = 11.sp)
                                            Text("$remainingSource ${scannedItem.unit}", color = if (remainingSource >= 0) EmeraldGreenLight else CoralRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الرصيد المتوقع بمصنع $targetPlantId:", color = TextMuted, fontSize = 11.sp)
                                            Text("$expectedDest ${scannedItem.unit}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Large CONFIRM TRANSFER Button (Idempotent Execution)
                                Button(
                                    onClick = {
                                        onInterFactoryTransfer(
                                            scannedItem.plantId,
                                            targetPlantId,
                                            currentTransferQty,
                                            isFullTransfer,
                                            destLocationText.ifBlank { scannedItem.location },
                                            destWarehouseText.ifBlank { "WH-01" },
                                            transferNotesText
                                        )
                                    },
                                    enabled = !isTransferInFlight && currentTransferQty > 0 && currentTransferQty <= scannedItem.quantity,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        disabledContainerColor = DarkNavySurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_confirm_transfer")
                                ) {
                                    if (isTransferInFlight) {
                                        CircularProgressIndicator(color = DarkNavyBase, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري التنفيذ الذري...", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = DarkNavyBase)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isArabic) "تأكيد وتنفيذ التحويل الذري (CONFIRM TRANSFER)" else "CONFIRM TRANSFER",
                                            color = DarkNavyBase,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // ----------------------------------------------------
                            // Operation B: Add Quantity (Inbound Receive)
                            // ----------------------------------------------------
                            ScannerOpTab.ADD_STOCK -> {
                                Text("إضافة كمية للمخزون (Add Quantity / Receive):", color = EmeraldGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = addQtyText,
                                    onValueChange = { addQtyText = it },
                                    label = { Text("الكمية المضافة (${scannedItem.unit})") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("add_qty_input")
                                )
                                OutlinedTextField(
                                    value = addNotesText,
                                    onValueChange = { addNotesText = it },
                                    label = { Text("رقم إذن الاستلام / المورد") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val qty = addQtyText.toIntOrNull() ?: 0
                                        onAddQuantity(qty, scannedItem.plantId, scannedItem.location, addNotesText)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_confirm_add")
                                ) {
                                    Text("تأكيد التوريد والإضافة (+)", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                                }
                            }

                            // ----------------------------------------------------
                            // Operation C: Remove or Issue Quantity
                            // ----------------------------------------------------
                            ScannerOpTab.ISSUE_STOCK -> {
                                Text("صرف كمية من المخزون (Remove / Issue):", color = CoralRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = issueQtyText,
                                    onValueChange = { issueQtyText = it },
                                    label = { Text("الكمية المصروفة (${scannedItem.unit})") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("issue_qty_input")
                                )
                                OutlinedTextField(
                                    value = issueDeptText,
                                    onValueChange = { issueDeptText = it },
                                    label = { Text("جهة الاستلام / خط الإنتاج") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val issueInt = issueQtyText.toIntOrNull() ?: 0
                                if (issueInt > scannedItem.quantity) {
                                    Text("⛔ تنبيه: الكمية المطلوبة تتجاوز الرصيد المتاح (${scannedItem.quantity})", color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onIssueQuantity(issueInt, issueDeptText, issueNotesText)
                                    },
                                    enabled = issueInt > 0 && issueInt <= scannedItem.quantity,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralRed,
                                        disabledContainerColor = DarkNavySurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_confirm_issue")
                                ) {
                                    Text("تأكيد الصرف للإنتاج (-)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // ----------------------------------------------------
                            // Operation D: Change Material Location
                            // ----------------------------------------------------
                            ScannerOpTab.CHANGE_LOCATION -> {
                                Text("تغيير موقع الرف بالمصنع (Relocate Bin):", color = AmberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("الموقع الحالي: ${scannedItem.location}", color = TextMuted, fontSize = 11.sp)
                                OutlinedTextField(
                                    value = newLocationInput,
                                    onValueChange = { newLocationInput = it },
                                    label = { Text("كود الرف الجديد (مثال: B0501)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("relocate_location_input")
                                )

                                Button(
                                    onClick = {
                                        onChangeLocation(newLocationInput, locationNotesText)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_confirm_relocate")
                                ) {
                                    Text("تأكيد نقل موقع الرف (⇄)", color = DarkNavyBase, fontWeight = FontWeight.Bold)
                                }
                            }

                            ScannerOpTab.VIEW_HISTORY -> {}
                        }
                    }

                    // ==========================================
                    // 6. Bottom Scanner Session Action Buttons (SCAN AGAIN & FINISH)
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onFinishSession,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("btn_finish_session")
                        ) {
                            Text("إنهاء الجلسة (FINISH)", color = TextMuted, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onScanAgain,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f).height(46.dp).testTag("btn_scan_again")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = DarkNavyBase)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح جديد (SCAN AGAIN)", color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecPill(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = DarkNavySurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextMuted, fontSize = 9.sp, maxLines = 1)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun OpTabButton(
    title: String,
    tab: ScannerOpTab,
    selected: ScannerOpTab,
    modifier: Modifier = Modifier,
    onClick: (ScannerOpTab) -> Unit
) {
    val isSelected = tab == selected
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkNavySurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else DarkNavyCardBorder),
        modifier = modifier.clickable { onClick(tab) }
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
        )
    }
}
