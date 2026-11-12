package com.example.model

enum class StockStatus {
    NORMAL,
    LOW_STOCK,
    CRITICAL,
    OVERSTOCK
}

enum class InventoryViewMode {
    CARD,
    TABLE
}

data class WarehouseItem(
    val sku: String,                      // 8-digit SKU e.g. "13008512"
    val name: String,                     // Description e.g. "Main Harness Volvo LFT"
    val quantity: Int,                    // in PCS (Available Physical Quantity)
    val unit: String = "PCS",
    val location: String,                 // e.g. "A0101"
    val zone: String = "A",               // e.g. "A"
    val aisle: String = "01",             // e.g. "01"
    val shelf: String = "01",             // e.g. "01"
    val bin: String = "01",               // e.g. "01"
    val block: String = "غير محدد",
    val line: String = "Volvo LFT",       // Primary line (legacy compatibility)
    val productionLines: List<String> = listOf("Volvo LFT"), // Multiple assigned production lines
    val lineRequirements: Map<String, Int> = emptyMap(),      // Line-specific requirement quantities
    val perHarness: Int = 0,
    val dailyUsage: Int = 0,
    val planTarget: Int = 2500,
    val planCurrent: Int = 0,
    val plantId: String = "P7",
    val minStockThreshold: Int = 800,
    val maxStockCapacity: Int = 4000,
    val lastDispenseTime: String = "لا يوجد صرف سابق",
    val lastDispenseNote: String = "مسجل في المخزن الأساسي",
    val supplier: String = "Volvo Global Logistics",
    val customer: String = "Volvo Trucks",
    val priority: String = "NORMAL",      // URGENT, HIGH, NORMAL
    val minLot: Int = 100,
    val isActive: Boolean = true,
    val warehouse: String = "WH-01",
    val reservedQuantity: Int = 0,
    val issuedQuantity: Int = 0,
    val notes: String = ""
) {
    val status: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.CRITICAL
            quantity < minStockThreshold -> StockStatus.LOW_STOCK
            quantity > maxStockCapacity -> StockStatus.OVERSTOCK
            else -> StockStatus.NORMAL
        }

    val linesDisplay: String
        get() = if (productionLines.isNotEmpty()) {
            productionLines.joinToString(" + ")
        } else {
            line
        }

    val coverageDays: String
        get() = if (dailyUsage > 0) {
            val days = quantity.toDouble() / dailyUsage
            "%.1f يوم".format(days)
        } else {
            "غير محدد"
        }
}

data class SavedFilter(
    val id: String,
    val name: String,
    val selectedLines: Set<String> = emptySet(),
    val statusFilter: StockStatus? = null,
    val plantId: String? = null,
    val isSystemPreset: Boolean = false
)

data class BinLocation(
    val code: String,            // e.g. "A0101"
    val zone: String,            // "A"
    val aisle: String,           // "01"
    val shelf: String,           // "01"
    val binNumber: String,       // "01"
    val plantId: String = "P7",
    val maxCapacityPcs: Int = 8000
)

enum class TransactionType {
    RECEIVE_INBOUND,       // توريد (+)
    ISSUE_OUTBOUND,        // صرف (-)
    RETURN_INBOUND,        // مرتجع (+)
    RELOCATE_BIN,          // نقل موقع (⇄)
    INTER_PLANT_TRANSFER,  // نقل بين المصانع
    PLAN_UPDATE,           // تحديث بلان
    CYCLE_COUNT_AUDIT,     // تسوية جرد دوري (±)
    MATERIAL_EDIT,         // تعديل مواصفات الصنف
    LINE_ASSIGNMENT_UPDATE // تحديث خطوط الإنتاج
}

data class InventoryTransaction(
    val id: String,
    val timestamp: Long,
    val formattedTime: String,
    val sku: String,
    val plantId: String,
    val toPlantId: String = "",
    val type: TransactionType,
    val quantityDelta: Int,
    val previousQty: Int,
    val newQty: Int,
    val fromLocation: String,
    val toLocation: String,
    val operator: String,
    val notes: String = "",
    val status: String = "COMPLETED",
    val canUndo: Boolean = true
)

data class Plant(
    val id: String,              // "P7", "P2", "P1"
    val nameEn: String,
    val nameAr: String,
    val isActive: Boolean = false
)

enum class UserRole {
    ADMIN,         // مدير النظام
    SUPERVISOR,    // مشرف تشغيل
    SCANNER_TECH,  // فني ماسح
    VIEWER         // مشاهد فقط
}

data class UserAccount(
    val id: String,
    val name: String,
    val role: UserRole,
    val device: String,
    val isLocked: Boolean = false
)

data class StockAlert(
    val id: String,
    val sku: String,
    val title: String,
    val message: String,
    val severity: StockStatus,
    val location: String,
    val timestamp: String
)

data class ActiveOperator(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "WMS Specialist",
    val terminalId: String = "Terminal",
    val factoryId: String = "P7",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
)

data class WarehouseOperatorProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val terminalId: String = "Terminal #01",
    val role: String = "Lead WMS Supervisor"
)

