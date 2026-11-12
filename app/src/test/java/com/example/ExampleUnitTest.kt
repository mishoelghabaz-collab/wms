package com.example

import com.example.model.TransactionType
import com.example.viewmodel.ScannerControlState
import com.example.viewmodel.WarehouseViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test warehouse stock issue and atomic undo`() {
        val vm = WarehouseViewModel()
        val initialItem = vm.items.value.first { it.sku == "13008512" && it.plantId == "P7" }
        val initialQty = initialItem.quantity

        // Issue 500 pcs
        vm.issueStock("13008512", 500, "Test Issue")
        val updatedQty = vm.items.value.first { it.sku == "13008512" && it.plantId == "P7" }.quantity
        assertEquals(initialQty - 500, updatedQty)

        // Find the latest transaction and test undo
        val latestTrx = vm.transactions.value.first()
        assertEquals(TransactionType.ISSUE_OUTBOUND, latestTrx.type)
        assertEquals("13008512", latestTrx.sku)

        // Undo the issue transaction
        vm.undoTransaction(latestTrx.id)
        val restoredQty = vm.items.value.first { it.sku == "13008512" && it.plantId == "P7" }.quantity
        assertEquals(initialQty, restoredQty)
    }

    @Test
    fun `test smart barcode scanning pause and anti-duplicate protection`() {
        val vm = WarehouseViewModel()
        vm.startScanning()
        assertEquals(ScannerControlState.SCANNING, vm.scannerState.value)

        // 1. First scan of material barcode: succeeds and immediately pauses scanner
        val firstScanResult = vm.processBarcodeScan("13008512")
        assertTrue("First scan should succeed", firstScanResult)
        assertEquals(ScannerControlState.PAUSED, vm.scannerState.value)
        assertNotNull(vm.scannedItem.value)
        assertEquals("13008512", vm.scannedItem.value?.sku)

        // 2. Continuous camera frame / duplicate input while paused must be rejected
        val duplicateFrameResult = vm.processBarcodeScan("13008512")
        assertFalse("Duplicate scan frame while paused must be rejected", duplicateFrameResult)

        val anotherCodeWhilePaused = vm.processBarcodeScan("13009444")
        assertFalse("Scanning another code while paused must not overwrite current review", anotherCodeWhilePaused)
        assertEquals("13008512", vm.scannedItem.value?.sku)

        // 3. Manually resume scanning
        vm.resumeScanning()
        assertEquals(ScannerControlState.SCANNING, vm.scannerState.value)

        // 4. Manually pause scanning
        vm.pauseScanning()
        assertEquals(ScannerControlState.PAUSED, vm.scannerState.value)

        // 5. Scan again clears and resets to scanning
        vm.scanAgain()
        assertEquals(ScannerControlState.SCANNING, vm.scannerState.value)
        assertNull(vm.scannedItem.value)
    }

    @Test
    fun `test inter-factory partial transfer`() {
        val vm = WarehouseViewModel()
        val sourcePlant = "P7"
        val destPlant = "P2"
        val sku = "13008512"

        val initialSourceItem = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }
        val initialSourceQty = initialSourceItem.quantity
        val initialDestQty = vm.items.value.find { it.sku == sku && it.plantId == destPlant }?.quantity ?: 0

        // Select item in scanner
        vm.selectScannedItemDirectly(initialSourceItem)

        // Transfer 300 PCS partially from P7 to P2
        val transferQty = 300
        vm.executeScannedInterFactoryTransfer(
            sourceFactory = sourcePlant,
            destFactory = destPlant,
            transferQty = transferQty,
            isFullTransfer = false,
            destLocation = "B0201",
            destWarehouse = "WH-02",
            notes = "Partial batch delivery"
        )

        // Verify Source Factory decreased by exactly transferQty
        val updatedSourceQty = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }.quantity
        assertEquals(initialSourceQty - transferQty, updatedSourceQty)

        // Verify Destination Factory increased by exactly transferQty
        val updatedDestQty = vm.items.value.first { it.sku == sku && it.plantId == destPlant }.quantity
        assertEquals(initialDestQty + transferQty, updatedDestQty)

        // Verify Transaction Record
        val latestTrx = vm.transactions.value.first()
        assertEquals(TransactionType.INTER_PLANT_TRANSFER, latestTrx.type)
        assertEquals(sku, latestTrx.sku)
        assertEquals(sourcePlant, latestTrx.plantId)
        assertEquals(destPlant, latestTrx.toPlantId)
        assertEquals(-transferQty, latestTrx.quantityDelta)
        assertEquals("COMPLETED", latestTrx.status)
    }

    @Test
    fun `test inter-factory full quantity transfer`() {
        val vm = WarehouseViewModel()
        val sourcePlant = "P7"
        val destPlant = "P1"
        val sku = "13008509"

        val initialSourceItem = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }
        val fullQty = initialSourceItem.quantity // 500 PCS
        val initialDestQty = vm.items.value.find { it.sku == sku && it.plantId == destPlant }?.quantity ?: 0

        vm.selectScannedItemDirectly(initialSourceItem)

        // Transfer full quantity
        vm.executeScannedInterFactoryTransfer(
            sourceFactory = sourcePlant,
            destFactory = destPlant,
            transferQty = fullQty,
            isFullTransfer = true,
            destLocation = "C0101",
            notes = "Full plant inventory relocation"
        )

        // Source Factory should now have 0 pieces
        val updatedSourceQty = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }.quantity
        assertEquals(0, updatedSourceQty)

        // Destination Factory should now have initial + fullQty
        val updatedDestQty = vm.items.value.first { it.sku == sku && it.plantId == destPlant }.quantity
        assertEquals(initialDestQty + fullQty, updatedDestQty)
    }

    @Test
    fun `test stock validation prevents over-transfer and over-issue`() {
        val vm = WarehouseViewModel()
        val sourcePlant = "P7"
        val sku = "13008509"
        val item = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }
        val initialQty = item.quantity // 500

        vm.selectScannedItemDirectly(item)

        // Attempt to transfer 600 PCS (more than available 500)
        vm.executeScannedInterFactoryTransfer(
            sourceFactory = sourcePlant,
            destFactory = "P2",
            transferQty = 600,
            isFullTransfer = false
        )

        // Stock must remain unchanged
        val afterTransferQty = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }.quantity
        assertEquals(initialQty, afterTransferQty)

        // Attempt to issue 700 PCS
        vm.executeScannedIssueQuantity(700, "Assembly Line", "Over-issue attempt")
        val afterIssueQty = vm.items.value.first { it.sku == sku && it.plantId == sourcePlant }.quantity
        assertEquals(initialQty, afterIssueQty)
    }

    @Test
    fun `test change material location within factory`() {
        val vm = WarehouseViewModel()
        val item = vm.items.value.first { it.sku == "13008512" && it.plantId == "P7" }
        vm.selectScannedItemDirectly(item)

        vm.executeScannedLocationChange("B0502", "Relocated to higher shelf")

        val updatedItem = vm.items.value.first { it.sku == "13008512" && it.plantId == "P7" }
        assertEquals("B0502", updatedItem.location)
        assertEquals("B", updatedItem.zone)

        val latestTrx = vm.transactions.value.first()
        assertEquals(TransactionType.RELOCATE_BIN, latestTrx.type)
        assertEquals("B0502", latestTrx.toLocation)
    }

    @Test
    fun `test plant switching`() {
        val vm = WarehouseViewModel()
        assertEquals("P7", vm.currentPlantId.value)
        vm.switchPlant("P2")
        assertEquals("P2", vm.currentPlantId.value)
        assertTrue(vm.plants.value.first { it.id == "P2" }.isActive)
    }

    @Test
    fun `test voice command parser`() {
        val vm = WarehouseViewModel()
        val resp = vm.executeVoiceCommand("صرف 200 من 13008512")
        assertTrue(resp.contains("200"))
    }

    @Test
    fun `test multi-line assignment maintains single master inventory quantity`() {
        val vm = WarehouseViewModel()
        val sku = "13008512"
        val item = vm.items.value.first { it.sku == sku && it.plantId == "P7" }
        val initialQty = item.quantity

        // Assign additional production line "Nissan" with requirement 800 PCS
        vm.assignLineToMaterial(sku = sku, line = "Nissan", requirementQty = 800)

        val updatedItem = vm.items.value.first { it.sku == sku && it.plantId == "P7" }
        // 1. Verify item has both lines
        assertTrue(updatedItem.productionLines.contains("Nissan"))
        assertTrue(updatedItem.productionLines.contains("Volvo LFT"))

        // 2. CRITICAL RULE: Physical inventory quantity must NEVER multiply
        assertEquals(initialQty, updatedItem.quantity)

        // 3. Verify line requirement
        assertEquals(800, updatedItem.lineRequirements["Nissan"])

        // 4. Verify linesDisplay formatted with " + "
        assertTrue(updatedItem.linesDisplay.contains(" + "))

        // 5. Verify transaction was logged
        val latestTrx = vm.transactions.value.first()
        assertEquals(TransactionType.LINE_ASSIGNMENT_UPDATE, latestTrx.type)
        assertEquals(sku, latestTrx.sku)
    }

    @Test
    fun `test remove production line with safety check`() {
        val vm = WarehouseViewModel()
        val sku = "13008512"
        // Ensure has multiple lines
        vm.assignLineToMaterial(sku, "Line 1", 200)
        vm.removeLineFromMaterial(sku, "Line 1")

        val updatedItem = vm.items.value.first { it.sku == sku && it.plantId == "P7" }
        assertFalse(updatedItem.productionLines.contains("Line 1"))
    }

    @Test
    fun `test saved filters save and apply`() {
        val vm = WarehouseViewModel()
        val initialFilterCount = vm.savedFilters.value.size

        // Set line filter
        vm.setLineFilter(setOf("CDPO", "Nissan"))

        // Save current filter
        vm.saveCurrentFilter("Custom Alliance Lines")
        assertEquals(initialFilterCount + 1, vm.savedFilters.value.size)

        val saved = vm.savedFilters.value.last()
        assertEquals("Custom Alliance Lines", saved.name)
        assertTrue(saved.selectedLines.contains("CDPO"))

        // Clear filter
        vm.clearLineFilter()
        assertTrue(vm.selectedLinesFilter.value.isEmpty())

        // Apply saved filter
        vm.applySavedFilter(saved)
        assertEquals(setOf("CDPO", "Nissan"), vm.selectedLinesFilter.value)
    }

    @Test
    fun `test bulk line assignment`() {
        val vm = WarehouseViewModel()
        val skus = listOf("13008512", "13009444")

        vm.executeBulkAssignLine(skus, "Assembly Line 9")

        skus.forEach { targetSku ->
            val itm = vm.items.value.first { it.sku == targetSku && it.plantId == "P7" }
            assertTrue(itm.productionLines.contains("Assembly Line 9"))
        }
    }

    @Test
    fun `test real-time inventory search filtering by sku and name`() {
        val vm = WarehouseViewModel()
        
        // 1. Initial query is empty
        vm.setSearchQuery("")
        assertEquals("", vm.searchQuery.value)

        // 2. Search by SKU
        vm.setSearchQuery("13008512")
        assertEquals("13008512", vm.searchQuery.value)
        val skuMatches = vm.items.value.filter {
            it.sku.contains(vm.searchQuery.value, ignoreCase = true) ||
            it.name.contains(vm.searchQuery.value, ignoreCase = true)
        }
        assertTrue(skuMatches.isNotEmpty())
        assertTrue(skuMatches.all { it.sku.contains("13008512") || it.name.contains("13008512") })

        // 3. Search by partial Name
        vm.setSearchQuery("Bearing")
        val nameMatches = vm.items.value.filter {
            it.sku.contains(vm.searchQuery.value, ignoreCase = true) ||
            it.name.contains(vm.searchQuery.value, ignoreCase = true)
        }
        nameMatches.forEach {
            assertTrue(it.sku.contains("Bearing", ignoreCase = true) || it.name.contains("Bearing", ignoreCase = true))
        }

        // 4. Clear search
        vm.setSearchQuery("")
        assertEquals("", vm.searchQuery.value)
    }

    @Test
    fun `test auth diagnostic report clipboard summary and verdict formatting`() {
        val report = com.example.auth.AuthDiagnosticReport(
            verdict = com.example.auth.AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED,
            checkedAt = "12:30:00",
            isEmulator = true,
            deviceSignature = "Google sdk_gphone64 (API 34)",
            googlePlayServicesAvailable = false,
            googlePlayServicesStatusText = "SERVICE_MISSING",
            isWebClientIdConfigured = true,
            webClientIdPreview = "197994110770-x...apps.googleusercontent.com",
            isFirebaseSignedIn = false,
            idTokenValid = false,
            idTokenStatus = "MISSING (Not Signed In)",
            gmailOAuthStatus = "INACTIVE — No authenticated session",
            isGmailAppInstalled = false,
            hasMailtoHandler = false,
            isPreviewBypassActive = true,
            summaryMessageEn = "Emulator running without login."
        )

        val summary = report.toClipboardSummary()
        assertTrue(summary.contains("Verdict: PREVIEW_BYPASS_UNAUTHENTICATED"))
        assertTrue(summary.contains("Preview Auth Bypass Active: true"))
        assertTrue(summary.contains("Firebase Signed In: false"))
        assertTrue(summary.contains("Gmail App Installed (com.google.android.gm): false"))
        assertTrue(summary.contains("Emulator running without login."))
    }

    @Test
    fun `test viewmodel opens auth login vs diagnostics tab properly`() {
        val vm = WarehouseViewModel()
        assertFalse(vm.isAuthDialogOpen.value)
        assertFalse(vm.authDiagnosticsInitialTab.value)

        vm.openAuthDiagnostics()
        assertTrue(vm.isAuthDialogOpen.value)
        assertTrue(vm.authDiagnosticsInitialTab.value)

        vm.closeAuthDialog()
        assertFalse(vm.isAuthDialogOpen.value)
        assertFalse(vm.authDiagnosticsInitialTab.value)

        vm.openAuthDialog()
        assertTrue(vm.isAuthDialogOpen.value)
        assertFalse(vm.authDiagnosticsInitialTab.value)
    }

    @Test
    fun `test warehouse item category inference for charts`() {
        val wireItem = com.example.model.WarehouseItem(sku = "13008512", name = "Engine Harness Wire Loom", quantity = 500, location = "A0101")
        assertEquals("Wiring & Harnesses", wireItem.resolvedCategory)
        assertEquals("الضفائر والأسلاك", com.example.model.getLocalizedCategoryName("Wiring & Harnesses", isArabic = true))

        val terminalItem = com.example.model.WarehouseItem(sku = "13009999", name = "Gold Plated Pin Connector", quantity = 250, location = "B0201")
        assertEquals("Connectors & Terminals", terminalItem.resolvedCategory)
    }
}

