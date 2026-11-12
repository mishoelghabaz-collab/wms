package com.example.data

object WarehouseStrings {
    fun t(isArabic: Boolean, ar: String, en: String): String = if (isArabic) ar else en

    fun appTitle(isAr: Boolean) = t(isAr, "نظام إدارة المستودعات الذكي (Amazon WMS)", "Amazon WMS Pro")
    fun plantSwitchBanner(isAr: Boolean, plantId: String) =
        t(isAr, "تم تحويل المخزن النشط إلى مصنع $plantId ✓ (/plants/$plantId/inventory)", "Switched active store to Plant $plantId ✓ (/plants/$plantId/inventory)")
    fun close(isAr: Boolean) = t(isAr, "إغلاق", "Dismiss")

    // Nav (5 Primary Enterprise Tabs)
    fun navDashboard(isAr: Boolean) = t(isAr, "لوحة القيادة", "Dashboard")
    fun navInventory(isAr: Boolean) = t(isAr, "المخزون", "Inventory")
    fun navProductionLines(isAr: Boolean) = t(isAr, "خطوط الإنتاج", "Production Lines")
    fun navLocations(isAr: Boolean) = t(isAr, "خريطة المواقع", "Location Map")
    fun navHistory(isAr: Boolean) = t(isAr, "السجل", "History")
    fun navManagement(isAr: Boolean) = t(isAr, "الإدارة", "Management")

    // Dashboard
    fun dashboardTitle(isAr: Boolean, plantId: String) = t(isAr, "لوحة التحكم والعمليات المباشرة — مصنع $plantId", "Enterprise WMS Dashboard — Plant $plantId")
    fun dashboardSubtitle(isAr: Boolean) = t(isAr, "مؤشرات الأداء اللوجستي والربط مع خطوط الإنتاج والمصانع", "Real-time logistics KPIs & production line synchronizer")
    fun kpiTotalUnique(isAr: Boolean) = t(isAr, "الأصناف الفريدة", "Unique Materials")
    fun kpiTotalStockPcs(isAr: Boolean) = t(isAr, "إجمالي الرصيد (PCS)", "Total Inventory (PCS)")
    fun kpiTotalLines(isAr: Boolean) = t(isAr, "خطوط الإنتاج", "Production Lines")
    fun kpiTotalPlants(isAr: Boolean) = t(isAr, "المصانع المتصلة", "Connected Plants")
    fun kpiLowStockWarning(isAr: Boolean) = t(isAr, "نواقص المخزون", "Low Stock Items")
    fun kpiCriticalOut(isAr: Boolean) = t(isAr, "أصناف منتهية الرصيد", "Out of Stock")

    // Production Lines Screen
    fun productionLinesTitle(isAr: Boolean) = t(isAr, "لوحة إدارة خطوط الإنتاج والربط المتعدد", "Multi-Line Production Control Center")
    fun productionLinesSubtitle(isAr: Boolean) = t(isAr, "ربط الصنف الواحد بأكثر من خط إنتاج بدون مضاعفة الرصيد الفعلي للمخزون", "Link single material to multiple lines without physical stock duplication")
    fun searchLinesPlaceholder(isAr: Boolean) = t(isAr, "بحث في خطوط الإنتاج أو أكواد الأصناف...", "Search production lines or material SKU...")
    fun addNewLinePrompt(isAr: Boolean) = t(isAr, "+ إضافة خط إنتاج جديد", "+ Add New Production Line")
    fun lineAssignedMaterials(isAr: Boolean, count: Int) = t(isAr, "$count صنف مرتبط", "$count assigned materials")
    fun allLinesOption(isAr: Boolean) = t(isAr, "كافة الخطوط", "All Lines")

    // Views & Filters
    fun viewCards(isAr: Boolean) = t(isAr, "بطاقات", "Cards")
    fun viewTable(isAr: Boolean) = t(isAr, "جدول مضغوط", "Compact Table")
    fun savedViews(isAr: Boolean) = t(isAr, "طرق العرض المحفوظة:", "Saved Views:")
    fun saveCurrentView(isAr: Boolean) = t(isAr, "حفظ الفلتر الحالي", "Save Current View")
    fun filterByLine(isAr: Boolean) = t(isAr, "تصفية حسب خط الإنتاج (متعدد):", "Filter by Production Line (Multi):")
    fun copySku(isAr: Boolean) = t(isAr, "نسخ الكود", "Copy SKU")
    fun copySkuSuccess(isAr: Boolean, sku: String) = t(isAr, "تم نسخ الكود $sku إلى الحافظة ✓", "SKU $sku copied to clipboard ✓")

    // Material Details
    fun materialDetailsTitle(isAr: Boolean) = t(isAr, "تفاصيل ومواصفات الصنف الشاملة", "Master Material Specification & Line Mapping")
    fun sectionIdentification(isAr: Boolean) = t(isAr, "1. بيانات وهوية الصنف (Identification)", "1. Material Identification")
    fun sectionProduction(isAr: Boolean) = t(isAr, "2. خطوط الإنتاج والتشغيل (Production Lines)", "2. Production Lines & Requirement")
    fun sectionInventory(isAr: Boolean) = t(isAr, "3. بيانات وحركة المخزون (Inventory Balance)", "3. Inventory & Physical Location")
    fun sectionAdditional(isAr: Boolean) = t(isAr, "4. بيانات إضافية والمورد (Supply & Priority)", "4. Additional Info & Supplier")
    fun assignToLine(isAr: Boolean) = t(isAr, "+ ربط بخط إنتاج إضافي", "+ Assign to Production Line")
    fun removeLine(isAr: Boolean) = t(isAr, "إلغاء الربط", "Remove")

    // Bulk Operations
    fun bulkAssignLines(isAr: Boolean) = t(isAr, "ربط بخط إنتاج", "Assign Line")
    fun bulkStatus(isAr: Boolean) = t(isAr, "تعديل الحالة", "Toggle Active")
    fun bulkPriority(isAr: Boolean) = t(isAr, "تعيين الأولوية", "Set Priority")

    // Operations
    fun receive(isAr: Boolean) = t(isAr, "توريد +", "+ Inbound")
    fun issue(isAr: Boolean) = t(isAr, "صرف -", "- Issue")
    fun transfer(isAr: Boolean) = t(isAr, "نقل ⇄", "Transfer ⇄")
    fun interPlant(isAr: Boolean) = t(isAr, "🏭 نقل بين المصانع", "🏭 Plant Transfer")
    fun operationData(isAr: Boolean) = t(isAr, "⚙ بيانات التشغيل والبلان", "⚙ Operation & Plan")

    // Selection
    fun selectedCount(isAr: Boolean, count: Int) = t(isAr, "تم تحديد $count عنصر", "$count items selected")
    fun selectAll(isAr: Boolean, count: Int) = t(isAr, "تحديد الكل ($count)", "Select All ($count)")
    fun clearSelection(isAr: Boolean) = t(isAr, "إلغاء", "Clear")
    fun exportExcel(isAr: Boolean, count: Int) = t(isAr, "نتائج الفلتر لـ Excel ($count عنصر) • XLSX", "Export Filter to Excel ($count items) • XLSX")
    fun exportCsv(isAr: Boolean) = t(isAr, "تصدير CSV", "Export CSV")

    // Chips
    fun block(isAr: Boolean, value: String) = t(isAr, "Block: $value", "Block: $value")
    fun line(isAr: Boolean, value: String) = t(isAr, "Line: $value", "Line: $value")
    fun perHarness(isAr: Boolean, count: Int) = t(isAr, "Per Harness: $count PCS", "Per Harness: $count PCS")
    fun dailyUsage(isAr: Boolean, count: Int) = t(isAr, "Daily Usage: $count", "Daily Usage: $count")
    fun coverage(isAr: Boolean, value: String) = t(isAr, "التغطية: $value", "Coverage: $value")
    fun plan(isAr: Boolean, target: Int, current: Int) = t(isAr, "Plan (البلان): $target/$current", "Plan: $target/$current")

    // Scanner
    fun scannerTitle(isAr: Boolean) = t(isAr, "ماسح اللقطة الواحدة ومركز العمليات الفوري", "Single-Shot Scanner & Operations Center")
    fun scannerSubtitle(isAr: Boolean) = t(isAr, "يتوقف تلقائياً فور التقاط كود 8 أرقام أو لوكيشن (Single-Shot Mode)", "Auto-stops upon capturing 8-digit code or bin location")
    fun scannerMode(isAr: Boolean) = t(isAr, "وضع اللقطة الواحدة: وجّه نحو الكود", "Single-Shot Mode: Aim at code")
    fun scannerPrompt(isAr: Boolean) = t(isAr, "امسح بالكاميرا أو أدخل كود 8 أرقام / موقع", "Scan with camera or enter 8-digit code / location")
    fun clickToLock(isAr: Boolean) = t(isAr, "اضغط للقفل والإجراء ←", "Tap to lock & execute →")

    // History
    fun historyTitle(isAr: Boolean, plantId: String) = t(isAr, "سجل حركات الوردية المباشر — مصنع $plantId", "Live Shift Transactions Log — Plant $plantId")
    fun historySubtitle(isAr: Boolean, plantId: String) = t(isAr, "plants/$plantId/transactions • Permanent Atomic Undo & Excel (.XLSX) Export", "plants/$plantId/transactions • Permanent Atomic Undo & Excel (.XLSX) Export")
    fun searchHistoryPlaceholder(isAr: Boolean) = t(isAr, "فلترة السجل بكود 8 أرقام (13007655) أو الموقع (A0101)", "Filter log by 8-digit SKU (13007655) or Bin (A0101)")
    fun allFilter(isAr: Boolean) = t(isAr, "الكل", "All")
    fun receiveFilter(isAr: Boolean) = t(isAr, "توريد (+)", "Receive (+)")
    fun returnFilter(isAr: Boolean) = t(isAr, "مرتجع (+)", "Return (+)")
    fun issueFilter(isAr: Boolean) = t(isAr, "صرف (-)", "Issue (-)")
    fun transferFilter(isAr: Boolean) = t(isAr, "نقل موقع", "Relocate")
    fun undo(isAr: Boolean) = t(isAr, "تراجع ↺", "Undo ↺")

    // Locations
    fun locationsTitle(isAr: Boolean, plantId: String) = t(isAr, "🗺 خريطة المواقع والرفوف — مصنع $plantId", "🗺 Warehouse Locations & Bins — Plant $plantId")
    fun locationsSubtitle(isAr: Boolean) = t(isAr, "مجمعة حسب الزون (A, B, C, G, H, K, L) وكود الرف ([A-Z]\\d{4})", "Grouped by Zone (A, B, C, G, H, K, L) & Bin Code ([A-Z]\\d{4})")
    fun newLocation(isAr: Boolean) = t(isAr, "+ موقع جديد", "+ New Location")
    fun allZones(isAr: Boolean, count: Int) = t(isAr, "كل الزونات ($count)", "All Zones ($count)")
    fun zoneLabel(isAr: Boolean, zone: String) = t(isAr, "زون $zone", "Zone $zone")
    fun storedCodesCount(isAr: Boolean, plantId: String, count: Int) = t(isAr, "مصنع $plantId • $count أكواد مخزنة", "Plant $plantId • $count stored SKUs")
    fun totalShelfPcs(isAr: Boolean, count: Int) = t(isAr, "PCS $count إجمالي الرصيد بالرف", "Total shelf balance: $count PCS")

    // Management
    fun adminRootActive(isAr: Boolean) = t(isAr, "المدير الجذري مفعل ومحمي (Root Admin Active)", "Root Admin Active & Protected")
    fun plantSectionTitle(isAr: Boolean) = t(isAr, "القسم (ب): إدارة المصانع الديناميكية (إضافة / تعديل / حذف)", "Section (B): Dynamic Plant Management (Add / Edit / Delete)")
    fun plantSectionSubtitle(isAr: Boolean) = t(isAr, "plants/{plantId} • Multi-Tenant Isolated Stores", "plants/{plantId} • Multi-Tenant Isolated Stores")
    fun activate(isAr: Boolean) = t(isAr, "تفعيل", "Activate")
    fun active(isAr: Boolean) = t(isAr, "نشط", "Active")
    fun addNewPlant(isAr: Boolean) = t(isAr, "+ إضافة مصنع جديد:", "+ Add New Plant:")
    fun plantCodePlaceholder(isAr: Boolean) = t(isAr, "الكود (P1)", "Plant Code (P1)")
    fun plantEnPlaceholder(isAr: Boolean) = t(isAr, "Name (EN)", "Name (EN)")
    fun plantArPlaceholder(isAr: Boolean) = t(isAr, "اسم المصنع (عربي)", "Plant Name (AR)")

    // Users
    fun usersSectionTitle(isAr: Boolean) = t(isAr, "القسم (أ): حسابات المشغلين والماسحات", "Section (A): Operator Accounts & Scanners")
    fun roleAdmin(isAr: Boolean) = t(isAr, "مدير النظام", "System Admin")
    fun roleSupervisor(isAr: Boolean) = t(isAr, "مشرف تشغيل", "Supervisor")
    fun roleViewer(isAr: Boolean) = t(isAr, "مشاهد فقط", "Viewer")
    fun roleScanner(isAr: Boolean) = t(isAr, "فني ماسح", "Scanner Tech")

    // Fulfillment & Amazon-Style
    fun fulfillmentTitle(isAr: Boolean) = t(isAr, "مركز عمليات الشحن واستلام التوريدات", "Fulfillment & Receiving Operations")
    fun inboundTab(isAr: Boolean) = t(isAr, "استلام التوريد (Inbound)", "Inbound Receiving")
    fun outboundTab(isAr: Boolean) = t(isAr, "تجهيز الشحنات (Picking/Packing)", "Outbound Picking")
    fun stockAlertsTab(isAr: Boolean) = t(isAr, "تنبيهات المخزون الذكية", "Smart Stock Alerts")
    fun pickItem(isAr: Boolean) = t(isAr, "التقاط من الرف (Pick)", "Pick from Bin")
    fun packItem(isAr: Boolean) = t(isAr, "تغليف وفحص (Pack)", "Pack & Verify")
    fun shipItem(isAr: Boolean) = t(isAr, "إرسال للشحن (Ship)", "Dispatch to Ship")
    fun receivePo(isAr: Boolean) = t(isAr, "استلام وإيداع ذكي (Putaway)", "Receive & Smart Putaway")

    // Metrics
    fun totalInventoryPcs(isAr: Boolean) = t(isAr, "إجمالي رصيد المخزون", "Total Inventory Pieces")
    fun pendingShipments(isAr: Boolean) = t(isAr, "شحنات بانتظار الصرف", "Pending Pick Orders")
    fun dailyThroughput(isAr: Boolean) = t(isAr, "حركة الوردية اليوم", "Today's Throughput")
    fun capacityUtilization(isAr: Boolean) = t(isAr, "نسبة إشغال المستودع", "Warehouse Capacity")
    fun lowStockCount(isAr: Boolean) = t(isAr, "أكواد تحت حد الأمان", "Low Stock SKUs")

    // Voice
    fun voiceAssistantTitle(isAr: Boolean) = t(isAr, "المساعد الصوتي لأمين المخزن", "Warehouse Voice Assistant")
    fun voiceListening(isAr: Boolean) = t(isAr, "جارٍ الاستماع... تحدث بالأمر", "Listening... Speak command")
    fun voiceHelp(isAr: Boolean) = t(isAr, "أوامر سريعة: 'صرف 500 من 13008512' أو 'موقع 13009444' أو 'توريد 1000'", "Commands: 'Pick 500 from 13008512', 'Where is 13009444', 'Receive 1000'")
}
