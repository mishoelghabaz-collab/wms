package com.example.data

import com.example.model.*

object WarehouseMockData {

    fun initialPlants(): List<Plant> = listOf(
        Plant(id = "P7", nameEn = "Plant 7 - Main Assembly", nameAr = "مصنع 7", isActive = true),
        Plant(id = "P2", nameEn = "Plant 2 - Stamping & Foam", nameAr = "مصنع 2", isActive = false),
        Plant(id = "P1", nameEn = "Plant 1 - Sub-assemblies", nameAr = "مصنع 1", isActive = false)
    )

    fun initialBins(): List<BinLocation> = listOf(
        BinLocation(code = "A0101", zone = "A", aisle = "01", shelf = "01", binNumber = "01", plantId = "P7", maxCapacityPcs = 8000),
        BinLocation(code = "A0102", zone = "A", aisle = "01", shelf = "01", binNumber = "02", plantId = "P7", maxCapacityPcs = 8000),
        BinLocation(code = "B0201", zone = "B", aisle = "02", shelf = "01", binNumber = "01", plantId = "P7", maxCapacityPcs = 6000),
        BinLocation(code = "C0302", zone = "C", aisle = "03", shelf = "02", binNumber = "02", plantId = "P7", maxCapacityPcs = 7000),
        BinLocation(code = "G0101", zone = "G", aisle = "01", shelf = "01", binNumber = "01", plantId = "P7", maxCapacityPcs = 5000),
        BinLocation(code = "H0402", zone = "H", aisle = "04", shelf = "02", binNumber = "02", plantId = "P7", maxCapacityPcs = 5000),
        BinLocation(code = "K0101", zone = "K", aisle = "01", shelf = "01", binNumber = "01", plantId = "P7", maxCapacityPcs = 6000)
    )

    fun initialSavedFilters(): List<SavedFilter> = listOf(
        SavedFilter(id = "FLT-ALL", name = "كل الأصناف", selectedLines = emptySet(), isSystemPreset = true),
        SavedFilter(id = "FLT-VOLVO", name = "Volvo LFT", selectedLines = setOf("Volvo LFT"), isSystemPreset = true),
        SavedFilter(id = "FLT-MULTI", name = "CDPO + Nissan", selectedLines = setOf("CDPO", "Nissan"), isSystemPreset = true),
        SavedFilter(id = "FLT-LOW", name = "نواقص المخزون", statusFilter = StockStatus.LOW_STOCK, isSystemPreset = true)
    )

    fun initialProductionLines(): List<String> = listOf(
        "Volvo LFT",
        "CDPO",
        "Nissan",
        "Line 1",
        "Line 2",
        "Scania Truck",
        "Mercedes Sprinter"
    )

    fun initialItems(): List<WarehouseItem> = listOf(
        WarehouseItem(
            sku = "13008512",
            name = "Main Cockpit Wire Harness",
            quantity = 2500,
            location = "A0101",
            zone = "A",
            aisle = "01",
            shelf = "01",
            bin = "01",
            block = "Block 1",
            line = "Volvo LFT",
            productionLines = listOf("Volvo LFT", "CDPO"),
            lineRequirements = mapOf("Volvo LFT" to 1500, "CDPO" to 1000),
            perHarness = 1,
            dailyUsage = 180,
            planTarget = 2500,
            planCurrent = 1400,
            plantId = "P7",
            minStockThreshold = 1000,
            maxStockCapacity = 4500,
            customer = "Volvo Trucks",
            priority = "URGENT",
            minLot = 100,
            lastDispenseTime = "لا يوجد صرف سابق",
            lastDispenseNote = "مسجل في المخزن الرئيسي",
            notes = "ضفيرة رئيسية متعددة الاستخدام لخط فولفو وCDPO"
        ),
        WarehouseItem(
            sku = "13009444",
            name = "Engine Bay Sensor Lead Loom",
            quantity = 2500,
            location = "A0102",
            zone = "A",
            aisle = "01",
            shelf = "01",
            bin = "02",
            block = "Block 1",
            line = "Volvo LFT",
            productionLines = listOf("Volvo LFT", "Nissan"),
            lineRequirements = mapOf("Volvo LFT" to 1800, "Nissan" to 700),
            perHarness = 2,
            dailyUsage = 150,
            planTarget = 2500,
            planCurrent = 1200,
            plantId = "P7",
            minStockThreshold = 1000,
            maxStockCapacity = 4000,
            customer = "Nissan & Volvo",
            priority = "HIGH",
            minLot = 50,
            lastDispenseTime = "2026-11-08 02:15",
            lastDispenseNote = "صرف خط إنتاج فولفو 500 قطعة",
            notes = "حساسات حرارة محرك مشتركة"
        ),
        WarehouseItem(
            sku = "13008509",
            name = "Grounding Terminal Connector Block",
            quantity = 500,
            location = "A0101",
            zone = "A",
            aisle = "01",
            shelf = "01",
            bin = "01",
            block = "Block 2",
            line = "Volvo LFT",
            productionLines = listOf("Volvo LFT"),
            lineRequirements = mapOf("Volvo LFT" to 500),
            perHarness = 4,
            dailyUsage = 80,
            planTarget = 2500,
            planCurrent = 1900,
            plantId = "P7",
            minStockThreshold = 800,
            maxStockCapacity = 3000,
            customer = "Volvo Trucks",
            priority = "URGENT",
            minLot = 200,
            lastDispenseTime = "2026-11-07 18:40",
            lastDispenseNote = "تغذية وردية مسائية",
            notes = "صنف منخفض الرصيد يتطلب إعادة طلب فوري"
        ),
        WarehouseItem(
            sku = "13007859",
            name = "CAN-Bus Twisted Cable 4-Pin",
            quantity = 3000,
            location = "A0101",
            zone = "A",
            aisle = "01",
            shelf = "01",
            bin = "01",
            block = "Block 1",
            line = "CDPO",
            productionLines = listOf("CDPO", "Nissan", "Line 1"),
            lineRequirements = mapOf("CDPO" to 1200, "Nissan" to 1000, "Line 1" to 800),
            perHarness = 1,
            dailyUsage = 120,
            planTarget = 3000,
            planCurrent = 2800,
            plantId = "P7",
            minStockThreshold = 900,
            maxStockCapacity = 5000,
            customer = "Renault Nissan Alliance",
            priority = "NORMAL",
            minLot = 100,
            lastDispenseTime = "2026-11-06 14:10",
            lastDispenseNote = "توريد شحنة مورد 1000 قطعة",
            notes = "كابل نقل بيانات عالي السرعة"
        ),
        WarehouseItem(
            sku = "13004321",
            name = "Heavy Duty Battery Cable 50mm²",
            quantity = 420,
            location = "B0201",
            zone = "B",
            aisle = "02",
            shelf = "01",
            bin = "01",
            block = "Block 2B",
            line = "Scania Truck",
            productionLines = listOf("Scania Truck", "Volvo LFT"),
            lineRequirements = mapOf("Scania Truck" to 300, "Volvo LFT" to 120),
            perHarness = 2,
            dailyUsage = 90,
            planTarget = 1500,
            planCurrent = 1100,
            plantId = "P7",
            minStockThreshold = 600,
            maxStockCapacity = 2500,
            customer = "Scania Group",
            priority = "HIGH",
            minLot = 50,
            lastDispenseTime = "2026-11-08 01:05",
            lastDispenseNote = "سحب طارئ لمصنع تجميع الشاحنات"
        ),
        WarehouseItem(
            sku = "13006789",
            name = "Door Switch Micro-Module",
            quantity = 4200,
            location = "C0302",
            zone = "C",
            aisle = "03",
            shelf = "02",
            bin = "02",
            block = "Block 1A",
            line = "Mercedes Sprinter",
            productionLines = listOf("Mercedes Sprinter", "Line 2"),
            lineRequirements = mapOf("Mercedes Sprinter" to 2500, "Line 2" to 1700),
            perHarness = 4,
            dailyUsage = 200,
            planTarget = 4000,
            planCurrent = 3800,
            plantId = "P7",
            minStockThreshold = 1000,
            maxStockCapacity = 4500,
            customer = "Daimler Truck",
            priority = "NORMAL",
            minLot = 250,
            lastDispenseTime = "2026-11-07 20:30",
            lastDispenseNote = "صرف عادي"
        )
    )

    fun initialTransactions(): List<InventoryTransaction> = listOf(
        InventoryTransaction(
            id = "TRX-1001",
            timestamp = 1762594200000L,
            formattedTime = "2026-11-08 02:24",
            sku = "13008512",
            plantId = "P7",
            type = TransactionType.PLAN_UPDATE,
            quantityDelta = 0,
            previousQty = 2500,
            newQty = 2500,
            fromLocation = "A0101",
            toLocation = "A0101",
            operator = "Ahmed Atem",
            notes = "تحديث بلان التشغيل (2500/0)",
            canUndo = true
        ),
        InventoryTransaction(
            id = "TRX-1002",
            timestamp = 1762593600000L,
            formattedTime = "2026-11-08 02:14",
            sku = "13009444",
            plantId = "P7",
            type = TransactionType.ISSUE_OUTBOUND,
            quantityDelta = -500,
            previousQty = 3000,
            newQty = 2500,
            fromLocation = "A0102",
            toLocation = "Line: volvo LFT",
            operator = "NADER ELANSARY",
            notes = "صرف خط إنتاج تجميع فوري",
            canUndo = true
        ),
        InventoryTransaction(
            id = "TRX-1003",
            timestamp = 1762590000000L,
            formattedTime = "2026-11-08 01:14",
            sku = "13008509",
            plantId = "P7",
            type = TransactionType.RECEIVE_INBOUND,
            quantityDelta = 200,
            previousQty = 300,
            newQty = 500,
            fromLocation = "Dock 1 Inbound",
            toLocation = "A0101",
            operator = "محمد شردي",
            notes = "استلام توريد من المورد الخارجي",
            canUndo = true
        ),
        InventoryTransaction(
            id = "TRX-1004",
            timestamp = 1762586400000L,
            formattedTime = "2026-11-08 00:14",
            sku = "13007859",
            plantId = "P7",
            type = TransactionType.RELOCATE_BIN,
            quantityDelta = 0,
            previousQty = 3000,
            newQty = 3000,
            fromLocation = "A0102",
            toLocation = "A0101",
            operator = "Ahmed Atem",
            notes = "نقل داخلي لتحسين مسار الالتقاط الذكي",
            canUndo = true
        )
    )

    fun initialUsers(): List<UserAccount> = listOf(
        UserAccount(
            id = "USR-01",
            name = "محمد شردي",
            role = UserRole.ADMIN,
            device = "Handheld Scanner Terminal #01",
            isLocked = false
        ),
        UserAccount(
            id = "USR-02",
            name = "Ahmed Atem",
            role = UserRole.ADMIN,
            device = "Primary Admin Handheld #01",
            isLocked = false
        ),
        UserAccount(
            id = "USR-03",
            name = "NADER ELANSARY Elharony",
            role = UserRole.SUPERVISOR,
            device = "Handheld Scanner Terminal #03",
            isLocked = false
        ),
        UserAccount(
            id = "USR-04",
            name = "Mahmoud Hassan",
            role = UserRole.SCANNER_TECH,
            device = "Zebra TC57 Forklift Terminal",
            isLocked = false
        )
    )
}

