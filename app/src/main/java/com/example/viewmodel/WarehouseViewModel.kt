package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WarehouseFirestoreRepository
import com.example.data.WarehouseMockData
import com.example.model.*
import com.example.data.AuditItemRecord
import com.example.data.InventoryAuditLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ScannerControlState {
    IDLE,
    SCANNING,
    PAUSED,
    LOCKED,
    COMPLETED
}

data class BarcodeNotFoundEvent(
    val barcode: String,
    val factoryId: String,
    val factoryName: String
)

enum class AuditMode {
    RACK_LEVEL,
    FULL_FACTORY
}

class WarehouseViewModel(
    private val firestoreRepository: WarehouseFirestoreRepository? = null
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val _isArabic = MutableStateFlow(true)
    val isArabic: StateFlow<Boolean> = _isArabic.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private val _currentPlantId = MutableStateFlow("P7")
    val currentPlantId: StateFlow<String> = _currentPlantId.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Dashboard, 1: Inventory, 2: Production Lines, 3: History, 4: Management
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _viewMode = MutableStateFlow(InventoryViewMode.CARD)
    val viewMode: StateFlow<InventoryViewMode> = _viewMode.asStateFlow()

    private val _selectedLinesFilter = MutableStateFlow<Set<String>>(emptySet())
    val selectedLinesFilter: StateFlow<Set<String>> = _selectedLinesFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<StockStatus?>(null)
    val statusFilter: StateFlow<StockStatus?> = _statusFilter.asStateFlow()

    private val _savedFilters = MutableStateFlow(WarehouseMockData.initialSavedFilters())
    val savedFilters: StateFlow<List<SavedFilter>> = _savedFilters.asStateFlow()

    private val _activeSavedFilter = MutableStateFlow<SavedFilter?>(WarehouseMockData.initialSavedFilters().firstOrNull())
    val activeSavedFilter: StateFlow<SavedFilter?> = _activeSavedFilter.asStateFlow()

    private val _selectedMaterialForDetails = MutableStateFlow<WarehouseItem?>(null)
    val selectedMaterialForDetails: StateFlow<WarehouseItem?> = _selectedMaterialForDetails.asStateFlow()

    // Strict Production Line Isolation per Factory (Requirement 2)
    private val _factoryLinesMap = MutableStateFlow<Map<String, List<String>>>(
        mapOf(
            "P7" to listOf("volvo LFT", "volvo RHT", "Renault Master"),
            "P2" to listOf("Toyota Chassis Line", "Engine Wiring Loom", "Corolla Cross Harness"),
            "P1" to listOf("Stellantis Main Line", "EV Battery Pack Feeder")
        )
    )
    val customLines: StateFlow<List<String>> = combine(_currentPlantId, _factoryLinesMap) { plantId, map ->
        map[plantId] ?: listOf("General Production Line")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf("volvo LFT", "volvo RHT", "Renault Master"))

    // Barcode Not Found in Active Factory Modal Event (Requirement 3)
    private val _barcodeNotFoundEvent = MutableStateFlow<BarcodeNotFoundEvent?>(null)
    val barcodeNotFoundEvent: StateFlow<BarcodeNotFoundEvent?> = _barcodeNotFoundEvent.asStateFlow()

    private val _prefilledSku = MutableStateFlow("")
    val prefilledSku: StateFlow<String> = _prefilledSku.asStateFlow()

    // Inventory Audit & Stocktaking State (Requirement 4)
    private val _isAuditDialogOpen = MutableStateFlow(false)
    val isAuditDialogOpen: StateFlow<Boolean> = _isAuditDialogOpen.asStateFlow()

    private val _auditMode = MutableStateFlow(AuditMode.RACK_LEVEL)
    val auditMode: StateFlow<AuditMode> = _auditMode.asStateFlow()

    private val _auditSelectedRackId = MutableStateFlow("A0101")
    val auditSelectedRackId: StateFlow<String> = _auditSelectedRackId.asStateFlow()

    private val _auditPhysicalCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val auditPhysicalCounts: StateFlow<Map<String, Int>> = _auditPhysicalCounts.asStateFlow()

    private val _isReconciling = MutableStateFlow(false)
    val isReconciling: StateFlow<Boolean> = _isReconciling.asStateFlow()

    private val _items = MutableStateFlow(WarehouseMockData.initialItems())
    val items: StateFlow<List<WarehouseItem>> = _items.asStateFlow()

    private val _selectedSkus = MutableStateFlow<Set<String>>(emptySet())
    val selectedSkus: StateFlow<Set<String>> = _selectedSkus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _transactions = MutableStateFlow(WarehouseMockData.initialTransactions())
    val transactions: StateFlow<List<InventoryTransaction>> = _transactions.asStateFlow()

    private val _historyFilter = MutableStateFlow<TransactionType?>(null)
    val historyFilter: StateFlow<TransactionType?> = _historyFilter.asStateFlow()

    private val _historySearch = MutableStateFlow("")
    val historySearch: StateFlow<String> = _historySearch.asStateFlow()

    private val _plants = MutableStateFlow(WarehouseMockData.initialPlants())
    val plants: StateFlow<List<Plant>> = _plants.asStateFlow()

    private val _bins = MutableStateFlow(WarehouseMockData.initialBins())
    val bins: StateFlow<List<BinLocation>> = _bins.asStateFlow()

    private val _selectedZone = MutableStateFlow<String?>(null)
    val selectedZone: StateFlow<String?> = _selectedZone.asStateFlow()

    private val _users = MutableStateFlow(WarehouseMockData.initialUsers())
    val users: StateFlow<List<UserAccount>> = _users.asStateFlow()

    private val _currentUserRole = MutableStateFlow(UserRole.ADMIN)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _activeAlertBanner = MutableStateFlow<String?>("Enterprise WMS System Online ✓ Active Plant: P7")
    val activeAlertBanner: StateFlow<String?> = _activeAlertBanner.asStateFlow()

    // Smart Barcode Scanner Control States
    private val _scannerState = MutableStateFlow(ScannerControlState.SCANNING)
    val scannerState: StateFlow<ScannerControlState> = _scannerState.asStateFlow()

    private val _scannedItem = MutableStateFlow<WarehouseItem?>(null)
    val scannedItem: StateFlow<WarehouseItem?> = _scannedItem.asStateFlow()

    private val _scannedBarcode = MutableStateFlow("")
    val scannedBarcode: StateFlow<String> = _scannedBarcode.asStateFlow()

    private val _isTransferInFlight = MutableStateFlow(false)
    val isTransferInFlight: StateFlow<Boolean> = _isTransferInFlight.asStateFlow()

    private val _lastTransferSuccess = MutableStateFlow<String?>(null)
    val lastTransferSuccess: StateFlow<String?> = _lastTransferSuccess.asStateFlow()

    // Modals
    private val _isSingleShotScannerOpen = MutableStateFlow(false)
    val isSingleShotScannerOpen: StateFlow<Boolean> = _isSingleShotScannerOpen.asStateFlow()

    private val _isVoiceStudioOpen = MutableStateFlow(false)
    val isVoiceStudioOpen: StateFlow<Boolean> = _isVoiceStudioOpen.asStateFlow()

    private val _isOcrScannerOpen = MutableStateFlow(false)
    val isOcrScannerOpen: StateFlow<Boolean> = _isOcrScannerOpen.asStateFlow()

    private val _isAuthDialogOpen = MutableStateFlow(false)
    val isAuthDialogOpen: StateFlow<Boolean> = _isAuthDialogOpen.asStateFlow()

    private val _isGmailDispatcherOpen = MutableStateFlow(false)
    val isGmailDispatcherOpen: StateFlow<Boolean> = _isGmailDispatcherOpen.asStateFlow()

    private val _isQuickAddOpen = MutableStateFlow(false)
    val isQuickAddOpen: StateFlow<Boolean> = _isQuickAddOpen.asStateFlow()

    private val _isAddLocationOpen = MutableStateFlow(false)
    val isAddLocationOpen: StateFlow<Boolean> = _isAddLocationOpen.asStateFlow()

    private val _operationItem = MutableStateFlow<WarehouseItem?>(null)
    val operationItem: StateFlow<WarehouseItem?> = _operationItem.asStateFlow()

    private val _operationType = MutableStateFlow<String?>(null) // "RECEIVE", "ISSUE", "TRANSFER", "PLAN", "INTER_PLANT", "EDIT_MATERIAL", "CYCLE_COUNT"
    val operationType: StateFlow<String?> = _operationType.asStateFlow()

    private val _itemToDelete = MutableStateFlow<WarehouseItem?>(null)
    val itemToDelete: StateFlow<WarehouseItem?> = _itemToDelete.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Connect Firestore Repository if provided
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.seedInitialItemsIfEmpty(WarehouseMockData.initialItems())
            }
            viewModelScope.launch {
                repo.observeItems().collect { cloudItems ->
                    if (cloudItems.isNotEmpty()) {
                        val merged = cloudItems + _items.value.filter { local ->
                            cloudItems.none { it.sku == local.sku && it.plantId == local.plantId }
                        }
                        _items.value = merged
                    }
                }
            }
            viewModelScope.launch {
                repo.observeTransactions().collect { cloudTrx ->
                    if (cloudTrx.isNotEmpty()) {
                        val merged = cloudTrx + _transactions.value.filter { local ->
                            cloudTrx.none { it.id == local.id }
                        }
                        _transactions.value = merged
                    }
                }
            }
        }
    }

    // Role-based permissions
    fun canPerformOperations(): Boolean = _currentUserRole.value != UserRole.VIEWER
    fun canManageMaterials(): Boolean = _currentUserRole.value == UserRole.ADMIN || _currentUserRole.value == UserRole.SUPERVISOR

    fun toggleLanguage() {
        _isArabic.value = !_isArabic.value
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun dismissAlertBanner() {
        _activeAlertBanner.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistorySearch(query: String) {
        _historySearch.value = query
    }

    fun setHistoryFilter(type: TransactionType?) {
        _historyFilter.value = type
    }

    fun setSelectedZone(zone: String?) {
        _selectedZone.value = zone
    }

    fun toggleSkuSelection(sku: String) {
        val current = _selectedSkus.value.toMutableSet()
        if (current.contains(sku)) {
            current.remove(sku)
        } else {
            current.add(sku)
        }
        _selectedSkus.value = current
    }

    fun selectAllSkus(skus: List<String>) {
        _selectedSkus.value = skus.toSet()
    }

    fun clearSkuSelection() {
        _selectedSkus.value = emptySet()
    }

    fun setViewMode(mode: InventoryViewMode) {
        _viewMode.value = mode
    }

    fun setStatusFilter(status: StockStatus?) {
        _statusFilter.value = status
        _activeSavedFilter.value = null
    }

    fun toggleLineFilter(line: String) {
        val current = _selectedLinesFilter.value.toMutableSet()
        if (current.contains(line)) {
            current.remove(line)
        } else {
            current.add(line)
        }
        _selectedLinesFilter.value = current
        _activeSavedFilter.value = null
    }

    fun setLineFilter(lines: Set<String>) {
        _selectedLinesFilter.value = lines
        _activeSavedFilter.value = null
    }

    fun clearLineFilter() {
        _selectedLinesFilter.value = emptySet()
        _statusFilter.value = null
        _activeSavedFilter.value = _savedFilters.value.firstOrNull { it.id == "FLT-ALL" }
    }

    fun applySavedFilter(filter: SavedFilter) {
        _activeSavedFilter.value = filter
        _selectedLinesFilter.value = filter.selectedLines
        _statusFilter.value = filter.statusFilter
        filter.plantId?.let { switchPlant(it) }
        showToast("تم تطبيق فلتر: ${filter.name}")
    }

    fun saveCurrentFilter(name: String) {
        if (name.isBlank()) return
        val newFilter = SavedFilter(
            id = "FLT-USR-${System.currentTimeMillis() % 10000}",
            name = name.trim(),
            selectedLines = _selectedLinesFilter.value,
            statusFilter = _statusFilter.value,
            plantId = _currentPlantId.value,
            isSystemPreset = false
        )
        _savedFilters.value = _savedFilters.value + newFilter
        _activeSavedFilter.value = newFilter
        showToast("تم حفظ الفلتر: $name بنجاح ✓")
    }

    fun deleteSavedFilter(filterId: String) {
        val filter = _savedFilters.value.find { it.id == filterId }
        if (filter?.isSystemPreset == true) {
            showToast("لا يمكن حذف الفلاتر الافتراضية للنظام")
            return
        }
        _savedFilters.value = _savedFilters.value.filterNot { it.id == filterId }
        if (_activeSavedFilter.value?.id == filterId) {
            _activeSavedFilter.value = null
        }
        showToast("تم حذف الفلتر بنجاح")
    }

    fun openMaterialDetails(item: WarehouseItem) {
        _selectedMaterialForDetails.value = item
    }

    fun closeMaterialDetails() {
        _selectedMaterialForDetails.value = null
    }

    fun createProductionLine(lineName: String) {
        val clean = lineName.trim()
        if (clean.isBlank()) return
        val plantId = _currentPlantId.value
        val currentLines = _factoryLinesMap.value[plantId] ?: emptyList()
        if (!currentLines.contains(clean)) {
            _factoryLinesMap.value = _factoryLinesMap.value + (plantId to (currentLines + clean))
            firestoreRepository?.let { repo ->
                viewModelScope.launch {
                    repo.saveFactoryLine(plantId, clean)
                }
            }
            showToast("تم إنشاء خط الإنتاج: $clean بنجاح في مصنع $plantId")
        }
    }

    fun assignLineToMaterial(sku: String, line: String, requirementQty: Int = 100) {
        val cleanLine = line.trim()
        if (cleanLine.isBlank()) return
        createProductionLine(cleanLine)

        var updatedItem: WarehouseItem? = null
        val plantId = _currentPlantId.value

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                val newLines = if (!item.productionLines.contains(cleanLine)) {
                    item.productionLines + cleanLine
                } else item.productionLines

                val newReqs = item.lineRequirements.toMutableMap()
                newReqs[cleanLine] = requirementQty

                val newItem = item.copy(
                    line = newLines.firstOrNull() ?: item.line,
                    productionLines = newLines,
                    lineRequirements = newReqs
                )
                updatedItem = newItem
                newItem
            } else item
        }

        if (_selectedMaterialForDetails.value?.sku == sku) {
            _selectedMaterialForDetails.value = updatedItem
        }

        recordTransaction(
            sku = sku,
            type = TransactionType.LINE_ASSIGNMENT_UPDATE,
            delta = 0,
            prev = updatedItem?.quantity ?: 0,
            next = updatedItem?.quantity ?: 0,
            from = "Lines",
            to = cleanLine,
            notes = "ربط الصنف بخط الإنتاج $cleanLine (المطلوب: $requirementQty PCS)"
        )

        viewModelScope.launch {
            updatedItem?.let { firestoreRepository?.saveItem(it) }
        }

        showToast("تم ربط الصنف $sku بخط $cleanLine بنجاح (الرصيد الفعلي لم يتغير)")
    }

    fun removeLineFromMaterial(sku: String, line: String) {
        val plantId = _currentPlantId.value
        var updatedItem: WarehouseItem? = null

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                if (item.productionLines.size <= 1) {
                    showToast("يجب أن يرتبط الصنف بخط إنتاج واحد على الأقل")
                    return@map item
                }
                val newLines = item.productionLines.filterNot { it.equals(line, ignoreCase = true) }
                val newReqs = item.lineRequirements.filterKeys { !it.equals(line, ignoreCase = true) }
                val newItem = item.copy(
                    line = newLines.firstOrNull() ?: item.line,
                    productionLines = newLines,
                    lineRequirements = newReqs
                )
                updatedItem = newItem
                newItem
            } else item
        }

        if (_selectedMaterialForDetails.value?.sku == sku) {
            _selectedMaterialForDetails.value = updatedItem
        }

        recordTransaction(
            sku = sku,
            type = TransactionType.LINE_ASSIGNMENT_UPDATE,
            delta = 0,
            prev = updatedItem?.quantity ?: 0,
            next = updatedItem?.quantity ?: 0,
            from = line,
            to = "Unassigned",
            notes = "إلغاء ارتباط الصنف من خط الإنتاج $line"
        )

        viewModelScope.launch {
            updatedItem?.let { firestoreRepository?.saveItem(it) }
        }

        showToast("تم إزالة ارتباط الصنف $sku من خط $line")
    }

    fun updateMaterialLineRequirement(sku: String, line: String, qty: Int) {
        val plantId = _currentPlantId.value
        var updatedItem: WarehouseItem? = null

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                val newReqs = item.lineRequirements.toMutableMap()
                newReqs[line] = qty
                val newItem = item.copy(lineRequirements = newReqs)
                updatedItem = newItem
                newItem
            } else item
        }

        if (_selectedMaterialForDetails.value?.sku == sku) {
            _selectedMaterialForDetails.value = updatedItem
        }

        viewModelScope.launch {
            updatedItem?.let { firestoreRepository?.saveItem(it) }
        }
        showToast("تم تحديث احتياج خط $line إلى $qty PCS")
    }

    fun executeBulkAssignLine(skus: List<String>, line: String) {
        if (skus.isEmpty() || line.isBlank()) return
        createProductionLine(line)
        val plantId = _currentPlantId.value

        _items.value = _items.value.map { item ->
            if (skus.contains(item.sku) && item.plantId == plantId) {
                val newLines = if (!item.productionLines.contains(line)) item.productionLines + line else item.productionLines
                val newReqs = item.lineRequirements.toMutableMap()
                if (!newReqs.containsKey(line)) newReqs[line] = 100
                item.copy(line = newLines.first(), productionLines = newLines, lineRequirements = newReqs)
            } else item
        }

        viewModelScope.launch {
            _items.value.filter { skus.contains(it.sku) && it.plantId == plantId }.forEach {
                firestoreRepository?.saveItem(it)
            }
        }
        clearSkuSelection()
        showToast("تم ربط ${skus.size} صنف بخط الإنتاج $line دفعة واحدة ✓")
    }

    fun executeBulkToggleActive(skus: List<String>, active: Boolean) {
        if (skus.isEmpty()) return
        val plantId = _currentPlantId.value
        _items.value = _items.value.map { item ->
            if (skus.contains(item.sku) && item.plantId == plantId) {
                item.copy(isActive = active)
            } else item
        }
        clearSkuSelection()
        showToast("تم تحديث حالة ${skus.size} صنف إلى ${if (active) "نشط" else "معطل"}")
    }

    fun executeBulkSetPriority(skus: List<String>, priority: String) {
        if (skus.isEmpty()) return
        val plantId = _currentPlantId.value
        _items.value = _items.value.map { item ->
            if (skus.contains(item.sku) && item.plantId == plantId) {
                item.copy(priority = priority)
            } else item
        }
        clearSkuSelection()
        showToast("تم تعيين الأولوية ($priority) لـ ${skus.size} صنف")
    }

    // ==========================================
    // Smart Barcode Scanner Control Methods
    // ==========================================

    fun openScanner() {
        _isSingleShotScannerOpen.value = true
        _scannerState.value = ScannerControlState.SCANNING
        _scannedItem.value = null
        _scannedBarcode.value = ""
        _lastTransferSuccess.value = null
    }

    fun closeScanner() {
        _isSingleShotScannerOpen.value = false
        _scannerState.value = ScannerControlState.IDLE
    }

    fun startScanning() {
        _scannerState.value = ScannerControlState.SCANNING
        _lastTransferSuccess.value = null
    }

    fun pauseScanning() {
        _scannerState.value = ScannerControlState.PAUSED
    }

    fun resumeScanning() {
        _scannerState.value = ScannerControlState.SCANNING
        _lastTransferSuccess.value = null
    }

    fun finishScanningSession() {
        _scannerState.value = ScannerControlState.IDLE
        _scannedItem.value = null
        _scannedBarcode.value = ""
        _lastTransferSuccess.value = null
        closeScanner()
    }

    /**
     * Process incoming barcode with single-scan registration, automatic pausing,
     * and strict factory-scoped evaluation (Requirement 3).
     */
    fun processBarcodeScan(barcode: String): Boolean {
        // Anti-duplicate protection: Ignore if paused or locked or already completed
        if (_scannerState.value == ScannerControlState.PAUSED ||
            _scannerState.value == ScannerControlState.COMPLETED ||
            _scannerState.value == ScannerControlState.LOCKED
        ) {
            return false
        }

        val clean = barcode.trim()
        if (clean.isEmpty()) return false

        val activeFactoryId = _currentPlantId.value
        val activePlant = _plants.value.find { it.id == activeFactoryId }
        val activeFactoryName = if (_isArabic.value) (activePlant?.nameAr ?: activeFactoryId) else (activePlant?.nameEn ?: activeFactoryId)

        // Requirement 3.1: Search exclusively within the currently selected factoryId
        val matched = _items.value.find { it.sku == clean && it.plantId == activeFactoryId }
            ?: _items.value.find { it.location.equals(clean, ignoreCase = true) && it.plantId == activeFactoryId }

        if (matched != null) {
            _scannedItem.value = matched
            _scannedBarcode.value = matched.sku
            // Immediately PAUSE scanning after successful scan
            _scannerState.value = ScannerControlState.PAUSED
            showToast(if (_isArabic.value) "تم مسح الكود ${matched.sku} بنجاح — تم إيقاف الكاميرا تلقائياً للمعاينة" else "Scanned ${matched.sku} in $activeFactoryId")
            return true
        } else {
            // Requirement 3.2: Item is NOT FOUND in the active factory -> Trigger modal dialog
            _scannerState.value = ScannerControlState.PAUSED
            _barcodeNotFoundEvent.value = BarcodeNotFoundEvent(
                barcode = clean,
                factoryId = activeFactoryId,
                factoryName = activeFactoryName
            )
            return false
        }
    }

    fun dismissBarcodeNotFoundDialog() {
        _barcodeNotFoundEvent.value = null
    }

    fun onAddScannedBarcodeToCurrentFactory() {
        val event = _barcodeNotFoundEvent.value ?: return
        val barcodeToAdd = event.barcode
        _barcodeNotFoundEvent.value = null
        _prefilledSku.value = barcodeToAdd
        _isQuickAddOpen.value = true
    }

    fun clearPrefilledSku() {
        _prefilledSku.value = ""
    }

    fun selectScannedItemDirectly(item: WarehouseItem) {
        _scannedItem.value = item
        _scannedBarcode.value = item.sku
        _scannerState.value = ScannerControlState.PAUSED
    }

    // ==========================================
    // Inventory Operations from Scanner
    // ==========================================

    fun executeScannedAddQuantity(qty: Int, destFactory: String, destLocation: String, notes: String) {
        val item = _scannedItem.value ?: return
        if (qty <= 0) {
            showToast("الكمية يجب أن تكون أكبر من صفر")
            return
        }
        val targetPlant = if (destFactory.isNotBlank()) destFactory else item.plantId
        receiveStock(item.sku, qty, notes)
        // Refresh scanned item in place
        _scannedItem.value = _items.value.find { it.sku == item.sku && it.plantId == targetPlant } ?: item.copy(quantity = item.quantity + qty)
        _lastTransferSuccess.value = "تمت إضافة +$qty ${item.unit} بنجاح إلى رصيد ${item.sku}"
        _scannerState.value = ScannerControlState.COMPLETED
    }

    fun executeScannedIssueQuantity(qty: Int, destinationDept: String, notes: String) {
        val item = _scannedItem.value ?: return
        if (qty <= 0) {
            showToast("الكمية يجب أن تكون أكبر من صفر")
            return
        }
        if (qty > item.quantity) {
            showToast("⛔ عجز مخزني: الكمية المطلوبة ($qty) تتجاوز الرصيد المتاح (${item.quantity})")
            return
        }
        issueStock(item.sku, qty, if (destinationDept.isNotBlank()) "صرف إلى: $destinationDept • $notes" else notes)
        _scannedItem.value = _items.value.find { it.sku == item.sku && it.plantId == item.plantId } ?: item.copy(quantity = item.quantity - qty)
        _lastTransferSuccess.value = "تم صرف -$qty ${item.unit} بنجاح من ${item.sku}"
        _scannerState.value = ScannerControlState.COMPLETED
    }

    fun executeScannedLocationChange(newLocation: String, notes: String) {
        val item = _scannedItem.value ?: return
        if (newLocation.isBlank()) {
            showToast("يرجى إدخال كود الرف الجديد")
            return
        }
        val cleanLoc = newLocation.trim().uppercase()
        relocateStock(item.sku, cleanLoc, notes)
        _scannedItem.value = _items.value.find { it.sku == item.sku && it.plantId == item.plantId } ?: item.copy(location = cleanLoc)
        _lastTransferSuccess.value = "تم تغيير موقع الصنف ${item.sku} إلى الرف $cleanLoc بنجاح"
        _scannerState.value = ScannerControlState.COMPLETED
    }

    /**
     * Inter-Factory Transfer: Transfers Full or Partial quantity between factories atomically.
     * Enforces double-submission prevention and consistency on both source and destination records.
     */
    fun executeScannedInterFactoryTransfer(
        sourceFactory: String,
        destFactory: String,
        transferQty: Int,
        isFullTransfer: Boolean,
        destLocation: String = "",
        destWarehouse: String = "WH-01",
        notes: String = ""
    ) {
        if (!canPerformOperations()) {
            showToast("غير مصرح: حساب المشاهدة لا يمكنه تحويل المخزون")
            return
        }
        val sourceItem = _scannedItem.value ?: return
        val actualQty = if (isFullTransfer) sourceItem.quantity else transferQty

        if (actualQty <= 0) {
            showToast("الكمية المراد تحويلها يجب أن تكون أكبر من صفر")
            return
        }
        if (actualQty > sourceItem.quantity) {
            showToast("⛔ عجز مخزني: الكمية المراد تحويلها ($actualQty) تتجاوز الرصيد المتاح (${sourceItem.quantity})")
            return
        }
        if (sourceFactory == destFactory) {
            showToast("المصنع المستهدف يجب أن يكون مختلفاً عن مصنع المصدر")
            return
        }

        // Idempotency check: prevent double clicks
        if (_isTransferInFlight.value) return
        _isTransferInFlight.value = true

        val operatorName = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem"
        val clientTrxId = "TRX-XFER-${System.currentTimeMillis()}-${(1000..9999).random()}"

        // Synchronous in-memory update for zero latency and testing
        val currentList = _items.value.toMutableList()
        val srcIdx = currentList.indexOfFirst { it.sku == sourceItem.sku && it.plantId == sourceFactory }
        val remainingSrcQty = sourceItem.quantity - actualQty
        var updatedSourceItem: WarehouseItem? = null
        if (srcIdx >= 0) {
            val oldSrc = currentList[srcIdx]
            val newSrc = oldSrc.copy(
                quantity = remainingSrcQty,
                lastDispenseTime = dateFormat.format(Date()),
                lastDispenseNote = "تحويل إلى مصنع $destFactory: -$actualQty ${oldSrc.unit}"
            )
            currentList[srcIdx] = newSrc
            updatedSourceItem = newSrc
        }

        val finalDstLoc = if (destLocation.isNotBlank()) destLocation.uppercase() else sourceItem.location
        val dstIdx = currentList.indexOfFirst { it.sku == sourceItem.sku && it.plantId == destFactory }
        var expectedDestQty = actualQty
        if (dstIdx >= 0) {
            val oldDst = currentList[dstIdx]
            expectedDestQty = oldDst.quantity + actualQty
            val newDst = oldDst.copy(
                quantity = expectedDestQty,
                location = finalDstLoc,
                warehouse = destWarehouse,
                lastDispenseTime = dateFormat.format(Date()),
                lastDispenseNote = "استلام محول من مصنع $sourceFactory: +$actualQty ${oldDst.unit}"
            )
            currentList[dstIdx] = newDst
        } else {
            val newDst = sourceItem.copy(
                plantId = destFactory,
                quantity = actualQty,
                location = finalDstLoc,
                warehouse = destWarehouse,
                zone = finalDstLoc.take(1).uppercase().ifEmpty { "A" },
                lastDispenseTime = dateFormat.format(Date()),
                lastDispenseNote = "استلام محول من مصنع $sourceFactory: +$actualQty ${sourceItem.unit}"
            )
            currentList.add(newDst)
        }
        _items.value = currentList

        val trx = InventoryTransaction(
            id = clientTrxId,
            timestamp = System.currentTimeMillis(),
            formattedTime = dateFormat.format(Date()),
            sku = sourceItem.sku,
            plantId = sourceFactory,
            toPlantId = destFactory,
            type = TransactionType.INTER_PLANT_TRANSFER,
            quantityDelta = -actualQty,
            previousQty = sourceItem.quantity,
            newQty = remainingSrcQty,
            fromLocation = "${sourceFactory}:${sourceItem.location}",
            toLocation = "${destFactory}:${finalDstLoc}",
            operator = operatorName,
            notes = notes.ifEmpty { "تحويل مصانع (${if (isFullTransfer) "كامل" else "جزئي"}): $actualQty ${sourceItem.unit}" },
            status = "COMPLETED",
            canUndo = true
        )
        _transactions.value = listOf(trx) + _transactions.value

        // Cloud atomic transaction
        if (firestoreRepository != null) {
            viewModelScope.launch {
                val res = firestoreRepository.executeAtomicInterFactoryTransfer(
                    sourcePlant = sourceFactory,
                    destPlant = destFactory,
                    sku = sourceItem.sku,
                    transferQty = actualQty,
                    operator = operatorName,
                    destLocation = finalDstLoc,
                    destWarehouse = destWarehouse,
                    notes = notes,
                    clientTransactionId = clientTrxId
                )
                if (res.isFailure) {
                    showToast("⛔ فشل المزامنة السحابية للتحويل: ${res.exceptionOrNull()?.message}")
                }
            }
        }

        _scannedItem.value = updatedSourceItem ?: sourceItem.copy(quantity = remainingSrcQty)
        _lastTransferSuccess.value = "تم تنفيذ التحويل بنجاح ✓\nالمحول: $actualQty ${sourceItem.unit} | المتبقي بمصنع $sourceFactory: $remainingSrcQty | الرصيد بمصنع $destFactory: $expectedDestQty"
        _scannerState.value = ScannerControlState.COMPLETED
        _isTransferInFlight.value = false
        showToast("تم تحويل $actualQty ${sourceItem.unit} إلى مصنع $destFactory بنجاح")
    }

    // ==========================================
    // Voice & OCR Dialogs
    // ==========================================

    fun openVoiceStudio() {
        _isVoiceStudioOpen.value = true
    }

    fun closeVoiceStudio() {
        _isVoiceStudioOpen.value = false
    }

    fun openOcrScanner() {
        _isOcrScannerOpen.value = true
    }

    fun closeOcrScanner() {
        _isOcrScannerOpen.value = false
    }

    fun openAuthDialog() {
        _isAuthDialogOpen.value = true
    }

    fun closeAuthDialog() {
        _isAuthDialogOpen.value = false
    }

    fun openGmailDispatcher() {
        _isGmailDispatcherOpen.value = true
    }

    fun closeGmailDispatcher() {
        _isGmailDispatcherOpen.value = false
    }

    fun openQuickAdd() {
        if (!canPerformOperations()) {
            showToast("غير مصرح: حساب المشاهدة لا يمكنه إضافة أصناف جديدة")
            return
        }
        _isQuickAddOpen.value = true
    }

    fun closeQuickAdd() {
        _isQuickAddOpen.value = false
    }

    fun openAddLocation() {
        if (!canManageMaterials()) {
            showToast("غير مصرح: المشرف أو المدير فقط يمكنه إضافة لوكيشن")
            return
        }
        _isAddLocationOpen.value = true
    }

    fun closeAddLocation() {
        _isAddLocationOpen.value = false
    }

    fun openOperation(item: WarehouseItem, type: String) {
        if (!canPerformOperations() && type != "PLAN") {
            showToast("غير مصرح: حساب المشاهدة لا يمكنه تنفيذ حركات مخزنية")
            return
        }
        _operationItem.value = item
        _operationType.value = type
    }

    fun closeOperation() {
        _operationItem.value = null
        _operationType.value = null
    }

    fun confirmDeleteItem(item: WarehouseItem) {
        if (!canManageMaterials()) {
            showToast("غير مصرح: المشرف والمدير فقط يمكنهما حذف الأصناف")
            return
        }
        if (item.quantity > 0) {
            showToast("⛔ حماية المخزون: لا يمكن حذف الصنف لأن رصيده ${item.quantity} ${item.unit}. يجب تصفير الرصيد أولاً!")
            return
        }
        _itemToDelete.value = item
    }

    fun dismissDeleteDialog() {
        _itemToDelete.value = null
    }

    fun executeDeleteItem() {
        val target = _itemToDelete.value ?: return
        if (target.quantity > 0) {
            showToast("⛔ منع الحذف: الرصيد أكبر من صفر")
            _itemToDelete.value = null
            return
        }

        _items.value = _items.value.filterNot { it.sku == target.sku && it.plantId == target.plantId }
        _itemToDelete.value = null

        viewModelScope.launch {
            val result = firestoreRepository?.deleteItemSafe(target.plantId, target.sku)
            if (result != null && result.isFailure) {
                showToast(result.exceptionOrNull()?.message ?: "خطأ في حذف الصنف من السحابة")
            } else {
                showToast("تم حذف الصنف ${target.sku} بنجاح ✓")
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun switchPlant(plantId: String) {
        _currentPlantId.value = plantId
        _plants.value = _plants.value.map { it.copy(isActive = it.id == plantId) }
        _activeAlertBanner.value = "Switched active store to Plant $plantId ✓ (/plants/$plantId/inventory)"
        clearSkuSelection()
    }

    fun addNewPlant(id: String, nameEn: String, nameAr: String) {
        if (_currentUserRole.value != UserRole.ADMIN) {
            showToast("⛔ غير مصرح: إضافة مصانع جديدة يتطلب صلاحية مدير النظام")
            return
        }
        if (id.isBlank()) return
        val newPlant = Plant(id = id.uppercase(), nameEn = nameEn, nameAr = nameAr, isActive = false)
        _plants.value = _plants.value + newPlant
        showToast("تمت إضافة مصنع $id بنجاح")
    }

    fun deletePlant(id: String) {
        if (_currentUserRole.value != UserRole.ADMIN) {
            showToast("⛔ غير مصرح: حذف المصانع يتطلب صلاحية مدير النظام")
            return
        }
        if (_currentPlantId.value == id) {
            showToast("لا يمكن حذف المصنع النشط حالياً")
            return
        }
        _plants.value = _plants.value.filter { it.id != id }
        showToast("تم حذف المصنع $id")
    }

    fun addNewLocation(zone: String, aisle: String, shelf: String, bin: String, capacity: Int) {
        val code = "${zone.uppercase()}$aisle$shelf"
        val newBin = BinLocation(
            code = code,
            zone = zone.uppercase(),
            aisle = aisle,
            shelf = shelf,
            binNumber = bin,
            plantId = _currentPlantId.value,
            maxCapacityPcs = capacity
        )
        _bins.value = _bins.value + newBin
        showToast("تمت إضافة موقع الرف $code بنجاح")
        closeAddLocation()
    }

    fun commitExtractedOcrItems(itemsToAdd: List<WarehouseItem>) {
        if (itemsToAdd.isEmpty()) return
        var count = 0
        val current = _items.value.toMutableList()
        itemsToAdd.forEach { newItem ->
            val existingIndex = current.indexOfFirst { it.sku == newItem.sku && it.plantId == _currentPlantId.value }
            if (existingIndex >= 0) {
                val old = current[existingIndex]
                current[existingIndex] = old.copy(quantity = old.quantity + newItem.quantity)
            } else {
                current.add(0, newItem.copy(plantId = _currentPlantId.value))
                count++
            }
            viewModelScope.launch {
                firestoreRepository?.saveItem(newItem)
            }
        }
        _items.value = current
        recordTransaction(
            sku = itemsToAdd.first().sku,
            type = TransactionType.RECEIVE_INBOUND,
            delta = itemsToAdd.sumOf { it.quantity },
            prev = 0,
            next = itemsToAdd.sumOf { it.quantity },
            from = "Blueprint OCR Schematic 6925",
            to = "Warehouse Bins",
            notes = "اعتماد مخطط تفاعلي عالي الكثافة (${itemsToAdd.size} صنف)"
        )
        showToast("تم اعتماد وحفظ ${itemsToAdd.size} صنف في مصنع ${_currentPlantId.value} بنجاح ✓")
    }

    fun receiveStock(sku: String, delta: Int, notes: String) {
        if (!canPerformOperations()) {
            showToast("غير مصرح: حساب المشاهدة لا يمكنه تنفيذ التوريد")
            return
        }
        if (delta <= 0) {
            showToast("يجب إدخال كمية صحيحة أكبر من الصفر")
            return
        }

        val operatorName = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem"
        val plantId = _currentPlantId.value
        val existingItem = _items.value.find { it.sku == sku && it.plantId == plantId }
        val prev = existingItem?.quantity ?: 0
        val next = prev + delta

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                item.copy(quantity = next)
            } else item
        }

        recordTransaction(
            sku = sku,
            type = TransactionType.RECEIVE_INBOUND,
            delta = delta,
            prev = prev,
            next = next,
            from = "Inbound Receiving Dock",
            to = existingItem?.location ?: "",
            notes = notes.ifEmpty { "توريد بضاعة جديدة" }
        )

        if (firestoreRepository != null) {
            viewModelScope.launch {
                val res = firestoreRepository.executeAtomicStockAdjustment(
                    plantId = plantId,
                    sku = sku,
                    delta = delta,
                    type = TransactionType.RECEIVE_INBOUND,
                    operator = operatorName,
                    notes = notes.ifEmpty { "توريد بضاعة جديدة" }
                )
                if (res.isFailure) {
                    showToast("فشلت مزامنة التوريد السحابية: ${res.exceptionOrNull()?.message}")
                }
            }
        }
        showToast("تم توريد +$delta قطعة إلى $sku بنجاح")
        closeOperation()
    }

    fun issueStock(sku: String, delta: Int, notes: String) {
        if (!canPerformOperations()) {
            showToast("غير مصرح: حساب المشاهدة لا يمكنه صرف المخزون")
            return
        }
        if (delta <= 0) {
            showToast("يجب إدخال كمية صحيحة أكبر من الصفر")
            return
        }

        val operatorName = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem"
        val plantId = _currentPlantId.value
        val existingItem = _items.value.find { it.sku == sku && it.plantId == plantId }

        if (existingItem != null && existingItem.quantity < delta) {
            showToast("⛔ عجز مخزني: الرصيد المتاح ${existingItem.quantity} قطعة، والمطلوب صرفه $delta قطعة!")
            return
        }

        val prev = existingItem?.quantity ?: 0
        val next = prev - delta

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                item.copy(
                    quantity = next,
                    issuedQuantity = item.issuedQuantity + delta,
                    lastDispenseTime = dateFormat.format(Date()),
                    lastDispenseNote = "صرف $delta قطعة لخط ${item.line}"
                )
            } else item
        }

        recordTransaction(
            sku = sku,
            type = TransactionType.ISSUE_OUTBOUND,
            delta = -delta,
            prev = prev,
            next = next,
            from = existingItem?.location ?: "",
            to = "Production Line: ${existingItem?.line ?: "volvo LFT"}",
            notes = notes.ifEmpty { "صرف إنتاج" }
        )

        if (firestoreRepository != null) {
            viewModelScope.launch {
                val res = firestoreRepository.executeAtomicStockAdjustment(
                    plantId = plantId,
                    sku = sku,
                    delta = -delta,
                    type = TransactionType.ISSUE_OUTBOUND,
                    operator = operatorName,
                    notes = notes.ifEmpty { "صرف إنتاج" },
                    toLoc = "Production Line: ${existingItem?.line ?: "volvo LFT"}",
                    allowNegative = false
                )
                if (res.isFailure) {
                    showToast("⛔ فشل الصرف في السحابة: ${res.exceptionOrNull()?.message}")
                }
            }
        }
        showToast("تم صرف -$delta قطعة من $sku بنجاح")
        closeOperation()
    }

    fun reconcileCycleCount(sku: String, physicalCount: Int, reason: String) {
        if (!canManageMaterials()) {
            showToast("غير مصرح: تسوية الجرد تتطلب صلاحية مشرف أو مدير")
            return
        }
        if (physicalCount < 0) {
            showToast("العدد الفعلي لا يمكن أن يكون سالباً")
            return
        }

        val operatorName = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem"
        val plantId = _currentPlantId.value

        viewModelScope.launch {
            if (firestoreRepository != null) {
                val res = firestoreRepository.executeCycleCountReconciliation(
                    plantId = plantId,
                    sku = sku,
                    physicalCount = physicalCount,
                    operator = operatorName,
                    reason = reason.ifEmpty { "مطابقة جرد فعلي دوري" }
                )
                if (res.isSuccess) {
                    val (updatedItem, trx) = res.getOrThrow()
                    _items.value = _items.value.map { if (it.sku == sku && it.plantId == plantId) updatedItem else it }
                    _transactions.value = listOf(trx) + _transactions.value
                    val diff = trx.quantityDelta
                    showToast("تمت تسوية الجرد بنجاح ✓ فرق: ${if (diff >= 0) "+$diff" else "$diff"} PCS")
                } else {
                    showToast("فشلت تسوية الجرد: ${res.exceptionOrNull()?.message}")
                }
            } else {
                _items.value = _items.value.map { item ->
                    if (item.sku == sku && item.plantId == plantId) {
                        val diff = physicalCount - item.quantity
                        recordTransaction(
                            sku = sku,
                            type = TransactionType.CYCLE_COUNT_AUDIT,
                            delta = diff,
                            prev = item.quantity,
                            next = physicalCount,
                            from = item.location,
                            to = item.location,
                            notes = "تسوية جرد دوري: الرصيد الفعلي $physicalCount مقابل النظامي ${item.quantity} ($reason)"
                        )
                        item.copy(quantity = physicalCount)
                    } else item
                }
                showToast("تمت تسوية الجرد الفعلي لكود $sku")
            }
        }
        closeOperation()
    }

    fun editMaterialAttributes(
        sku: String,
        name: String,
        unit: String,
        minStock: Int,
        maxStock: Int,
        supplier: String,
        isActive: Boolean
    ) {
        if (!canManageMaterials()) {
            showToast("غير مصرح: المشرف أو المدير فقط يمكنه تعديل مواصفات الصنف")
            return
        }

        val plantId = _currentPlantId.value
        var updatedItem: WarehouseItem? = null

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                val newItem = item.copy(
                    name = name.ifEmpty { item.name },
                    unit = unit.ifEmpty { item.unit },
                    minStockThreshold = minStock,
                    maxStockCapacity = maxStock,
                    supplier = supplier.ifEmpty { item.supplier },
                    isActive = isActive
                )
                updatedItem = newItem
                newItem
            } else item
        }

        recordTransaction(
            sku = sku,
            type = TransactionType.MATERIAL_EDIT,
            delta = 0,
            prev = updatedItem?.quantity ?: 0,
            next = updatedItem?.quantity ?: 0,
            from = updatedItem?.location ?: "",
            to = updatedItem?.location ?: "",
            notes = "تعديل مواصفات الصنف: حد أدنى $minStock / حد أقصى $maxStock / المورد: $supplier"
        )

        viewModelScope.launch {
            updatedItem?.let { firestoreRepository?.saveItem(it) }
        }

        showToast("تم تعديل مواصفات الصنف $sku بنجاح ✓")
        closeOperation()
    }

    fun relocateStock(sku: String, newBin: String, notes: String) {
        if (newBin.isBlank()) return
        val plantId = _currentPlantId.value
        var updatedItem: WarehouseItem? = null
        val cleanBin = newBin.trim().uppercase()
        val zone = cleanBin.take(1).uppercase().ifEmpty { "A" }

        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == plantId) {
                val prevLoc = item.location
                recordTransaction(
                    sku = sku,
                    type = TransactionType.RELOCATE_BIN,
                    delta = 0,
                    prev = item.quantity,
                    next = item.quantity,
                    from = prevLoc,
                    to = cleanBin,
                    notes = notes.ifEmpty { "نقل داخلي بين الرفوف" }
                )
                val newItem = item.copy(
                    location = cleanBin,
                    zone = zone,
                    shelf = cleanBin.drop(1).take(2).ifEmpty { "01" }
                )
                updatedItem = newItem
                newItem
            } else item
        }

        if (firestoreRepository != null) {
            val operatorName = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem"
            viewModelScope.launch {
                firestoreRepository.executeAtomicLocationChange(plantId, sku, cleanBin, operatorName, notes)
            }
        }
        showToast("تم نقل $sku إلى $cleanBin بنجاح")
        closeOperation()
    }

    fun transferPlant(sku: String, targetPlant: String, qty: Int) {
        executeScannedInterFactoryTransfer(
            sourceFactory = _currentPlantId.value,
            destFactory = targetPlant,
            transferQty = qty,
            isFullTransfer = false,
            destLocation = "",
            notes = "تحويل شحنة بين المصانع"
        )
        closeOperation()
    }

    fun updatePlanData(sku: String, target: Int, current: Int, line: String, block: String, daily: Int) {
        var updatedItem: WarehouseItem? = null
        _items.value = _items.value.map { item ->
            if (item.sku == sku && item.plantId == _currentPlantId.value) {
                recordTransaction(
                    sku = sku,
                    type = TransactionType.PLAN_UPDATE,
                    delta = 0,
                    prev = item.quantity,
                    next = item.quantity,
                    from = item.location,
                    to = item.location,
                    notes = "تحديث خط $line وبلان ($target/$current)"
                )
                val newItem = item.copy(
                    planTarget = target,
                    planCurrent = current,
                    line = line,
                    block = block,
                    dailyUsage = daily
                )
                updatedItem = newItem
                newItem
            } else item
        }
        updatedItem?.let { item ->
            viewModelScope.launch {
                firestoreRepository?.saveItem(item)
            }
        }
        showToast("تم حفظ بيانات التشغيل والبلان لكود $sku")
        closeOperation()
    }

    fun addNewSku(
        sku: String,
        name: String,
        qty: Int,
        location: String,
        line: String,
        dailyUsage: Int,
        planTarget: Int
    ) {
        if (sku.length < 4) return
        val zone = location.take(1).uppercase()
        val newItem = WarehouseItem(
            sku = sku,
            name = name.ifEmpty { "Wire Harness Component" },
            quantity = qty,
            location = location.uppercase(),
            zone = if (zone.isNotEmpty()) zone else "A",
            aisle = "01",
            shelf = "01",
            bin = "01",
            block = "Block 1",
            line = line.ifEmpty { "volvo LFT" },
            dailyUsage = dailyUsage,
            planTarget = planTarget,
            plantId = _currentPlantId.value
        )
        _items.value = listOf(newItem) + _items.value
        recordTransaction(
            sku = sku,
            type = TransactionType.RECEIVE_INBOUND,
            delta = qty,
            prev = 0,
            next = qty,
            from = "Manual Addition",
            to = location,
            notes = "إضافة صنف جديد بالمخزن"
        )
        viewModelScope.launch {
            firestoreRepository?.saveItem(newItem)
        }
        showToast("تمت إضافة الصنف $sku بنجاح")
        closeQuickAdd()
    }

    fun undoTransaction(trxId: String) {
        val trx = _transactions.value.find { it.id == trxId } ?: return
        if (!trx.canUndo) return

        var affectedItem: WarehouseItem? = null
        _items.value = _items.value.map { item ->
            if (item.sku == trx.sku && item.plantId == trx.plantId) {
                val newItem = when (trx.type) {
                    TransactionType.RECEIVE_INBOUND -> item.copy(quantity = item.quantity - trx.quantityDelta)
                    TransactionType.ISSUE_OUTBOUND -> item.copy(quantity = item.quantity - trx.quantityDelta)
                    TransactionType.RELOCATE_BIN -> item.copy(location = trx.fromLocation)
                    TransactionType.INTER_PLANT_TRANSFER -> item.copy(quantity = item.quantity - trx.quantityDelta)
                    TransactionType.CYCLE_COUNT_AUDIT -> item.copy(quantity = item.quantity - trx.quantityDelta)
                    else -> item
                }
                affectedItem = newItem
                newItem
            } else item
        }

        val updatedNotes = "${trx.notes} [تم التراجع]"
        _transactions.value = _transactions.value.map {
            if (it.id == trxId) it.copy(canUndo = false, notes = updatedNotes) else it
        }

        viewModelScope.launch {
            affectedItem?.let { firestoreRepository?.saveItem(it) }
            firestoreRepository?.markTransactionUndone(trxId, updatedNotes)
        }

        showToast("تم التراجع عن الحركة ${trx.id} بنجاح")
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        if (_currentUserRole.value != UserRole.ADMIN) {
            showToast("⛔ غير مصرح: تغيير صلاحيات المستخدمين يتطلب صلاحية مدير النظام (ADMIN)")
            return
        }
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(role = newRole) else it
        }
        showToast("تم تحديث صلاحيات المستخدم إلى $newRole")
    }

    fun toggleUserLock(userId: String) {
        if (_currentUserRole.value != UserRole.ADMIN) {
            showToast("⛔ غير مصرح: قفل أو فتح المحطات يتطلب صلاحية مدير النظام (ADMIN)")
            return
        }
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(isLocked = !it.isLocked) else it
        }
        showToast("تم تحديث حالة القفل للمحطة")
    }

    fun executeVoiceCommand(command: String): String {
        val clean = command.trim()
        val digitsMatch = Regex("\\d{8}").find(clean)?.value
            ?: Regex("\\d{5,8}").find(clean)?.value
        val qtyMatch = Regex("(\\d+)\\s*(قطعة|pcs|piece|بيس)?", RegexOption.IGNORE_CASE).find(clean)?.groupValues?.get(1)?.toIntOrNull()
        val locationMatch = Regex("([A-Za-z]\\d{4})").find(clean)?.value

        return when {
            // Issue
            clean.contains("إيشو") || clean.contains("صرف") || clean.contains("issue", ignoreCase = true) -> {
                val sku = digitsMatch ?: "13008512"
                val qty = qtyMatch ?: 20
                issueStock(sku, qty, "أمر صوتي مباشر")
                "تم تنفيذ الصرف بنجاح: -$qty قطعة من كود $sku"
            }
            // Receive
            clean.contains("ريسيف") || clean.contains("توريد") || clean.contains("receive", ignoreCase = true) -> {
                val sku = digitsMatch ?: "13008509"
                val qty = qtyMatch ?: 50
                receiveStock(sku, qty, "توريد صوتي مباشر")
                "تم استلام التوريد بنجاح: +$qty قطعة إلى كود $sku"
            }
            // Relocate
            clean.contains("انقل") || clean.contains("نقل") || clean.contains("move", ignoreCase = true) -> {
                val sku = digitsMatch ?: "13008512"
                val targetBin = locationMatch ?: "B0501"
                relocateStock(sku, targetBin, "نقل موقع صوتي")
                "تم نقل كود $sku إلى اللوكيشن $targetBin بنجاح"
            }
            // Locate query
            clean.contains("موقع") || clean.contains("مكان") || clean.contains("location", ignoreCase = true) -> {
                val sku = digitsMatch ?: "13008512"
                val item = _items.value.find { it.sku == sku }
                if (item != null) "الكود $sku موجود بالرف ${item.location} برصيد ${item.quantity} قطعة"
                else "لم يتم العثور على الكود $sku"
            }
            clean.contains("بحث") || clean.contains("فلتر") -> {
                if (digitsMatch != null) {
                    setSearchQuery(digitsMatch)
                    "تمت فلترة القائمة بالكود $digitsMatch"
                } else "يرجى ذكر رقم الكود المطلوب للبحث"
            }
            else -> {
                "تم تسجيل الأمر: $command. يمكنك تجربة: 'إيشو 20 بيس من كود 13008512' أو 'ريسيف 50 بيس كود 13008509'"
            }
        }
    }

    fun generateInventoryCsv(): String {
        val currentItems = _items.value.filter { it.plantId == _currentPlantId.value }
        val sb = StringBuilder()
        sb.append("SKU,Description,Quantity,Unit,Location,Zone,Line,Block,DailyUsage,PlanTarget,PlanCurrent,MinThreshold,MaxCapacity,Status\n")
        currentItems.forEach { item ->
            sb.append("\"${item.sku}\",")
            sb.append("\"${item.name}\",")
            sb.append("${item.quantity},")
            sb.append("\"${item.unit}\",")
            sb.append("\"${item.location}\",")
            sb.append("\"${item.zone}\",")
            sb.append("\"${item.line}\",")
            sb.append("\"${item.block}\",")
            sb.append("${item.dailyUsage},")
            sb.append("${item.planTarget},")
            sb.append("${item.planCurrent},")
            sb.append("${item.minStockThreshold},")
            sb.append("${item.maxStockCapacity},")
            sb.append("\"${item.status.name}\"\n")
        }
        return sb.toString()
    }

    fun generateTransactionsCsv(): String {
        val currentTrx = _transactions.value.filter { it.plantId == _currentPlantId.value }
        val sb = StringBuilder()
        sb.append("TransactionID,Timestamp,SKU,SourcePlant,DestPlant,Type,Delta,PreviousQty,NewQty,FromLocation,ToLocation,Operator,Notes,Status\n")
        currentTrx.forEach { trx ->
            sb.append("\"${trx.id}\",")
            sb.append("\"${trx.formattedTime}\",")
            sb.append("\"${trx.sku}\",")
            sb.append("\"${trx.plantId}\",")
            sb.append("\"${trx.toPlantId}\",")
            sb.append("\"${trx.type.name}\",")
            sb.append("${trx.quantityDelta},")
            sb.append("${trx.previousQty},")
            sb.append("${trx.newQty},")
            sb.append("\"${trx.fromLocation}\",")
            sb.append("\"${trx.toLocation}\",")
            sb.append("\"${trx.operator}\",")
            sb.append("\"${trx.notes.replace("\"", "'")}\",")
            sb.append("\"${trx.status}\"\n")
        }
        return sb.toString()
    }

    private fun recordTransaction(
        sku: String,
        type: TransactionType,
        delta: Int,
        prev: Int,
        next: Int,
        from: String,
        to: String,
        notes: String
    ): InventoryTransaction {
        val newTrx = InventoryTransaction(
            id = "TRX-${System.currentTimeMillis() % 100000}",
            timestamp = System.currentTimeMillis(),
            formattedTime = dateFormat.format(Date()),
            sku = sku,
            plantId = _currentPlantId.value,
            type = type,
            quantityDelta = delta,
            previousQty = prev,
            newQty = next,
            fromLocation = from,
            toLocation = to,
            operator = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem",
            notes = notes,
            status = "COMPLETED",
            canUndo = true
        )
        _transactions.value = listOf(newTrx) + _transactions.value
        viewModelScope.launch {
            firestoreRepository?.recordTransaction(newTrx)
        }
        return newTrx
    }

    fun scanAgain() {
        _scannedItem.value = null
        _scannedBarcode.value = ""
        _lastTransferSuccess.value = null
        _scannerState.value = ScannerControlState.SCANNING
    }

    // ==========================================
    // Inventory Audit & Stocktaking System (Requirement 4)
    // ==========================================

    fun openAuditDialog(mode: AuditMode = AuditMode.RACK_LEVEL, rackId: String? = null) {
        _auditMode.value = mode
        val factoryRacks = _items.value.filter { it.plantId == _currentPlantId.value }.map { it.location }.distinct().sorted()
        _auditSelectedRackId.value = rackId ?: factoryRacks.firstOrNull() ?: "A0101"

        val targetItems = if (mode == AuditMode.RACK_LEVEL) {
            _items.value.filter { it.plantId == _currentPlantId.value && it.location == _auditSelectedRackId.value }
        } else {
            _items.value.filter { it.plantId == _currentPlantId.value }
        }
        _auditPhysicalCounts.value = targetItems.associate { it.sku to it.quantity }
        _isAuditDialogOpen.value = true
    }

    fun closeAuditDialog() {
        _isAuditDialogOpen.value = false
    }

    fun setAuditMode(mode: AuditMode) {
        _auditMode.value = mode
        val targetItems = if (mode == AuditMode.RACK_LEVEL) {
            _items.value.filter { it.plantId == _currentPlantId.value && it.location == _auditSelectedRackId.value }
        } else {
            _items.value.filter { it.plantId == _currentPlantId.value }
        }
        val currentCounts = _auditPhysicalCounts.value.toMutableMap()
        targetItems.forEach { item ->
            if (!currentCounts.containsKey(item.sku)) {
                currentCounts[item.sku] = item.quantity
            }
        }
        _auditPhysicalCounts.value = currentCounts
    }

    fun setAuditRackId(rackId: String) {
        _auditSelectedRackId.value = rackId
        val targetItems = _items.value.filter { it.plantId == _currentPlantId.value && it.location == rackId }
        val currentCounts = _auditPhysicalCounts.value.toMutableMap()
        targetItems.forEach { item ->
            if (!currentCounts.containsKey(item.sku)) {
                currentCounts[item.sku] = item.quantity
            }
        }
        _auditPhysicalCounts.value = currentCounts
    }

    fun updateAuditPhysicalCount(sku: String, count: Int) {
        _auditPhysicalCounts.value = _auditPhysicalCounts.value + (sku to count.coerceAtLeast(0))
    }

    fun executeReconcileAudit(notes: String = "") {
        val factoryId = _currentPlantId.value
        val mode = _auditMode.value
        val itemsInScope = if (mode == AuditMode.RACK_LEVEL) {
            _items.value.filter { it.plantId == factoryId && it.location == _auditSelectedRackId.value }
        } else {
            _items.value.filter { it.plantId == factoryId }
        }

        if (itemsInScope.isEmpty()) {
            showToast("لا توجد أصناف في نطاق الجرد المحدد")
            return
        }

        val records = itemsInScope.map { item ->
            val phys = _auditPhysicalCounts.value[item.sku] ?: item.quantity
            AuditItemRecord(
                sku = item.sku,
                name = item.name,
                rackId = item.location,
                systemCount = item.quantity,
                physicalCount = phys,
                variance = phys - item.quantity
            )
        }

        _isReconciling.value = true

        // Instant in-memory optimistic update
        val updatedMap = records.associateBy { it.sku }
        _items.value = _items.value.map { item ->
            if (item.plantId == factoryId && updatedMap.containsKey(item.sku)) {
                item.copy(quantity = updatedMap[item.sku]!!.physicalCount)
            } else item
        }

        val operatorName = _users.value.firstOrNull { it.role == _currentUserRole.value }?.name ?: "Ahmed Atem"
        val operatorId = "OP-${operatorName.hashCode()}"

        // Firestore real-time reconciliation commit
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                val result = repo.reconcileInventoryAudit(
                    factoryId = factoryId,
                    auditMode = mode.name,
                    targetRackId = if (mode == AuditMode.RACK_LEVEL) _auditSelectedRackId.value else null,
                    auditRecords = records,
                    operatorId = operatorId,
                    operatorName = operatorName,
                    notes = notes
                )
                _isReconciling.value = false
                if (result.isSuccess) {
                    val log = result.getOrNull()
                    val varianceTxt = if ((log?.netVariance ?: 0) >= 0) "+${log?.netVariance}" else "${log?.netVariance}"
                    showToast("✓ تم اعتماد تسوية الجرد بنجاح في [$factoryId] • صافي الفارق: $varianceTxt قطعة")
                    _isAuditDialogOpen.value = false
                } else {
                    showToast("فشلت المزامنة السحابية للجرد: ${result.exceptionOrNull()?.message}")
                }
            }
        } ?: run {
            _isReconciling.value = false
            showToast("✓ تم حفظ التسوية محلياً بنجاح")
            _isAuditDialogOpen.value = false
        }
    }
}
