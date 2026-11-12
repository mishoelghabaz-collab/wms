package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.FirebaseAuthService
import com.example.data.WarehouseFirestoreRepository
import com.example.ui.components.WarehouseBottomNav
import com.example.ui.components.WarehouseFloatingActionBar
import com.example.ui.components.WarehouseTopBar
import com.example.ui.dialogs.AddLocationDialog
import com.example.ui.dialogs.AdvancedBarcodeScannerDialog
import com.example.ui.dialogs.AuthDialog
import com.example.ui.dialogs.BarcodeNotFoundDialog
import com.example.ui.dialogs.ConfirmDeleteMaterialDialog
import com.example.ui.dialogs.GmailDispatcherDialog
import com.example.ui.dialogs.InventoryAuditDialog
import com.example.ui.dialogs.MaterialDetailDialog
import com.example.ui.dialogs.QuickAddSkuDialog
import com.example.ui.dialogs.SchematicOcrScannerDialog
import com.example.ui.dialogs.VoiceCommandStudioDialog
import com.example.ui.dialogs.WarehouseOperationDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.ManagementScreen
import com.example.ui.screens.ProductionLinesScreen
import com.example.ui.theme.DarkNavyBase
import com.example.ui.theme.WarehouseTheme
import com.example.viewmodel.WarehouseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val authService = remember(context) { FirebaseAuthService(context) }
            val repository = remember(context) {
                try {
                    WarehouseFirestoreRepository(context)
                } catch (e: Exception) {
                    null
                }
            }
            val viewModel: WarehouseViewModel = viewModel { WarehouseViewModel(repository) }
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            WarehouseTheme(darkTheme = isDarkMode) {
                WarehouseApp(viewModel, authService)
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, isArabic: Boolean) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Material SKU", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            if (isArabic) "تم نسخ الكود $text إلى الحافظة ✓" else "SKU $text copied to clipboard ✓",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}

private fun shareCsvData(context: Context, filename: String, csvContent: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, filename)
            putExtra(Intent.EXTRA_TEXT, csvContent)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "تصدير ومشاركة بيانات المستودع")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "فشل بدء المشاركة: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WarehouseApp(viewModel: WarehouseViewModel, authService: FirebaseAuthService) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isArabic by viewModel.isArabic.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentPlantId by viewModel.currentPlantId.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val selectedSkus by viewModel.selectedSkus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectedLinesFilter by viewModel.selectedLinesFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()
    val activeSavedFilter by viewModel.activeSavedFilter.collectAsStateWithLifecycle()
    val customLines by viewModel.customLines.collectAsStateWithLifecycle()
    val selectedMaterialForDetails by viewModel.selectedMaterialForDetails.collectAsStateWithLifecycle()

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val historySearch by viewModel.historySearch.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val bins by viewModel.bins.collectAsStateWithLifecycle()
    val selectedZone by viewModel.selectedZone.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val activeAlertBanner by viewModel.activeAlertBanner.collectAsStateWithLifecycle()

    val isSingleShotScannerOpen by viewModel.isSingleShotScannerOpen.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val scannedItem by viewModel.scannedItem.collectAsStateWithLifecycle()
    val scannedBarcode by viewModel.scannedBarcode.collectAsStateWithLifecycle()
    val isTransferInFlight by viewModel.isTransferInFlight.collectAsStateWithLifecycle()
    val lastTransferSuccess by viewModel.lastTransferSuccess.collectAsStateWithLifecycle()

    val isVoiceStudioOpen by viewModel.isVoiceStudioOpen.collectAsStateWithLifecycle()
    val isOcrScannerOpen by viewModel.isOcrScannerOpen.collectAsStateWithLifecycle()
    val isAuthDialogOpen by viewModel.isAuthDialogOpen.collectAsStateWithLifecycle()
    val isQuickAddOpen by viewModel.isQuickAddOpen.collectAsStateWithLifecycle()
    val isAddLocationOpen by viewModel.isAddLocationOpen.collectAsStateWithLifecycle()
    val operationItem by viewModel.operationItem.collectAsStateWithLifecycle()
    val operationType by viewModel.operationType.collectAsStateWithLifecycle()
    val itemToDelete by viewModel.itemToDelete.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val barcodeNotFoundEvent by viewModel.barcodeNotFoundEvent.collectAsStateWithLifecycle()
    val prefilledSku by viewModel.prefilledSku.collectAsStateWithLifecycle()
    val isAuditDialogOpen by viewModel.isAuditDialogOpen.collectAsStateWithLifecycle()
    val auditMode by viewModel.auditMode.collectAsStateWithLifecycle()
    val auditSelectedRackId by viewModel.auditSelectedRackId.collectAsStateWithLifecycle()
    val auditPhysicalCounts by viewModel.auditPhysicalCounts.collectAsStateWithLifecycle()
    val isReconciling by viewModel.isReconciling.collectAsStateWithLifecycle()
    val isGmailDispatcherOpen by viewModel.isGmailDispatcherOpen.collectAsStateWithLifecycle()

    // Show toast feedback
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Dynamic RTL/LTR Layout Direction
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                WarehouseTopBar(
                    isArabic = isArabic,
                    isDarkMode = isDarkMode,
                    currentPlantId = currentPlantId,
                    plants = plants,
                    currentUserRole = currentUserRole,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleTheme = { viewModel.toggleDarkMode() },
                    onSelectPlant = { viewModel.switchPlant(it) },
                    onOpenScanner = { viewModel.openScanner() },
                    onOpenOcrScanner = { viewModel.openOcrScanner() },
                    onOpenAudit = { viewModel.openAuditDialog() },
                    onOpenGmail = { viewModel.openGmailDispatcher() },
                    onOpenAuth = { viewModel.openAuthDialog() }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    WarehouseFloatingActionBar(
                        isArabic = isArabic,
                        onOpenQuickAdd = { viewModel.openQuickAdd() },
                        onOpenScanner = { viewModel.openScanner() },
                        onOpenVoice = { viewModel.openVoiceStudio() }
                    )
                    WarehouseBottomNav(
                        isArabic = isArabic,
                        selectedTab = selectedTab,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        isArabic = isArabic,
                        currentPlantId = currentPlantId,
                        items = items,
                        transactions = transactions,
                        plants = plants,
                        onSelectTab = { viewModel.selectTab(it) },
                        onOpenScanner = { viewModel.openScanner() },
                        onOpenQuickAdd = { viewModel.openQuickAdd() },
                        onOpenItemDetails = { viewModel.openMaterialDetails(it) },
                        onUndoTransaction = { viewModel.undoTransaction(it) },
                        onExportCsv = {
                            val csv = viewModel.generateInventoryCsv()
                            shareCsvData(context, "warehouse_inventory_${currentPlantId}.csv", csv)
                        }
                    )

                    1 -> InventoryScreen(
                        isArabic = isArabic,
                        currentPlantId = currentPlantId,
                        currentUserRole = currentUserRole,
                        items = items,
                        selectedSkus = selectedSkus,
                        searchQuery = searchQuery,
                        viewMode = viewMode,
                        selectedLinesFilter = selectedLinesFilter,
                        statusFilter = statusFilter,
                        savedFilters = savedFilters,
                        activeSavedFilter = activeSavedFilter,
                        customLines = customLines,
                        alertBanner = activeAlertBanner,
                        onDismissAlert = { viewModel.dismissAlertBanner() },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onToggleSelectSku = { viewModel.toggleSkuSelection(it) },
                        onSelectAll = { viewModel.selectAllSkus(it) },
                        onClearSelection = { viewModel.clearSkuSelection() },
                        onExportExcel = {
                            val csv = viewModel.generateInventoryCsv()
                            shareCsvData(context, "warehouse_inventory_${currentPlantId}.csv", csv)
                        },
                        onOpenOperation = { item, opType ->
                            viewModel.openOperation(item, opType)
                        },
                        onDeleteItem = { item ->
                            viewModel.confirmDeleteItem(item)
                        },
                        onSetViewMode = { viewModel.setViewMode(it) },
                        onToggleLineFilter = { viewModel.toggleLineFilter(it) },
                        onClearLineFilter = { viewModel.clearLineFilter() },
                        onApplySavedFilter = { viewModel.applySavedFilter(it) },
                        onSaveCurrentFilter = { viewModel.saveCurrentFilter(it) },
                        onDeleteSavedFilter = { viewModel.deleteSavedFilter(it) },
                        onOpenMaterialDetails = { viewModel.openMaterialDetails(it) },
                        onCopySku = { copyToClipboard(context, it, isArabic) },
                        onBulkAssignLine = { skus, line -> viewModel.executeBulkAssignLine(skus, line) },
                        onBulkToggleActive = { skus, active -> viewModel.executeBulkToggleActive(skus, active) },
                        onBulkSetPriority = { skus, prio -> viewModel.executeBulkSetPriority(skus, prio) }
                    )

                    2 -> ProductionLinesScreen(
                        isArabic = isArabic,
                        currentPlantId = currentPlantId,
                        items = items,
                        customLines = customLines,
                        onOpenMaterialDetails = { viewModel.openMaterialDetails(it) },
                        onCreateProductionLine = { viewModel.createProductionLine(it) },
                        onAssignLineToMaterial = { sku, line, req -> viewModel.assignLineToMaterial(sku, line, req) },
                        onRemoveLineFromMaterial = { sku, line -> viewModel.removeLineFromMaterial(sku, line) },
                        onCopySku = { copyToClipboard(context, it, isArabic) }
                    )

                    3 -> HistoryScreen(
                        isArabic = isArabic,
                        currentPlantId = currentPlantId,
                        transactions = transactions,
                        historyFilter = historyFilter,
                        historySearch = historySearch,
                        onFilterChange = { viewModel.setHistoryFilter(it) },
                        onSearchChange = { viewModel.setHistorySearch(it) },
                        onExportCsv = {
                            val csv = viewModel.generateTransactionsCsv()
                            shareCsvData(context, "warehouse_transactions_${currentPlantId}.csv", csv)
                        },
                        onExportExcel = {
                            val csv = viewModel.generateTransactionsCsv()
                            shareCsvData(context, "warehouse_transactions_${currentPlantId}.csv", csv)
                        },
                        onUndoTransaction = { viewModel.undoTransaction(it) }
                    )

                    4 -> ManagementScreen(
                        isArabic = isArabic,
                        currentPlantId = currentPlantId,
                        plants = plants,
                        users = users,
                        onSwitchPlant = { viewModel.switchPlant(it) },
                        onAddNewPlant = { id, en, ar -> viewModel.addNewPlant(id, en, ar) },
                        onDeletePlant = { viewModel.deletePlant(it) },
                        onUpdateUserRole = { userId, role -> viewModel.updateUserRole(userId, role) },
                        onToggleUserLock = { viewModel.toggleUserLock(it) }
                    )
                }
            }
        }

        // Master Material Specification & Line Details Dialog (Phase 11)
        if (selectedMaterialForDetails != null) {
            MaterialDetailDialog(
                item = selectedMaterialForDetails!!,
                availableLines = customLines,
                isArabic = isArabic,
                onDismiss = { viewModel.closeMaterialDetails() },
                onCopySku = { copyToClipboard(context, it, isArabic) },
                onAssignLine = { sku, line, reqQty ->
                    viewModel.assignLineToMaterial(sku, line, reqQty)
                },
                onRemoveLine = { sku, line ->
                    viewModel.removeLineFromMaterial(sku, line)
                },
                onUpdateLineReq = { sku, line, reqQty ->
                    viewModel.updateMaterialLineRequirement(sku, line, reqQty)
                },
                onOpenReceive = { viewModel.openOperation(it, "RECEIVE") },
                onOpenIssue = { viewModel.openOperation(it, "ISSUE") },
                onOpenRelocate = { viewModel.openOperation(it, "TRANSFER") }
            )
        }

        // Advanced Smart Barcode Scanning & Movement Center
        if (isSingleShotScannerOpen) {
            AdvancedBarcodeScannerDialog(
                isArabic = isArabic,
                scannerState = scannerState,
                scannedItem = scannedItem,
                scannedBarcode = scannedBarcode,
                isTransferInFlight = isTransferInFlight,
                lastTransferSuccess = lastTransferSuccess,
                availablePlants = plants,
                allCatalogItems = items,
                onDismiss = { viewModel.closeScanner() },
                onStartScan = { viewModel.startScanning() },
                onPauseScan = { viewModel.pauseScanning() },
                onResumeScan = { viewModel.resumeScanning() },
                onFinishSession = { viewModel.finishScanningSession() },
                onScanAgain = { viewModel.scanAgain() },
                onBarcodeScanned = { barcode -> viewModel.processBarcodeScan(barcode) },
                onSelectQuickItem = { item -> viewModel.selectScannedItemDirectly(item) },
                onAddQuantity = { qty, factory, loc, notes ->
                    viewModel.executeScannedAddQuantity(qty, factory, loc, notes)
                },
                onIssueQuantity = { qty, dept, notes ->
                    viewModel.executeScannedIssueQuantity(qty, dept, notes)
                },
                onChangeLocation = { newLoc, notes ->
                    viewModel.executeScannedLocationChange(newLoc, notes)
                },
                onInterFactoryTransfer = { srcFactory, dstFactory, qty, isFull, dstLoc, dstWh, notes ->
                    viewModel.executeScannedInterFactoryTransfer(
                        srcFactory,
                        dstFactory,
                        qty,
                        isFull,
                        dstLoc,
                        dstWh,
                        notes
                    )
                }
            )
        }

        // Voice Command Studio (Screenshot 1)
        if (isVoiceStudioOpen) {
            VoiceCommandStudioDialog(
                isArabic = isArabic,
                currentPlantId = currentPlantId,
                onDismiss = { viewModel.closeVoiceStudio() },
                onExecuteCommand = { cmd ->
                    viewModel.executeVoiceCommand(cmd)
                }
            )
        }

        // Blueprint & Schematic OCR Scanner (Screenshot 2)
        if (isOcrScannerOpen) {
            SchematicOcrScannerDialog(
                isArabic = isArabic,
                currentPlantId = currentPlantId,
                onDismiss = { viewModel.closeOcrScanner() },
                onBatchCommit = { extractedItems ->
                    viewModel.commitExtractedOcrItems(extractedItems)
                }
            )
        }

        // Firebase Operator Authentication Dialog (Email/Password, Google & Multi-User Concurrency)
        if (isAuthDialogOpen) {
            AuthDialog(
                authService = authService,
                scope = coroutineScope,
                isArabic = isArabic,
                currentFactoryId = currentPlantId,
                onDismiss = { viewModel.closeAuthDialog() }
            )
        }

        // Gmail Warehouse Alert & Report Dispatcher Dialog
        if (isGmailDispatcherOpen) {
            GmailDispatcherDialog(
                isArabic = isArabic,
                currentFactoryId = currentPlantId,
                currentFactoryName = plants.firstOrNull { it.id == currentPlantId }?.let { if (isArabic) it.nameAr else it.nameEn } ?: currentPlantId,
                items = items,
                operatorName = authService.operatorProfile.value?.displayName ?: "WMS Supervisor",
                onDismiss = { viewModel.closeGmailDispatcher() }
            )
        }

        if (isQuickAddOpen) {
            QuickAddSkuDialog(
                isArabic = isArabic,
                initialSku = prefilledSku ?: "",
                onDismiss = {
                    viewModel.closeQuickAdd()
                    viewModel.clearPrefilledSku()
                },
                onSubmit = { sku, name, qty, loc, line, daily, plan ->
                    viewModel.addNewSku(sku, name, qty, loc, line, daily, plan)
                    viewModel.clearPrefilledSku()
                }
            )
        }

        // Barcode Not Found Modal Dialog with Quick Add Action
        if (barcodeNotFoundEvent != null) {
            BarcodeNotFoundDialog(
                barcode = barcodeNotFoundEvent!!.barcode,
                factoryId = barcodeNotFoundEvent!!.factoryId,
                factoryName = barcodeNotFoundEvent!!.factoryName,
                isArabic = isArabic,
                onDismiss = { viewModel.dismissBarcodeNotFoundDialog() },
                onAddItemToCurrentFactory = { viewModel.onAddScannedBarcodeToCurrentFactory() }
            )
        }

        // Comprehensive Inventory Audit & Stocktaking System Dialog (Mode A & Mode B)
        if (isAuditDialogOpen) {
            InventoryAuditDialog(
                isArabic = isArabic,
                currentFactoryId = currentPlantId,
                currentFactoryName = plants.firstOrNull { it.id == currentPlantId }?.let { if (isArabic) it.nameAr else it.nameEn } ?: currentPlantId,
                items = items,
                auditMode = auditMode,
                selectedRackId = auditSelectedRackId,
                physicalCounts = auditPhysicalCounts,
                isReconciling = isReconciling,
                onDismiss = { viewModel.closeAuditDialog() },
                onSelectMode = { viewModel.setAuditMode(it) },
                onSelectRack = { viewModel.setAuditRackId(it) },
                onUpdatePhysicalCount = { sku, count -> viewModel.updateAuditPhysicalCount(sku, count) },
                onReconcile = { notes -> viewModel.executeReconcileAudit(notes) }
            )
        }

        if (isAddLocationOpen) {
            AddLocationDialog(
                isArabic = isArabic,
                onDismiss = { viewModel.closeAddLocation() },
                onSubmit = { zone, aisle, shelf, bin, cap ->
                    viewModel.addNewLocation(zone, aisle, shelf, bin, cap)
                }
            )
        }

        // Material Safe Deletion Dialog
        if (itemToDelete != null) {
            ConfirmDeleteMaterialDialog(
                item = itemToDelete!!,
                isArabic = isArabic,
                onDismiss = { viewModel.dismissDeleteDialog() },
                onConfirm = { viewModel.executeDeleteItem() }
            )
        }

        // Warehouse Operation Dialog
        if (operationItem != null && operationType != null) {
            WarehouseOperationDialog(
                item = operationItem!!,
                type = operationType!!,
                isArabic = isArabic,
                plants = plants,
                onDismiss = { viewModel.closeOperation() },
                onReceive = { sku, qty, notes -> viewModel.receiveStock(sku, qty, notes) },
                onIssue = { sku, qty, notes -> viewModel.issueStock(sku, qty, notes) },
                onTransfer = { sku, newLoc, notes -> viewModel.relocateStock(sku, newLoc, notes) },
                onInterPlant = { sku, toPlant, qty -> viewModel.transferPlant(sku, toPlant, qty) },
                onUpdatePlan = { sku, target, current, line, block, daily ->
                    viewModel.updatePlanData(sku, target, current, line, block, daily)
                },
                onCycleCount = { sku, count, reason ->
                    viewModel.reconcileCycleCount(sku, count, reason)
                },
                onEditMaterial = { sku, name, unit, min, max, supplier, active ->
                    viewModel.editMaterialAttributes(sku, name, unit, min, max, supplier, active)
                }
            )
        }
    }
}
