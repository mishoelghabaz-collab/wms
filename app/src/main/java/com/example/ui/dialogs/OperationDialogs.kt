package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.WarehouseStrings
import com.example.model.Plant
import com.example.model.WarehouseItem
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

@Composable
fun WarehouseOperationDialog(
    item: WarehouseItem,
    type: String, // "RECEIVE", "ISSUE", "TRANSFER", "PLAN", "INTER_PLANT", "CYCLE_COUNT", "EDIT_MATERIAL"
    isArabic: Boolean,
    plants: List<Plant>,
    onDismiss: () -> Unit,
    onReceive: (sku: String, qty: Int, notes: String) -> Unit,
    onIssue: (sku: String, qty: Int, notes: String) -> Unit,
    onTransfer: (sku: String, newLoc: String, notes: String) -> Unit,
    onInterPlant: (sku: String, toPlant: String, qty: Int) -> Unit,
    onUpdatePlan: (sku: String, target: Int, current: Int, line: String, block: String, daily: Int) -> Unit,
    onCycleCount: (sku: String, physicalCount: Int, reason: String) -> Unit = { _, _, _ -> },
    onEditMaterial: (sku: String, name: String, unit: String, minStock: Int, maxStock: Int, supplier: String, isActive: Boolean) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var quantityText by remember { mutableStateOf("500") }
    var notesText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf(item.location) }
    var targetPlant by remember { mutableStateOf(plants.firstOrNull { it.id != item.plantId }?.id ?: "P2") }

    // Plan data fields
    var planTargetText by remember { mutableStateOf(item.planTarget.toString()) }
    var planCurrentText by remember { mutableStateOf(item.planCurrent.toString()) }
    var lineText by remember { mutableStateOf(item.line) }
    var blockText by remember { mutableStateOf(item.block) }
    var dailyUsageText by remember { mutableStateOf(item.dailyUsage.toString()) }

    // Cycle count fields
    var physicalCountText by remember { mutableStateOf(item.quantity.toString()) }
    var cycleReasonText by remember { mutableStateOf("") }

    // Edit material fields
    var editNameText by remember { mutableStateOf(item.name) }
    var editUnitText by remember { mutableStateOf(item.unit) }
    var editMinStockText by remember { mutableStateOf(item.minStockThreshold.toString()) }
    var editMaxStockText by remember { mutableStateOf(item.maxStockCapacity.toString()) }
    var editSupplierText by remember { mutableStateOf(item.supplier) }
    var editIsActive by remember { mutableStateOf(item.isActive) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("warehouse_operation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (type) {
                            "RECEIVE" -> "توريد مخزون جديد (+)"
                            "ISSUE" -> "صرف للإنتاج (-)"
                            "TRANSFER" -> "نقل بين الرفوف (⇄)"
                            "INTER_PLANT" -> "تحويل إلى مصنع آخر 🏭"
                            "PLAN" -> "تحديث بيانات التشغيل والبلان ⚙"
                            "CYCLE_COUNT" -> "مطابقة وتسوية الجرد الفعلي 📋"
                            "EDIT_MATERIAL" -> "تعديل مواصفات الصنف ✏"
                            else -> "عملية مخزنية"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (type) {
                            "RECEIVE" -> EmeraldGreenLight
                            "ISSUE" -> CoralRed
                            "TRANSFER" -> AmberOrange
                            "CYCLE_COUNT" -> PurpleAccent
                            "EDIT_MATERIAL" -> NeonCyan
                            else -> NeonCyan
                        }
                    )
                    TabularText(
                        text = item.sku,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${item.name} • الرصيد المسجل: ${item.quantity} ${item.unit} بالرف ${item.location}",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                // Body based on operation type
                when (type) {
                    "RECEIVE", "ISSUE" -> {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("الكمية (${item.unit})", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (type == "RECEIVE") EmeraldGreen else CoralRed,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("op_quantity_field")
                        )

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("ملاحظات الوردية / أمر التشغيل", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "TRANSFER" -> {
                        OutlinedTextField(
                            value = locationText,
                            onValueChange = { locationText = it },
                            label = { Text("كود الرف الجديد (مثال: A0102)", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberOrange,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("op_location_field")
                        )

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("سبب النقل الداخلي", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "INTER_PLANT" -> {
                        Text("اختر المصنع المستهدف للتحويل:", color = TextMuted, fontSize = 12.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            plants.forEach { plant ->
                                val isSelected = plant.id == targetPlant
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkNavySurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else DarkNavyCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { targetPlant = plant.id }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(plant.id, fontWeight = FontWeight.Bold, color = if (isSelected) NeonCyan else TextWhite)
                                        Text(plant.nameAr, fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("الكمية المراد نقلها (${item.unit})", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "PLAN" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = planTargetText,
                                onValueChange = { planTargetText = it },
                                label = { Text("البلان المستهدف", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = planCurrentText,
                                onValueChange = { planCurrentText = it },
                                label = { Text("المنفذ حالياً", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lineText,
                                onValueChange = { lineText = it },
                                label = { Text("خط الإنتاج (Line)", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dailyUsageText,
                                onValueChange = { dailyUsageText = it },
                                label = { Text("الاستهلاك اليومي", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = blockText,
                            onValueChange = { blockText = it },
                            label = { Text("Block (البلوك)", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "CYCLE_COUNT" -> {
                        val physicalInt = physicalCountText.toIntOrNull() ?: item.quantity
                        val discrepancy = physicalInt - item.quantity

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkNavySurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("رصيد النظام:", color = TextMuted, fontSize = 13.sp)
                                    Text("${item.quantity} ${item.unit}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("فارق الجرد الفعلي:", color = TextMuted, fontSize = 13.sp)
                                    Text(
                                        text = if (discrepancy >= 0) "+$discrepancy ${item.unit} (زيادة)" else "$discrepancy ${item.unit} (عجز)",
                                        color = if (discrepancy >= 0) EmeraldGreenLight else CoralRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = physicalCountText,
                            onValueChange = { physicalCountText = it },
                            label = { Text("العدد الفعلي على الرف (Physical Count)", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = DarkNavyCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("cycle_count_input")
                        )

                        OutlinedTextField(
                            value = cycleReasonText,
                            onValueChange = { cycleReasonText = it },
                            label = { Text("سبب التسوية / رقم محضر الجرد", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "EDIT_MATERIAL" -> {
                        OutlinedTextField(
                            value = editNameText,
                            onValueChange = { editNameText = it },
                            label = { Text("اسم وتوصيف الصنف", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editUnitText,
                                onValueChange = { editUnitText = it },
                                label = { Text("وحدة القياس (UOM)", color = TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editSupplierText,
                                onValueChange = { editSupplierText = it },
                                label = { Text("المورد الرئيسي", color = TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editMinStockText,
                                onValueChange = { editMinStockText = it },
                                label = { Text("الحد الأدنى الآمن", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editMaxStockText,
                                onValueChange = { editMaxStockText = it },
                                label = { Text("السعة القصوى للرف", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حالة الصنف: نشط بالمستودع", color = TextWhite, fontSize = 13.sp)
                            Switch(
                                checked = editIsActive,
                                onCheckedChange = { editIsActive = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(WarehouseStrings.close(isArabic), color = TextMuted)
                    }

                    Button(
                        onClick = {
                            val qty = quantityText.toIntOrNull() ?: 0
                            when (type) {
                                "RECEIVE" -> onReceive(item.sku, qty, notesText)
                                "ISSUE" -> onIssue(item.sku, qty, notesText)
                                "TRANSFER" -> onTransfer(item.sku, locationText, notesText)
                                "INTER_PLANT" -> onInterPlant(item.sku, targetPlant, qty)
                                "PLAN" -> onUpdatePlan(
                                    item.sku,
                                    planTargetText.toIntOrNull() ?: item.planTarget,
                                    planCurrentText.toIntOrNull() ?: item.planCurrent,
                                    lineText,
                                    blockText,
                                    dailyUsageText.toIntOrNull() ?: item.dailyUsage
                                )
                                "CYCLE_COUNT" -> onCycleCount(
                                    item.sku,
                                    physicalCountText.toIntOrNull() ?: item.quantity,
                                    cycleReasonText
                                )
                                "EDIT_MATERIAL" -> onEditMaterial(
                                    item.sku,
                                    editNameText,
                                    editUnitText,
                                    editMinStockText.toIntOrNull() ?: item.minStockThreshold,
                                    editMaxStockText.toIntOrNull() ?: item.maxStockCapacity,
                                    editSupplierText,
                                    editIsActive
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (type) {
                                "RECEIVE" -> EmeraldGreen
                                "ISSUE" -> CoralRed
                                "TRANSFER" -> AmberOrange
                                "CYCLE_COUNT" -> PurpleAccent
                                else -> NeonCyanMuted
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .minTouchTarget()
                            .testTag("op_submit_btn")
                    ) {
                        Text(
                            text = if (isArabic) "تأكيد وتنفيذ الحركة" else "Confirm & Execute",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteMaterialDialog(
    item: WarehouseItem,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth(0.92f).testTag("delete_material_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تأكيد حذف الصنف من المخزن" else "Confirm Delete Material",
                        color = CoralRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "كود:", color = TextMuted, fontSize = 13.sp)
                    TabularText(
                        text = item.sku,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• ${item.name}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                if (item.quantity > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CoralRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = CoralRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⛔ حماية المخزون: لا يمكن حذف الصنف لأن رصيده الحالي ${item.quantity} ${item.unit}. يجب تصفير الرصيد أو صرفه أولاً.",
                                color = CoralRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "الرصيد الحالي 0 ${item.unit}. هل أنت متأكد من رغبتك في حذف هذا الصنف وسجله من قاعدة البيانات؟",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(WarehouseStrings.close(isArabic), color = TextMuted)
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = item.quantity <= 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralRed,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .minTouchTarget()
                            .testTag("confirm_delete_btn")
                    ) {
                        Text(
                            text = if (isArabic) "حذف نهائي" else "Delete Permanently",
                            color = if (item.quantity <= 0) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddLocationDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (zone: String, aisle: String, shelf: String, bin: String, capacity: Int) -> Unit
) {
    var zone by remember { mutableStateOf("A") }
    var aisle by remember { mutableStateOf("01") }
    var shelf by remember { mutableStateOf("03") }
    var binNumber by remember { mutableStateOf("01") }
    var capacity by remember { mutableStateOf("8000") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("add_location_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "+ إضافة موقع رف جديد بالمستودع" else "+ Add New Warehouse Bin",
                    color = EmeraldGreenLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "الكود المتولد تلقائياً:", color = TextMuted, fontSize = 13.sp)
                    TabularText(
                        text = "${zone.uppercase()}$aisle$shelf",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = zone,
                        onValueChange = { zone = it },
                        label = { Text("Zone (A, B...)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = aisle,
                        onValueChange = { aisle = it },
                        label = { Text("Aisle (01)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shelf,
                        onValueChange = { shelf = it },
                        label = { Text("Shelf (01)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("السعة القصوى (PCS)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(WarehouseStrings.close(isArabic), color = TextMuted)
                    }
                    Button(
                        onClick = {
                            onSubmit(zone, aisle, shelf, binNumber, capacity.toIntOrNull() ?: 8000)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .minTouchTarget()
                            .testTag("confirm_add_location_btn")
                    ) {
                        Text(
                            text = if (isArabic) "حفظ الموقع" else "Save Location",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddSkuDialog(
    isArabic: Boolean,
    initialSku: String = "",
    onDismiss: () -> Unit,
    onSubmit: (sku: String, name: String, qty: Int, location: String, line: String, daily: Int, plan: Int) -> Unit
) {
    var sku by remember { mutableStateOf(if (initialSku.isNotBlank()) initialSku else "13009988") }
    var name by remember { mutableStateOf("Headlight Wiring Cable Assembly") }
    var qty by remember { mutableStateOf("1500") }
    var location by remember { mutableStateOf("A0101") }
    var line by remember { mutableStateOf("volvo LFT") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("quick_add_sku_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "+ إدخال كود صنف جديد للمخزون" else "+ Quick SKU Addition",
                    color = AmberOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("كود 8 أرقام (SKU)", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_sku_input")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم وتوصيف الصنف", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("الكمية (PCS)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("الرف (مثال: A0101)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = line,
                    onValueChange = { line = it },
                    label = { Text("خط الإنتاج (Line)", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget()
                    ) {
                        Text(WarehouseStrings.close(isArabic), color = TextMuted)
                    }
                    Button(
                        onClick = {
                            onSubmit(sku, name, qty.toIntOrNull() ?: 100, location, line, 100, 2500)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .minTouchTarget()
                            .testTag("confirm_add_sku_btn")
                    ) {
                        Text(
                            text = if (isArabic) "إضافة للمخزن" else "Add to Inventory",
                            color = DarkNavyBase,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
