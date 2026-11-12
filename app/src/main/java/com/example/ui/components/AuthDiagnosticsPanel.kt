package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthDiagnosticReport
import com.example.auth.AuthDiagnosticVerdict
import com.example.ui.theme.*

private fun verdictColor(verdict: AuthDiagnosticVerdict): Color = when (verdict) {
    AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> EmeraldGreen
    AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> NeonCyanMuted
    AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> AmberOrange
    AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> CoralRed
}

private fun verdictShortLabel(verdict: AuthDiagnosticVerdict, isArabic: Boolean): String = when (verdict) {
    AuthDiagnosticVerdict.ACTIVE_GMAIL_READY ->
        if (isArabic) "مصادقة Gmail والتوكن نشطة" else "Gmail Auth Active • Valid Token"
    AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY ->
        if (isArabic) "بريد Firebase فقط (بدون Google OAuth)" else "Email Auth Valid • No Gmail OAuth"
    AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED ->
        if (isArabic) "وضع المعاينة/المحاكي (بدون تسجيل دخول)" else "Preview/Emulator Bypass • No Token"
    AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING ->
        if (isArabic) "خطأ في التوكن أو بيئة المصادقة" else "Auth / Token Verification Failing"
}

/**
 * Compact inline status strip shown on DashboardScreen for immediate auth/Gmail visibility
 */
@Composable
fun CompactAuthStatusStrip(
    report: AuthDiagnosticReport,
    isArabic: Boolean,
    onRunCheck: () -> Unit,
    onOpenAuthOrDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = verdictColor(report.verdict)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenAuthOrDiagnostics() }
            .testTag("compact_auth_status_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (report.verdict) {
                            AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> Icons.Default.VerifiedUser
                            AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> Icons.Default.MarkEmailRead
                            AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> Icons.Default.WarningAmber
                            AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = verdictShortLabel(report.verdict, isArabic),
                            color = accent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (report.isEmulator) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "EMU",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (report.isFirebaseSignedIn) {
                            "${report.userEmail ?: "Signed in"} • ${report.idTokenStatus}"
                        } else {
                            if (isArabic) "اضغط لفتح لوحة فحص Auth و Gmail" else "Tap to inspect Gmail & Firebase token status"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRunCheck,
                    enabled = !report.isRunningCheck,
                    modifier = Modifier
                        .minTouchTarget(40.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .testTag("strip_verify_token_btn")
                ) {
                    if (report.isRunningCheck) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            color = accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Verify Auth Token",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Auth Status & Gmail Diagnostics Panel
 */
@Composable
fun AuthDiagnosticsPanel(
    report: AuthDiagnosticReport,
    isArabic: Boolean,
    onRunDiagnostics: () -> Unit,
    onCopyReport: (String) -> Unit,
    onOpenLogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = verdictColor(report.verdict)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .testTag("auth_diagnostics_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isArabic) "لوحة فحص مصادقة Gmail و Firebase" else "Auth Status & Gmail Diagnostics",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "آخر فحص: ${report.checkedAt}" else "Last checked: ${report.checkedAt}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = when (report.verdict) {
                            AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> "ACTIVE"
                            AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> "EMAIL ONLY"
                            AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> "BYPASSED"
                            AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> "FAILING"
                        },
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Explicit Summary Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isArabic) report.summaryMessageAr else report.summaryMessageEn,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(9.dp)
                )
            }

            // Structured Telemetry Rows
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    DiagnosticKeyValueRow(
                        label = if (isArabic) "حالة Gmail OAuth" else "Gmail Auth State",
                        value = report.gmailOAuthStatus,
                        valueColor = if (report.isGoogleProviderLinked && report.idTokenValid) EmeraldGreen else AmberOrange
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    DiagnosticKeyValueRow(
                        label = if (isArabic) "توكن Firebase ID" else "Firebase ID Token",
                        value = report.idTokenStatus,
                        valueColor = if (report.idTokenValid) EmeraldGreen else CoralRed
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    DiagnosticKeyValueRow(
                        label = if (isArabic) "المستخدم الحالي" else "Active User",
                        value = if (report.isFirebaseSignedIn) {
                            "${report.userEmail ?: "Signed In"} (${report.signInProviders.joinToString("+").ifBlank { "firebase" }})"
                        } else {
                            if (isArabic) "غير مسجل (تجاوز وضع المعاينة)" else "None (Preview Bypass Active)"
                        },
                        valueColor = if (report.isFirebaseSignedIn) MaterialTheme.colorScheme.onSurface else AmberOrange
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    DiagnosticKeyValueRow(
                        label = if (isArabic) "البيئة / المحاكي" else "Runtime Environment",
                        value = "${if (report.isEmulator) "Emulator / Preview" else "Physical Device"} • ${report.deviceSignature}",
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    DiagnosticKeyValueRow(
                        label = "Google Play Services",
                        value = report.googlePlayServicesStatusText,
                        valueColor = if (report.googlePlayServicesAvailable) EmeraldGreen else CoralRed
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    DiagnosticKeyValueRow(
                        label = if (isArabic) "تطبيق Gmail & OAuth ID" else "Gmail App & Web Client ID",
                        value = "Gmail App: ${if (report.isGmailAppInstalled) "Installed ✓" else "Missing ✕"} • Client ID: ${if (report.isWebClientIdConfigured) "OK ✓" else "Missing ✕"}",
                        valueColor = if (report.isWebClientIdConfigured) MaterialTheme.colorScheme.onSurface else CoralRed
                    )

                    if (!report.errorDetails.isNullOrBlank()) {
                        HorizontalDivider(color = CoralRed.copy(alpha = 0.4f))
                        DiagnosticKeyValueRow(
                            label = if (isArabic) "تفاصيل الخطأ" else "Error Detail",
                            value = report.errorDetails,
                            valueColor = CoralRed
                        )
                    }
                }
            }

            // Action Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onRunDiagnostics,
                    enabled = !report.isRunningCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .minTouchTarget(44.dp)
                        .testTag("run_auth_diagnostics_btn")
                ) {
                    if (report.isRunningCheck) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = DarkNavyBase,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = DarkNavyBase,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "فحص التوكن الآن" else "Verify Tokens",
                        color = DarkNavyBase,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onOpenLogin != null && (!report.isFirebaseSignedIn || !report.isGoogleProviderLinked)) {
                    OutlinedButton(
                        onClick = onOpenLogin,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minTouchTarget(44.dp)
                            .testTag("diag_open_login_btn")
                    ) {
                        Text(
                            text = if (isArabic) "ربط Google" else "Sign In",
                            color = NeonCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { onCopyReport(report.toClipboardSummary()) },
                    modifier = Modifier
                        .minTouchTarget(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("copy_diagnostics_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = if (isArabic) "نسخ التقرير" else "Copy Diagnostic Report",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticKeyValueRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}
