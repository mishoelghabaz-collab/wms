package com.example.gmail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.AuditItemRecord
import com.example.model.WarehouseItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GmailIntegrationService {

    const val DEFAULT_OPERATOR_EMAIL = "mishoelghabaz@gmail.com"

    private fun getCurrentFormattedTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    /**
     * Dispatch an urgent low stock alert email to management via Gmail
     */
    fun sendLowStockAlertEmail(
        context: Context,
        recipientEmail: String = DEFAULT_OPERATOR_EMAIL,
        factoryId: String,
        criticalItems: List<WarehouseItem>,
        operatorName: String = "WMS Supervisor"
    ) {
        val subject = "🚨 [NEXUS WMS Alert] نقص مخزون حرج في مصنع $factoryId - ${criticalItems.size} أصناف"
        val bodyBuilder = StringBuilder()
        bodyBuilder.append("=== تقرير تنبيه المخزون الحرج - نظام NEXUS WMS ===\n")
        bodyBuilder.append("المصنع: $factoryId\n")
        bodyBuilder.append("تاريخ وساعة التنبيه: ${getCurrentFormattedTime()}\n")
        bodyBuilder.append("المشغل المسؤول: $operatorName\n")
        bodyBuilder.append("عدد الأصناف الحرجة: ${criticalItems.size}\n\n")
        bodyBuilder.append("--------------------------------------------------\n")

        criticalItems.forEachIndexed { index, item ->
            bodyBuilder.append("${index + 1}. كود الصنف: ${item.sku}\n")
            bodyBuilder.append("   الاسم: ${item.name}\n")
            bodyBuilder.append("   الرصيد المتبقي: ${item.quantity} ${item.unit}\n")
            bodyBuilder.append("   الحد الأدنى المطلوب: ${item.minStockThreshold} ${item.unit}\n")
            bodyBuilder.append("   موقع الرف بالمخزن: ${item.location}\n")
            bodyBuilder.append("   خط الإنتاج: ${item.linesDisplay}\n")
            bodyBuilder.append("   المورد: ${item.supplier}\n\n")
        }

        bodyBuilder.append("--------------------------------------------------\n")
        bodyBuilder.append("تم إرسال هذا التنبيه التلقائي عبر تطبيق Nexus WMS مع تكامل Gmail المباشر.\n")

        dispatchEmail(context, recipientEmail, subject, bodyBuilder.toString())
    }

    /**
     * Dispatch an official inventory audit / stocktaking report to management via Gmail
     */
    fun sendInventoryAuditReportEmail(
        context: Context,
        recipientEmail: String = DEFAULT_OPERATOR_EMAIL,
        factoryId: String,
        auditMode: String,
        selectedRackId: String,
        totalExpected: Int,
        totalPhysical: Int,
        totalVariance: Int,
        auditRecords: List<AuditItemRecord> = emptyList(),
        auditorName: String,
        notes: String
    ) {
        val subject = "📋 [NEXUS WMS Report] تقرير نتائج الجرد الفعلي والتسوية - مصنع $factoryId"
        val bodyBuilder = StringBuilder()
        bodyBuilder.append("=== تقرير الجرد والتسوية المخزنية المعتمد ===\n")
        bodyBuilder.append("المصنع: $factoryId\n")
        bodyBuilder.append("نوع الجرد: $auditMode ${if (selectedRackId.isNotBlank()) "($selectedRackId)" else ""}\n")
        bodyBuilder.append("تاريخ الجرد: ${getCurrentFormattedTime()}\n")
        bodyBuilder.append("المشرف / القائم بالجرد: $auditorName\n")
        bodyBuilder.append("إجمالي الرصيد الدفتري (النظام): $totalExpected PCS\n")
        bodyBuilder.append("إجمالي الرصيد الفعلي (المعدود): $totalPhysical PCS\n")
        bodyBuilder.append("صافي الفارق (التباين): ${if (totalVariance >= 0) "+$totalVariance" else "$totalVariance"} PCS\n")
        if (notes.isNotBlank()) {
            bodyBuilder.append("ملاحظات الجرد: $notes\n")
        }
        bodyBuilder.append("--------------------------------------------------\n")
        bodyBuilder.append("تفاصيل الأصناف المجرودة:\n\n")

        auditRecords.forEachIndexed { index, record ->
            val sign = if (record.variance > 0) "+${record.variance}" else "${record.variance}"
            val statusArabic = when {
                record.variance == 0 -> "✓ مطابق تماماً"
                record.variance > 0 -> "▲ فائض مخزني"
                else -> "▼ عجز مخزني"
            }
            bodyBuilder.append("${index + 1}. كود: ${record.sku} | ${record.name}\n")
            bodyBuilder.append("   الموقع: ${record.rackId} | دفتري: ${record.systemCount} | فعلي: ${record.physicalCount} | الفارق: $sign ($statusArabic)\n\n")
        }

        bodyBuilder.append("--------------------------------------------------\n")
        bodyBuilder.append("سجل التدقيق محفوظ في قاعدة بيانات Firebase Firestore بصورة غير قابلة للتعديل.\n")

        dispatchEmail(context, recipientEmail, subject, bodyBuilder.toString())
    }

    /**
     * Dispatch an Inter-Factory Transfer notification to supervisor email
     */
    fun sendTransferDispatchEmail(
        context: Context,
        recipientEmail: String = DEFAULT_OPERATOR_EMAIL,
        sku: String,
        itemName: String,
        qty: Int,
        srcFactory: String,
        dstFactory: String,
        dstLocation: String,
        operatorName: String,
        notes: String
    ) {
        val subject = "🚚 [NEXUS WMS Transfer] إشعار شحن ونقل بين المصانع ($srcFactory ➔ $dstFactory)"
        val body = """
            === إشعار نقل أصناف بين المصانع ===
            كود الصنف: $sku
            اسم الصنف: $itemName
            الكمية المشحونة: $qty PCS
            من مصنع: $srcFactory
            إلى مصنع: $dstFactory
            الموقع المستهدف بالمستودع: $dstLocation
            المسؤول عن الإرسال: $operatorName
            تاريخ ووقت الإشعار: ${getCurrentFormattedTime()}
            ملاحظات النقل: ${notes.ifBlank { "شحنة مجدولة قياسية" }}
            
            --------------------------------------------------
            نظام إدارة المستودعات NEXUS WMS - تكامل Gmail المباشر
        """.trimIndent()

        dispatchEmail(context, recipientEmail, subject, body)
    }

    /**
     * Checks whether the official Gmail Android app is installed on this device/emulator
     */
    fun isGmailAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.gm", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks whether any mailto: client can handle outbound reports
     */
    fun hasEmailHandler(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Internal email launcher targeting Gmail / Email clients
     */
    private fun dispatchEmail(
        context: Context,
        recipientEmail: String,
        subject: String,
        body: String
    ) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                // Attempt to target Gmail package directly if available
                if (isGmailAppInstalled(context)) {
                    setPackage("com.google.android.gm")
                }
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general intent chooser if Gmail app is not default
                val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                val chooser = Intent.createChooser(fallbackIntent, "إرسال تقرير المستودع عبر البريد")
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "فشل بدء تطبيق Gmail / لا يوجد تطبيق بريد مثبت في المحاكي: ${e.localizedMessage ?: e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

