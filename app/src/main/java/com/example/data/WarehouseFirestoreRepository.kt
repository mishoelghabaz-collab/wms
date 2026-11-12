package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.InventoryTransaction
import com.example.model.TransactionType
import com.example.model.WarehouseItem
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@IgnoreExtraProperties
data class FirestoreWarehouseItem(
    @DocumentId val id: String = "",
    val sku: String = "",
    val name: String = "",
    val quantity: Long = 0L,
    val unit: String = "PCS",
    val location: String = "",
    val zone: String = "A",
    val aisle: String = "01",
    val shelf: String = "01",
    val bin: String = "01",
    val block: String = "غير محدد",
    val line: String = "volvo LFT",
    val perHarness: Long = 0L,
    val dailyUsage: Long = 0L,
    val planTarget: Long = 2500L,
    val planCurrent: Long = 0L,
    val plantId: String = "P7",
    val minStockThreshold: Long = 800L,
    val maxStockCapacity: Long = 4000L,
    val lastDispenseTime: String = "لا يوجد صرف سابق",
    val lastDispenseNote: String = "مسجل في المخزن الرئيسي",
    val supplier: String = "Volvo Global Logistics",
    val isActive: Boolean = true,
    val warehouse: String = "WH-01",
    val reservedQuantity: Long = 0L,
    val issuedQuantity: Long = 0L,
    val productionLines: List<String> = emptyList(),
    val lineRequirements: Map<String, Long> = emptyMap(),
    val customer: String = "Volvo Trucks",
    val priority: String = "NORMAL",
    val minLot: Long = 100L,
    val notes: String = ""
) {
    fun toDomain(): WarehouseItem = WarehouseItem(
        sku = sku,
        name = name,
        quantity = quantity.toInt(),
        unit = unit,
        location = location,
        zone = zone,
        aisle = aisle,
        shelf = shelf,
        bin = bin,
        block = block,
        line = line,
        productionLines = if (productionLines.isNotEmpty()) productionLines else listOf(line.ifEmpty { "Volvo LFT" }),
        lineRequirements = lineRequirements.mapValues { it.value.toInt() },
        perHarness = perHarness.toInt(),
        dailyUsage = dailyUsage.toInt(),
        planTarget = planTarget.toInt(),
        planCurrent = planCurrent.toInt(),
        plantId = plantId,
        minStockThreshold = minStockThreshold.toInt(),
        maxStockCapacity = maxStockCapacity.toInt(),
        lastDispenseTime = lastDispenseTime,
        lastDispenseNote = lastDispenseNote,
        supplier = supplier,
        customer = customer,
        priority = priority,
        minLot = minLot.toInt(),
        isActive = isActive,
        warehouse = warehouse,
        reservedQuantity = reservedQuantity.toInt(),
        issuedQuantity = issuedQuantity.toInt(),
        notes = notes
    )

    companion object {
        fun fromDomain(item: WarehouseItem): FirestoreWarehouseItem = FirestoreWarehouseItem(
            id = "${item.plantId}_${item.sku}",
            sku = item.sku,
            name = item.name,
            quantity = item.quantity.toLong(),
            unit = item.unit,
            location = item.location,
            zone = item.zone,
            aisle = item.aisle,
            shelf = item.shelf,
            bin = item.bin,
            block = item.block,
            line = item.line,
            productionLines = item.productionLines,
            lineRequirements = item.lineRequirements.mapValues { it.value.toLong() },
            perHarness = item.perHarness.toLong(),
            dailyUsage = item.dailyUsage.toLong(),
            planTarget = item.planTarget.toLong(),
            planCurrent = item.planCurrent.toLong(),
            plantId = item.plantId,
            minStockThreshold = item.minStockThreshold.toLong(),
            maxStockCapacity = item.maxStockCapacity.toLong(),
            lastDispenseTime = item.lastDispenseTime,
            lastDispenseNote = item.lastDispenseNote,
            supplier = item.supplier,
            customer = item.customer,
            priority = item.priority,
            minLot = item.minLot.toLong(),
            isActive = item.isActive,
            warehouse = item.warehouse,
            reservedQuantity = item.reservedQuantity.toLong(),
            issuedQuantity = item.issuedQuantity.toLong(),
            notes = item.notes
        )
    }
}

@IgnoreExtraProperties
data class FirestoreTransaction(
    @DocumentId val id: String = "",
    val timestamp: Long = 0L,
    val formattedTime: String = "",
    val sku: String = "",
    val plantId: String = "P7",
    val toPlantId: String = "",
    val type: String = "",
    val quantityDelta: Long = 0L,
    val previousQty: Long = 0L,
    val newQty: Long = 0L,
    val fromLocation: String = "",
    val toLocation: String = "",
    val operator: String = "",
    val notes: String = "",
    val status: String = "COMPLETED",
    val canUndo: Boolean = true
) {
    fun toDomain(): InventoryTransaction = InventoryTransaction(
        id = id,
        timestamp = timestamp,
        formattedTime = formattedTime,
        sku = sku,
        plantId = plantId,
        toPlantId = toPlantId,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.PLAN_UPDATE },
        quantityDelta = quantityDelta.toInt(),
        previousQty = previousQty.toInt(),
        newQty = newQty.toInt(),
        fromLocation = fromLocation,
        toLocation = toLocation,
        operator = operator,
        notes = notes,
        status = status,
        canUndo = canUndo
    )

    companion object {
        fun fromDomain(trx: InventoryTransaction): FirestoreTransaction = FirestoreTransaction(
            id = trx.id,
            timestamp = trx.timestamp,
            formattedTime = trx.formattedTime,
            sku = trx.sku,
            plantId = trx.plantId,
            toPlantId = trx.toPlantId,
            type = trx.type.name,
            quantityDelta = trx.quantityDelta.toLong(),
            previousQty = trx.previousQty.toLong(),
            newQty = trx.newQty.toLong(),
            fromLocation = trx.fromLocation,
            toLocation = trx.toLocation,
            operator = trx.operator,
            notes = trx.notes,
            status = trx.status,
            canUndo = trx.canUndo
        )
    }
}

class WarehouseFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val itemsCollection = db.collection("items")
    private val transactionsCollection = db.collection("transactions")
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Real-time stream of warehouse items across plants
     */
    fun observeItems(): Flow<List<WarehouseItem>> {
        return itemsCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("FirestoreRepo", "Error observing items: ${error.message}", error)
                emit(emptyList())
            }
    }

    /**
     * Real-time stream of inventory transactions
     */
    fun observeTransactions(): Flow<List<InventoryTransaction>> {
        return transactionsCollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreTransaction::class.java)?.toDomain()
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("FirestoreRepo", "Error observing transactions: ${error.message}", error)
                emit(emptyList())
            }
    }

    /**
     * Seed or sync initial items if collection is empty
     */
    suspend fun seedInitialItemsIfEmpty(defaultItems: List<WarehouseItem>) {
        try {
            val existing = itemsCollection.limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                defaultItems.forEach { item ->
                    val docId = "${item.plantId}_${item.sku}"
                    val docRef = itemsCollection.document(docId)
                    batch.set(docRef, FirestoreWarehouseItem.fromDomain(item))
                }
                batch.commit().await()
                Log.d("FirestoreRepo", "Successfully seeded ${defaultItems.size} warehouse items to Firestore")
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "Skipping seeding, offline or error: ${e.message}")
        }
    }

    /**
     * Atomically executes stock adjustment (inbound receive or outbound issue)
     * Enforces mathematical integrity and prevents negative stock race conditions.
     */
    suspend fun executeAtomicStockAdjustment(
        plantId: String,
        sku: String,
        delta: Int,
        type: TransactionType,
        operator: String,
        notes: String,
        fromLoc: String = "",
        toLoc: String = "",
        allowNegative: Boolean = false,
        clientTransactionId: String? = null
    ): Result<Pair<WarehouseItem, InventoryTransaction>> {
        val docId = "${plantId}_${sku}"
        val itemRef = itemsCollection.document(docId)
        val now = System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(now))
        val trxId = clientTransactionId ?: "TRX-${now % 1000000}"
        val trxRef = transactionsCollection.document(trxId)

        return try {
            val result = db.runTransaction { tx ->
                val trxSnap = tx.get(trxRef)
                if (trxSnap.exists()) {
                    val existingTrx = trxSnap.toObject(FirestoreTransaction::class.java)?.toDomain()
                        ?: throw IllegalStateException("Transaction already processed")
                    val existingItem = tx.get(itemRef).toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                        ?: throw IllegalStateException("Material record missing")
                    return@runTransaction Pair(existingItem, existingTrx)
                }

                val snapshot = tx.get(itemRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Material $sku does not exist in plant $plantId")
                }

                val currentDomain = snapshot.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                    ?: throw IllegalStateException("Failed to parse item $sku")

                val currentQty = currentDomain.quantity
                val targetQty = currentQty + delta

                if (targetQty < 0 && !allowNegative) {
                    throw IllegalStateException("Negative stock prevented. Current: $currentQty PCS, Requested deduction: ${-delta} PCS")
                }

                val newIssued = if (delta < 0) currentDomain.issuedQuantity + (-delta) else currentDomain.issuedQuantity

                val updatedItem = currentDomain.copy(
                    quantity = targetQty,
                    issuedQuantity = newIssued,
                    lastDispenseTime = if (delta < 0) formattedTime else currentDomain.lastDispenseTime,
                    lastDispenseNote = if (delta < 0) notes else currentDomain.lastDispenseNote
                )

                val trx = InventoryTransaction(
                    id = trxId,
                    timestamp = now,
                    formattedTime = formattedTime,
                    sku = sku,
                    plantId = plantId,
                    type = type,
                    quantityDelta = delta,
                    previousQty = currentQty,
                    newQty = targetQty,
                    fromLocation = if (fromLoc.isNotBlank()) fromLoc else currentDomain.location,
                    toLocation = if (toLoc.isNotBlank()) toLoc else currentDomain.location,
                    operator = operator,
                    notes = notes,
                    status = "COMPLETED",
                    canUndo = true
                )

                // Atomic writes in single commit
                tx.set(itemRef, FirestoreWarehouseItem.fromDomain(updatedItem), SetOptions.merge())
                tx.set(trxRef, FirestoreTransaction.fromDomain(trx))

                Pair(updatedItem, trx)
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Atomic transaction failed for $sku: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atomically transfers material between two factories (Full or Partial).
     * Locks both source and destination documents in a single atomic commit.
     * Guarantees all-or-nothing consistency and idempotency against double submission.
     */
    suspend fun executeAtomicInterFactoryTransfer(
        sourcePlant: String,
        destPlant: String,
        sku: String,
        transferQty: Int,
        operator: String,
        destLocation: String = "",
        destWarehouse: String = "WH-01",
        notes: String = "",
        clientTransactionId: String? = null
    ): Result<Triple<WarehouseItem, WarehouseItem, InventoryTransaction>> {
        if (transferQty <= 0) {
            return Result.failure(IllegalArgumentException("Transfer quantity must be greater than zero"))
        }
        if (sourcePlant == destPlant) {
            return Result.failure(IllegalArgumentException("Source and destination factory cannot be identical"))
        }

        val srcDocId = "${sourcePlant}_${sku}"
        val dstDocId = "${destPlant}_${sku}"
        val srcRef = itemsCollection.document(srcDocId)
        val dstRef = itemsCollection.document(dstDocId)
        val now = System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(now))
        val trxId = clientTransactionId ?: "TRX-XFER-${now % 1000000}"
        val trxRef = transactionsCollection.document(trxId)

        return try {
            val result = db.runTransaction { tx ->
                // Idempotency check: if transaction already committed, return without re-executing
                val trxSnap = tx.get(trxRef)
                if (trxSnap.exists()) {
                    val existingTrx = trxSnap.toObject(FirestoreTransaction::class.java)?.toDomain()
                        ?: throw IllegalStateException("Duplicate transaction record found")
                    val existingSrc = tx.get(srcRef).toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                        ?: throw IllegalStateException("Source material missing")
                    val existingDst = tx.get(dstRef).toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                        ?: throw IllegalStateException("Destination material missing")
                    return@runTransaction Triple(existingSrc, existingDst, existingTrx)
                }

                // 1. Read source material
                val srcSnap = tx.get(srcRef)
                if (!srcSnap.exists()) {
                    throw IllegalStateException("Material $sku does not exist in source factory $sourcePlant")
                }
                val srcDomain = srcSnap.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                    ?: throw IllegalStateException("Failed to parse source item $sku")

                // 2. Validate available stock
                if (srcDomain.quantity < transferQty) {
                    throw IllegalStateException("Insufficient stock in factory $sourcePlant: Available ${srcDomain.quantity} ${srcDomain.unit}, requested $transferQty")
                }

                // 3. Read destination material (or prepare new record if first time in dest plant)
                val dstSnap = tx.get(dstRef)
                val dstDomain = if (dstSnap.exists()) {
                    dstSnap.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                } else null

                val newSrcQty = srcDomain.quantity - transferQty
                val newDstQty = (dstDomain?.quantity ?: 0) + transferQty

                val finalDestLoc = if (destLocation.isNotBlank()) destLocation else (dstDomain?.location ?: srcDomain.location)
                val finalDestZone = finalDestLoc.take(1).uppercase().ifEmpty { "A" }

                val updatedSrc = srcDomain.copy(
                    quantity = newSrcQty,
                    lastDispenseTime = formattedTime,
                    lastDispenseNote = "تحويل لمصنع $destPlant: -$transferQty ${srcDomain.unit}"
                )

                val updatedDst = (dstDomain ?: srcDomain.copy(
                    plantId = destPlant,
                    location = finalDestLoc,
                    zone = finalDestZone,
                    warehouse = destWarehouse
                )).copy(
                    quantity = newDstQty,
                    location = finalDestLoc,
                    warehouse = destWarehouse,
                    lastDispenseTime = formattedTime,
                    lastDispenseNote = "استلام محول من مصنع $sourcePlant: +$transferQty ${srcDomain.unit}"
                )

                val trx = InventoryTransaction(
                    id = trxId,
                    timestamp = now,
                    formattedTime = formattedTime,
                    sku = sku,
                    plantId = sourcePlant,
                    toPlantId = destPlant,
                    type = TransactionType.INTER_PLANT_TRANSFER,
                    quantityDelta = -transferQty,
                    previousQty = srcDomain.quantity,
                    newQty = newSrcQty,
                    fromLocation = "${sourcePlant}:${srcDomain.location}",
                    toLocation = "${destPlant}:${finalDestLoc}",
                    operator = operator,
                    notes = notes.ifEmpty { "تحويل بين المصانع ($sourcePlant → $destPlant): $transferQty ${srcDomain.unit}" },
                    status = "COMPLETED",
                    canUndo = true
                )

                // Atomic writes in single transaction commit
                tx.set(srcRef, FirestoreWarehouseItem.fromDomain(updatedSrc), SetOptions.merge())
                tx.set(dstRef, FirestoreWarehouseItem.fromDomain(updatedDst), SetOptions.merge())
                tx.set(trxRef, FirestoreTransaction.fromDomain(trx))

                Triple(updatedSrc, updatedDst, trx)
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Inter-factory transfer failed for $sku: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atomically relocates material to a new bin within the same factory.
     */
    suspend fun executeAtomicLocationChange(
        plantId: String,
        sku: String,
        newLocation: String,
        operator: String,
        notes: String = ""
    ): Result<Pair<WarehouseItem, InventoryTransaction>> {
        val docId = "${plantId}_${sku}"
        val itemRef = itemsCollection.document(docId)
        val now = System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(now))
        val trxId = "LOC-${now % 1000000}"
        val trxRef = transactionsCollection.document(trxId)

        return try {
            val result = db.runTransaction { tx ->
                val snapshot = tx.get(itemRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Material $sku does not exist in plant $plantId")
                }
                val current = snapshot.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                    ?: throw IllegalStateException("Failed to parse item $sku")

                val prevLoc = current.location
                val newZone = newLocation.take(1).uppercase().ifEmpty { current.zone }

                val updated = current.copy(
                    location = newLocation.uppercase(),
                    zone = newZone,
                    lastDispenseTime = formattedTime,
                    lastDispenseNote = "نقل موقع من $prevLoc إلى $newLocation"
                )

                val trx = InventoryTransaction(
                    id = trxId,
                    timestamp = now,
                    formattedTime = formattedTime,
                    sku = sku,
                    plantId = plantId,
                    type = TransactionType.RELOCATE_BIN,
                    quantityDelta = 0,
                    previousQty = current.quantity,
                    newQty = current.quantity,
                    fromLocation = prevLoc,
                    toLocation = newLocation.uppercase(),
                    operator = operator,
                    notes = notes.ifEmpty { "نقل موقع الرف الداخلي" },
                    status = "COMPLETED",
                    canUndo = true
                )

                tx.set(itemRef, FirestoreWarehouseItem.fromDomain(updated), SetOptions.merge())
                tx.set(trxRef, FirestoreTransaction.fromDomain(trx))

                Pair(updated, trx)
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Location change failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Physical Cycle Count reconciliation: reconciles system records with actual physical shelf count.
     */
    suspend fun executeCycleCountReconciliation(
        plantId: String,
        sku: String,
        physicalCount: Int,
        operator: String,
        reason: String
    ): Result<Pair<WarehouseItem, InventoryTransaction>> {
        val docId = "${plantId}_${sku}"
        val itemRef = itemsCollection.document(docId)
        val now = System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(now))
        val trxId = "AUD-${now % 1000000}"
        val trxRef = transactionsCollection.document(trxId)

        return try {
            val result = db.runTransaction { tx ->
                val snapshot = tx.get(itemRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Material $sku does not exist in plant $plantId")
                }

                val currentDomain = snapshot.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                    ?: throw IllegalStateException("Failed to parse item $sku")

                val systemQty = currentDomain.quantity
                val discrepancy = physicalCount - systemQty

                val updatedItem = currentDomain.copy(
                    quantity = physicalCount,
                    lastDispenseTime = formattedTime,
                    lastDispenseNote = "تسوية جرد دوري: فرق $discrepancy PCS ($reason)"
                )

                val trx = InventoryTransaction(
                    id = trxId,
                    timestamp = now,
                    formattedTime = formattedTime,
                    sku = sku,
                    plantId = plantId,
                    type = TransactionType.CYCLE_COUNT_AUDIT,
                    quantityDelta = discrepancy,
                    previousQty = systemQty,
                    newQty = physicalCount,
                    fromLocation = currentDomain.location,
                    toLocation = currentDomain.location,
                    operator = operator,
                    notes = "تسوية جرد: الرصيد الفعلي $physicalCount مقابل النظامي $systemQty ($reason)",
                    status = "COMPLETED",
                    canUndo = true
                )

                tx.set(itemRef, FirestoreWarehouseItem.fromDomain(updatedItem), SetOptions.merge())
                tx.set(trxRef, FirestoreTransaction.fromDomain(trx))

                Pair(updatedItem, trx)
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Cycle count reconciliation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete material with safeguard: strict refusal if quantity > 0
     */
    suspend fun deleteItemSafe(plantId: String, sku: String): Result<Unit> {
        val docId = "${plantId}_${sku}"
        val itemRef = itemsCollection.document(docId)
        return try {
            val snap = itemRef.get().await()
            if (!snap.exists()) {
                return Result.success(Unit)
            }
            val qty = snap.getLong("quantity") ?: 0L
            if (qty > 0) {
                return Result.failure(
                    IllegalStateException("حماية المخزون: لا يمكن حذف صنف يحتوي على رصيد ($qty PCS). يجب تصفير الرصيد أو صرفه أولاً.")
                )
            }
            itemRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error deleting item: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Hierarchical Factory Collections (Strict Factory Isolation)
     */
    fun factoryItemsCollection(factoryId: String) =
        db.collection("factories").document(factoryId).collection("items")

    fun factoryLinesCollection(factoryId: String) =
        db.collection("factories").document(factoryId).collection("lines")

    fun factoryRacksCollection(factoryId: String) =
        db.collection("factories").document(factoryId).collection("racks")

    fun factoryAuditsCollection(factoryId: String) =
        db.collection("factories").document(factoryId).collection("audits")

    /**
     * Real-time stream of warehouse items strictly scoped to active factory
     */
    fun observeFactoryItems(factoryId: String): Flow<List<WarehouseItem>> {
        return factoryItemsCollection(factoryId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreWarehouseItem::class.java)?.toDomain()
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("FirestoreRepo", "Error observing factory items ($factoryId): ${error.message}", error)
                emit(emptyList())
            }
    }

    /**
     * Real-time stream of production lines strictly isolated to active factory
     */
    fun observeFactoryLines(factoryId: String): Flow<List<String>> {
        return factoryLinesCollection(factoryId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.getString("name") ?: doc.id
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("FirestoreRepo", "Error observing factory lines ($factoryId): ${error.message}", error)
                emit(emptyList())
            }
    }

    /**
     * Create or update a production line scoped strictly to factory
     */
    suspend fun saveFactoryLine(factoryId: String, lineName: String) {
        val lineRef = factoryLinesCollection(factoryId).document(lineName.trim())
        try {
            lineRef.set(mapOf(
                "name" to lineName.trim(),
                "factoryId" to factoryId,
                "updatedAt" to System.currentTimeMillis()
            ), SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving line: ${e.message}", e)
        }
    }

    /**
     * Reconcile inventory audit atomically committing physical counts to Firestore
     * and writing an immutable audit log to factories/{factoryId}/audits/{auditId}.
     */
    suspend fun reconcileInventoryAudit(
        factoryId: String,
        auditMode: String, // "RACK_LEVEL" or "FULL_FACTORY"
        targetRackId: String? = null,
        auditRecords: List<AuditItemRecord>,
        operatorId: String,
        operatorName: String,
        notes: String = ""
    ): Result<InventoryAuditLog> {
        val now = System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(now))
        val auditId = "AUDIT-${now % 1000000}"
        val auditRef = factoryAuditsCollection(factoryId).document(auditId)

        var totalSystem = 0
        var totalPhysical = 0
        var netVar = 0

        val batch = db.batch()

        auditRecords.forEach { record ->
            totalSystem += record.systemCount
            totalPhysical += record.physicalCount
            netVar += record.variance

            // Update item quantity in hierarchical factory collection
            val factoryItemRef = factoryItemsCollection(factoryId).document(record.sku)
            batch.set(
                factoryItemRef,
                mapOf(
                    "quantity" to record.physicalCount.toLong(),
                    "lastAuditTime" to formattedTime,
                    "location" to record.rackId
                ),
                SetOptions.merge()
            )

            // Also keep legacy cross-plant document in sync
            val legacyDocRef = itemsCollection.document("${factoryId}_${record.sku}")
            batch.set(
                legacyDocRef,
                mapOf(
                    "quantity" to record.physicalCount.toLong(),
                    "lastDispenseTime" to "تسوية جردية: $formattedTime"
                ),
                SetOptions.merge()
            )

            // Record transaction ledger entry if variance exists
            if (record.variance != 0) {
                val trxId = "TRX-AUDIT-${now % 100000}-${record.sku}"
                val trxRef = transactionsCollection.document(trxId)
                val trxData = FirestoreTransaction(
                    id = trxId,
                    timestamp = now,
                    formattedTime = formattedTime,
                    sku = record.sku,
                    plantId = factoryId,
                    type = TransactionType.CYCLE_COUNT_AUDIT.name,
                    quantityDelta = record.variance.toLong(),
                    previousQty = record.systemCount.toLong(),
                    newQty = record.physicalCount.toLong(),
                    fromLocation = record.rackId,
                    toLocation = record.rackId,
                    operator = operatorName,
                    notes = "تسوية جردية ($auditMode): ${if (record.variance > 0) "فائض" else "عجز"} ${record.variance} • $notes",
                    status = "COMPLETED",
                    canUndo = true
                )
                batch.set(trxRef, trxData)
            }
        }

        val auditLog = InventoryAuditLog(
            auditId = auditId,
            factoryId = factoryId,
            mode = auditMode,
            rackId = targetRackId,
            timestamp = now,
            formattedTime = formattedTime,
            operatorId = operatorId,
            operatorName = operatorName,
            totalItemsAudited = auditRecords.size,
            totalSystemCount = totalSystem,
            totalPhysicalCount = totalPhysical,
            netVariance = netVar,
            records = auditRecords
        )

        // Commit Audit Document
        val firestoreAuditPayload = mapOf(
            "auditId" to auditId,
            "factoryId" to factoryId,
            "mode" to auditMode,
            "rackId" to (targetRackId ?: ""),
            "timestamp" to now,
            "formattedTime" to formattedTime,
            "operatorId" to operatorId,
            "operatorName" to operatorName,
            "totalItemsAudited" to auditRecords.size.toLong(),
            "totalSystemCount" to totalSystem.toLong(),
            "totalPhysicalCount" to totalPhysical.toLong(),
            "netVariance" to netVar.toLong(),
            "records" to auditRecords.map {
                mapOf(
                    "sku" to it.sku,
                    "name" to it.name,
                    "rackId" to it.rackId,
                    "systemCount" to it.systemCount.toLong(),
                    "physicalCount" to it.physicalCount.toLong(),
                    "variance" to it.variance.toLong()
                )
            }
        )
        batch.set(auditRef, firestoreAuditPayload)

        return try {
            batch.commit().await()
            Log.d("FirestoreRepo", "Audit $auditId successfully reconciled in factory $factoryId")
            Result.success(auditLog)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Audit reconciliation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveItem(item: WarehouseItem) {
        val docId = "${item.plantId}_${item.sku}"
        val docRef = itemsCollection.document(docId)
        try {
            docRef.set(FirestoreWarehouseItem.fromDomain(item), SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving item: ${e.message}", e)
        }
    }

    suspend fun recordTransaction(trx: InventoryTransaction) {
        val docRef = transactionsCollection.document(trx.id)
        try {
            docRef.set(FirestoreTransaction.fromDomain(trx)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error recording transaction: ${e.message}", e)
        }
    }

    suspend fun markTransactionUndone(trxId: String, notes: String) {
        val docRef = transactionsCollection.document(trxId)
        try {
            docRef.update(mapOf("canUndo" to false, "notes" to notes)).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error marking transaction undone: ${e.message}", e)
        }
    }
}

data class AuditItemRecord(
    val sku: String,
    val name: String,
    val rackId: String,
    val systemCount: Int,
    val physicalCount: Int,
    val variance: Int
)

data class InventoryAuditLog(
    val auditId: String,
    val factoryId: String,
    val mode: String,
    val rackId: String? = null,
    val timestamp: Long,
    val formattedTime: String,
    val operatorId: String,
    val operatorName: String,
    val totalItemsAudited: Int,
    val totalSystemCount: Int,
    val totalPhysicalCount: Int,
    val netVariance: Int,
    val records: List<AuditItemRecord>
)
