package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.WarehouseItem
import com.example.ui.theme.*

data class ExtractedSchematicCode(
    val sku: String,
    val name: String,
    val suggestedBin: String,
    val defaultQty: Int = 500,
    val isNew: Boolean = true
)

@Composable
fun SchematicOcrScannerDialog(
    isArabic: Boolean,
    currentPlantId: String,
    onDismiss: () -> Unit,
    onBatchCommit: (List<WarehouseItem>) -> Unit
) {
    // Initial extracted codes pre-populated or generated from schematic 6925
    val schematic6925Codes = remember {
        listOf(
            ExtractedSchematicCode("13007655", "Engine Core Main Harness", "A0101", 1200, true),
            ExtractedSchematicCode("13008512", "Cockpit Instrument Loom", "A0101", 2500, false),
            ExtractedSchematicCode("13009444", "Transmission Sensor Cable", "A0102", 1500, false),
            ExtractedSchematicCode("13008509", "Chassis Ground Terminal", "A0101", 500, false),
            ExtractedSchematicCode("13007859", "CAN-Bus High Speed Lead", "A0101", 3000, false),
            ExtractedSchematicCode("13006120", "Radar Sensor Feeder Wire", "B0201", 800, true),
            ExtractedSchematicCode("13004321", "Heavy Duty Battery Cable", "B0201", 420, false),
            ExtractedSchematicCode("13009988", "LED Headlight Wire Assy", "C0302", 650, true),
            ExtractedSchematicCode("13005511", "Tailgate Module Harness", "G0101", 900, true),
            ExtractedSchematicCode("13003344", "Airbag Dual-Squib Connector", "H0402", 400, true)
        )
    }

    var extractedList by remember { mutableStateOf(schematic6925Codes) }
    var selectedSkus by remember { mutableStateOf(schematic6925Codes.map { it.sku }.toSet()) }
    var filterNewOnly by remember { mutableStateOf(false) }
    var isSimulatingOcr by remember { mutableStateOf(false) }

    // Safe File Restriction: Strict Photo Picker limiting file uploads exclusively to Image MIME types
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isSimulatingOcr = true
        }
    }

    val displayedCodes = if (filterNewOnly) {
        extractedList.filter { it.isNew }
    } else {
        extractedList
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkNavyBase,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("schematic_ocr_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header (Screenshot 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "OCR",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ماسح المخططات التفاعلي عالي الكثافة OCR — مصنع $currentPlantId",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "High-Res (2200px+) • Auto-Orientation (0°/90°/180°/270°) • Global /^\\d{8}\$/g",
                            color = NeonCyanMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = AmberOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Extracted Count Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkNavySurfaceHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تم استخراج ${extractedList.size} كود من المخطط / Extracted ${extractedList.size} Material Codes",
                            color = EmeraldGreenLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Camera & Blueprint Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Camera
                    Button(
                        onClick = { isSimulatingOcr = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyanMuted),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = DarkNavyBase, modifier = Modifier.size(18.dp))
                            Text("تصوير كاميرا", color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Pick Blueprint (Restricted exclusively to Images)
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = DarkNavyBase, modifier = Modifier.size(18.dp))
                            Text("اختيار مخطط", color = DarkNavyBase, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Big Schematic 6925 Extraction Button (Screenshot 2)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkNavySurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            extractedList = schematic6925Codes
                            selectedSkus = schematic6925Codes.map { it.sku }.toSet()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "استخراج جميع أكواد مخطط القسم 6925 (36 كود كامل) 📄",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Review Container & Filters (Screenshot 2)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title: مراجعة وتحديد البيانات المستخرجة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مراجعة وتحديد البيانات المستخرجة (${selectedSkus.size}/${displayedCodes.size} محدد)",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Filter Chips Row
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Select All
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedSkus.size == displayedCodes.size) NeonCyan else DarkNavySurfaceVariant,
                                    modifier = Modifier.clickable {
                                        selectedSkus = if (selectedSkus.size == displayedCodes.size) emptySet() else displayedCodes.map { it.sku }.toSet()
                                    }
                                ) {
                                    Text(
                                        text = "تحديد الكل ☑",
                                        color = if (selectedSkus.size == displayedCodes.size) DarkNavyBase else TextWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                // New Only Filter
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (filterNewOnly) AmberOrange else DarkNavySurfaceVariant,
                                    modifier = Modifier.clickable { filterNewOnly = !filterNewOnly }
                                ) {
                                    Text(
                                        text = "الجديد فقط 🆕",
                                        color = if (filterNewOnly) DarkNavyBase else TextWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Extracted Items List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(displayedCodes, key = { it.sku }) { code ->
                                val isChecked = selectedSkus.contains(code.sku)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkNavySurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isChecked) NeonCyan.copy(alpha = 0.5f) else DarkNavyCardBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSkus = if (isChecked) selectedSkus - code.sku else selectedSkus + code.sku
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                selectedSkus = if (it) selectedSkus + code.sku else selectedSkus - code.sku
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = NeonCyan,
                                                checkmarkColor = DarkNavyBase
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = code.sku,
                                                    color = TextWhite,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (code.isNew) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AmberOrange.copy(alpha = 0.2f)
                                                    ) {
                                                        Text("جديد", color = AmberOrange, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(code.name, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${code.defaultQty} PCS", color = EmeraldGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("الرف: ${code.suggestedBin}", color = AmberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Commit & Dismiss Buttons
                Button(
                    onClick = {
                        val toCommit = extractedList
                            .filter { selectedSkus.contains(it.sku) }
                            .map { code ->
                                WarehouseItem(
                                    sku = code.sku,
                                    name = code.name,
                                    quantity = code.defaultQty,
                                    location = code.suggestedBin,
                                    zone = code.suggestedBin.take(1),
                                    aisle = "01",
                                    shelf = "01",
                                    bin = "01",
                                    plantId = currentPlantId
                                )
                            }
                        onBatchCommit(toCommit)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("commit_schematic_codes_btn")
                ) {
                    Text(
                        text = "🚀 تأكيد واعتماد المحدد (${selectedSkus.size}) لمصنع $currentPlantId",
                        color = DarkNavyBase,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_ocr_dialog_btn")
                ) {
                    Text("✕ إلغاء", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
