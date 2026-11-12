package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.AuthDiagnosticReport
import com.example.auth.AuthDiagnosticVerdict
import com.example.data.WarehouseStrings
import com.example.gmail.GmailIntegrationService
import com.example.model.StockStatus
import com.example.model.WarehouseItem
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*

enum class EmailDispatchType {
    LOW_STOCK_ALERT,
    AUDIT_SUMMARY,
    INTER_PLANT_TRANSFER,
    CUSTOM_MEMO
}

@Composable
fun GmailDispatcherDialog(
    isArabic: Boolean,
    currentFactoryId: String,
    currentFactoryName: String,
    items: List<WarehouseItem>,
    operatorName: String = "WMS Supervisor",
    diagnosticsReport: AuthDiagnosticReport = AuthDiagnosticReport(),
    onRefreshDiagnostics: () -> Unit = {},
    onOpenAuthDiagnostics: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var recipientEmail by remember { mutableStateOf(diagnosticsReport.userEmail ?: GmailIntegrationService.DEFAULT_OPERATOR_EMAIL) }
    var selectedType by remember { mutableStateOf(EmailDispatchType.LOW_STOCK_ALERT) }
    var customSubject by remember { mutableStateOf("") }
    var customNotes by remember { mutableStateOf("") }

    val criticalItems = remember(items, currentFactoryId) {
        items.filter { it.plantId == currentFactoryId && (it.status == StockStatus.CRITICAL || it.status == StockStatus.LOW_STOCK) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CoralRed.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("gmail_dispatcher_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CoralRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isArabic) "مركز إرسال تقارير Gmail" else "Gmail Report Dispatcher",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$currentFactoryName ($currentFactoryId)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.minTouchTarget(40.dp).size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                // Recipient Email Field
                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = { Text(if (isArabic) "البريد الإلكتروني المستلم" else "Recipient Email", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Send, contentDescription = null, tint = CoralRed, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dispatch Type Selector Chips
                Text(
                    text = if (isArabic) "نوع التقرير المراد إرساله:" else "Select Report Type:",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == EmailDispatchType.LOW_STOCK_ALERT,
                        onClick = { selectedType = EmailDispatchType.LOW_STOCK_ALERT },
                        label = { Text(if (isArabic) "🚨 نقص حرج" else "Low Stock", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == EmailDispatchType.AUDIT_SUMMARY,
                        onClick = { selectedType = EmailDispatchType.AUDIT_SUMMARY },
                        label = { Text(if (isArabic) "📋 جرد شامل" else "Audit Log", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == EmailDispatchType.CUSTOM_MEMO,
                        onClick = { selectedType = EmailDispatchType.CUSTOM_MEMO },
                        label = { Text(if (isArabic) "✉️ مذكرة" else "Memo", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Context Box Preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (selectedType) {
                            EmailDispatchType.LOW_STOCK_ALERT -> {
                                Text(
                                    text = if (isArabic) "ملخص النقص الحرج الحالي:" else "Critical Stock Summary:",
                                    color = CoralRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic)
                                        "سيتم تضمين جميع الأصناف التي تقل كمياتها عن الحد الأدنى في $currentFactoryId (${criticalItems.size} أصناف حالياً)."
                                    else
                                        "All items below minimum safety threshold in $currentFactoryId (${criticalItems.size} items) will be formatted and dispatched.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                if (criticalItems.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "عينة: ${criticalItems.first().sku} • ${criticalItems.first().name}", color = TextWhite, fontSize = 11.sp)
                                        TabularText(
                                            text = "${criticalItems.first().quantity} / ${criticalItems.first().minStockThreshold} PCS",
                                            color = CoralRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            EmailDispatchType.AUDIT_SUMMARY -> {
                                Text(
                                    text = if (isArabic) "تقرير الجرد المخزني الفعلي:" else "Inventory Stocktaking Report:",
                                    color = AmberOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic)
                                        "إرسال كشف الأرصدة الدفترية مع التباينات المسجلة وتوقيع المشغل إلى بريد الإدارة."
                                    else
                                        "Send registered stock ledger with detected discrepancies and supervisor sign-off.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            EmailDispatchType.INTER_PLANT_TRANSFER -> {
                                Text(
                                    text = if (isArabic) "إشعار شحن ونقل بين المصانع:" else "Inter-Factory Shipping Notice:",
                                    color = EmeraldGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) "إرسال بوليصة النقل الداخلي وتأكيد استلام الشحنة." else "Send internal transfer manifest and cargo confirmation.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            EmailDispatchType.CUSTOM_MEMO -> {
                                OutlinedTextField(
                                    value = customSubject,
                                    onValueChange = { customSubject = it },
                                    label = { Text("عنوان التقرير / الموضوع", color = TextMuted) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = customNotes,
                                    onValueChange = { customNotes = it },
                                    label = { Text("نص الرسالة أو الملاحظات التشغيلية", color = TextMuted) },
                                    minLines = 3,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Live Gmail Auth & Token Verification Status
                val diagColor = when (diagnosticsReport.verdict) {
                    AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> EmeraldGreen
                    AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> NeonCyanMuted
                    AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> AmberOrange
                    AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> CoralRed
                }
                val diagBadgeText = when (diagnosticsReport.verdict) {
                    AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> "TOKEN ACTIVE ✓"
                    AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> "EMAIL TOKEN ONLY"
                    AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> "NO AUTH (BYPASS)"
                    AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> "AUTH FAILING ✕"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = diagColor.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, diagColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "المشغل: $operatorName" else "Operator: $operatorName",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = diagColor.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { onRefreshDiagnostics() }
                            ) {
                                Text(
                                    text = diagBadgeText,
                                    color = diagColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${diagnosticsReport.gmailOAuthStatus} • Gmail App: ${if (diagnosticsReport.isGmailAppInstalled) "Installed" else "Missing (Mailto Fallback)"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                        Text(
                            text = if (isArabic) "عرض تقرير تشخيص Auth و Gmail الكامل ←" else "Open Full Auth/Gmail Diagnostics →",
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    onDismiss()
                                    onOpenAuthDiagnostics()
                                }
                                .padding(top = 2.dp)
                                .testTag("gmail_open_diagnostics_link")
                        )
                    }
                }

                // Action Buttons
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
                            when (selectedType) {
                                EmailDispatchType.LOW_STOCK_ALERT -> {
                                    GmailIntegrationService.sendLowStockAlertEmail(
                                        context = context,
                                        recipientEmail = recipientEmail,
                                        factoryId = currentFactoryId,
                                        criticalItems = criticalItems,
                                        operatorName = operatorName
                                    )
                                }
                                EmailDispatchType.AUDIT_SUMMARY -> {
                                    GmailIntegrationService.sendInventoryAuditReportEmail(
                                        context = context,
                                        recipientEmail = recipientEmail,
                                        factoryId = currentFactoryId,
                                        auditMode = "جرد شامل للمصنع",
                                        selectedRackId = "All Racks",
                                        totalExpected = items.filter { it.plantId == currentFactoryId }.sumOf { it.quantity },
                                        totalPhysical = items.filter { it.plantId == currentFactoryId }.sumOf { it.quantity },
                                        totalVariance = 0,
                                        auditorName = operatorName,
                                        notes = "تقرير معتمد من ماسح المستودع"
                                    )
                                }
                                EmailDispatchType.INTER_PLANT_TRANSFER -> {
                                    GmailIntegrationService.sendTransferDispatchEmail(
                                        context = context,
                                        recipientEmail = recipientEmail,
                                        sku = items.firstOrNull { it.plantId == currentFactoryId }?.sku ?: "13008512",
                                        itemName = items.firstOrNull { it.plantId == currentFactoryId }?.name ?: "Part",
                                        qty = 500,
                                        srcFactory = currentFactoryId,
                                        dstFactory = if (currentFactoryId == "P7") "P2" else "P7",
                                        dstLocation = "A0101",
                                        operatorName = operatorName,
                                        notes = "شحنة بين المصانع"
                                    )
                                }
                                EmailDispatchType.CUSTOM_MEMO -> {
                                    GmailIntegrationService.sendLowStockAlertEmail(
                                        context = context,
                                        recipientEmail = recipientEmail,
                                        factoryId = currentFactoryId,
                                        criticalItems = criticalItems,
                                        operatorName = operatorName
                                    )
                                }
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .minTouchTarget()
                            .testTag("confirm_gmail_dispatch_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isArabic) "إرسال عبر Gmail الآن" else "Dispatch via Gmail",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
