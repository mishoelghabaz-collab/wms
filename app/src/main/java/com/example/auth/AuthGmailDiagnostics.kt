package com.example.auth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuthDiagnosticVerdict {
    ACTIVE_GMAIL_READY,
    FIREBASE_EMAIL_ONLY,
    PREVIEW_BYPASS_UNAUTHENTICATED,
    AUTH_OR_ENV_FAILING
}

data class AuthDiagnosticReport(
    val verdict: AuthDiagnosticVerdict = AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED,
    val checkedAt: String = "--:--:--",
    val isRunningCheck: Boolean = false,
    val isEmulator: Boolean = false,
    val deviceSignature: String = "Unknown",
    val googlePlayServicesAvailable: Boolean = false,
    val googlePlayServicesStatusText: String = "Not checked",
    val isWebClientIdConfigured: Boolean = false,
    val webClientIdPreview: String = "Missing",
    val isFirebaseSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userUid: String? = null,
    val signInProviders: List<String> = emptyList(),
    val isGoogleProviderLinked: Boolean = false,
    val idTokenValid: Boolean = false,
    val idTokenStatus: String = "Not checked",
    val idTokenExpiresInMinutes: Long? = null,
    val gmailOAuthStatus: String = "Unverified",
    val isGmailAppInstalled: Boolean = false,
    val hasMailtoHandler: Boolean = false,
    val isPreviewBypassActive: Boolean = true,
    val summaryMessageEn: String = "Diagnostic check pending...",
    val summaryMessageAr: String = "جاري فحص حالة المصادقة والتوكن...",
    val errorDetails: String? = null
) {
    fun toClipboardSummary(): String = buildString {
        appendLine("=== NEXUS WMS AUTH & GMAIL DIAGNOSTIC REPORT ===")
        appendLine("Checked At: $checkedAt")
        appendLine("Verdict: ${verdict.name}")
        appendLine("Environment: ${if (isEmulator) "EMULATOR / PREVIEW" else "PHYSICAL DEVICE"} ($deviceSignature)")
        appendLine("Preview Auth Bypass Active: $isPreviewBypassActive")
        appendLine("Google Play Services: $googlePlayServicesStatusText (Available=$googlePlayServicesAvailable)")
        appendLine("OAuth Web Client ID Configured: $isWebClientIdConfigured ($webClientIdPreview)")
        appendLine("Firebase Signed In: $isFirebaseSignedIn")
        appendLine("User Email: ${userEmail ?: "None"}")
        appendLine("User UID: ${userUid ?: "None"}")
        appendLine("Providers: ${if (signInProviders.isEmpty()) "None" else signInProviders.joinToString(", ")}")
        appendLine("Google OAuth Provider Linked: $isGoogleProviderLinked")
        appendLine("Firebase ID Token: $idTokenStatus (Valid=$idTokenValid, ExpiresInMin=${idTokenExpiresInMinutes ?: "N/A"})")
        appendLine("Gmail OAuth / Scopes Status: $gmailOAuthStatus")
        appendLine("Gmail App Installed (com.google.android.gm): $isGmailAppInstalled")
        appendLine("Mailto Handler Present: $hasMailtoHandler")
        if (!errorDetails.isNullOrBlank()) {
            appendLine("Error Details: $errorDetails")
        }
        appendLine("Summary: $summaryMessageEn")
    }
}

class AuthGmailDiagnosticsManager(
    private val context: Context,
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
    private val _report = MutableStateFlow(AuthDiagnosticReport())
    val report: StateFlow<AuthDiagnosticReport> = _report.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun runFullDiagnostics(forceTokenRefresh: Boolean = true): AuthDiagnosticReport {
        _report.value = _report.value.copy(isRunningCheck = true)

        val computed = withContext(Dispatchers.IO) {
            val nowText = timeFormat.format(Date())
            val isEmulator = detectEmulator()
            val deviceSig = "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"

            val (gmsOk, gmsStatus) = checkGooglePlayServices()
            val (clientIdOk, clientIdPreview) = checkWebClientId()
            val gmailAppInstalled = isPackageInstalled("com.google.android.gm")
            val mailtoAvailable = hasMailtoIntentHandler()

            val currentUser = runCatching { auth?.currentUser }.getOrNull()
            val isSignedIn = currentUser != null
            val email = currentUser?.email
            val uid = currentUser?.uid
            val providers = currentUser?.providerData
                ?.mapNotNull { it.providerId }
                ?.filter { it != "firebase" }
                ?.distinct()
                ?: emptyList()
            val isGoogleLinked = providers.contains("google.com")

            var idTokenValid = false
            var idTokenStatus = "MISSING (Not Signed In)"
            var expiresInMinutes: Long? = null
            var tokenError: String? = null

            if (currentUser != null) {
                try {
                    val tokenResult = currentUser.getIdToken(forceTokenRefresh).await()
                    val tokenStr = tokenResult.token
                    if (!tokenStr.isNullOrBlank()) {
                        idTokenValid = true
                        val expMs = tokenResult.expirationTimestamp * 1000L
                        val diffMin = ((expMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L)
                        expiresInMinutes = diffMin
                        idTokenStatus = "VALID (JWT ${tokenStr.take(10)}... • exp ${diffMin}m)"
                    } else {
                        idTokenStatus = "INVALID (Empty Token Returned)"
                        tokenError = "Firebase returned an empty ID token."
                    }
                } catch (e: Exception) {
                    Log.w("AuthDiagnostics", "Token verification error: ${e.message}")
                    idTokenStatus = "FAILING (${e.javaClass.simpleName})"
                    tokenError = e.localizedMessage ?: e.message ?: "Failed to refresh Firebase ID token."
                }
            }

            val gmailOAuthStatus = when {
                !isSignedIn -> "INACTIVE — No authenticated session; Gmail intents use device fallback."
                isGoogleLinked && idTokenValid -> "ACTIVE (Google session verified; gmail.send / gmail.readonly ready via OAuth/Gmail client)"
                isGoogleLinked && !idTokenValid -> "FAILING — Google account linked but ID token refresh failed."
                idTokenValid -> "EMAIL-ONLY — Signed in with password; Google/Gmail OAuth token not granted (mailto fallback only)."
                else -> "UNVERIFIED — Auth session error."
            }

            val isPreviewBypass = !isSignedIn
            val verdict = when {
                tokenError != null -> AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING
                !isSignedIn -> AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED
                isGoogleLinked && idTokenValid -> AuthDiagnosticVerdict.ACTIVE_GMAIL_READY
                idTokenValid -> AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY
                else -> AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING
            }

            val (msgEn, msgAr) = when (verdict) {
                AuthDiagnosticVerdict.ACTIVE_GMAIL_READY -> {
                    val gmsSuffix = if (!gmsOk) " (Note: GMS reported $gmsStatus)" else ""
                    "Gmail & Google Auth ACTIVE with valid Firebase ID token$gmsSuffix." to
                        "المصادقة عبر Google و Gmail نشطة والتوكن صالح ومُتحقق منه."
                }
                AuthDiagnosticVerdict.FIREBASE_EMAIL_ONLY -> {
                    "Email/Password Firebase Auth ACTIVE (valid token), but Gmail OAuth is NOT linked. Sign in with Google for direct Gmail API readiness." to
                        "الدخول بالبريد مفعل والتوكن صالح، لكن حساب Google/Gmail غير مربوط حالياً (يعمل كإرسال احتياطي فقط)."
                }
                AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED -> {
                    val envLabel = if (isEmulator) "Emulator/Preview environment" else "Device"
                    "$envLabel is running WITHOUT an active login (Preview Bypass mode). No Firebase or Gmail tokens are present." to
                        "التطبيق يعمل حالياً في وضع المعاينة/المحاكاة بدون تسجيل دخول فعلي — لا توجد توكنات مصادقة أو Gmail نشطة."
                }
                AuthDiagnosticVerdict.AUTH_OR_ENV_FAILING -> {
                    "Authentication or token refresh is FAILING: ${tokenError ?: gmsStatus}" to
                        "فشل التحقق من المصادقة أو تحديث التوكن: ${tokenError ?: gmsStatus}"
                }
            }

            AuthDiagnosticReport(
                verdict = verdict,
                checkedAt = nowText,
                isRunningCheck = false,
                isEmulator = isEmulator,
                deviceSignature = deviceSig,
                googlePlayServicesAvailable = gmsOk,
                googlePlayServicesStatusText = gmsStatus,
                isWebClientIdConfigured = clientIdOk,
                webClientIdPreview = clientIdPreview,
                isFirebaseSignedIn = isSignedIn,
                userEmail = email,
                userUid = uid,
                signInProviders = providers,
                isGoogleProviderLinked = isGoogleLinked,
                idTokenValid = idTokenValid,
                idTokenStatus = idTokenStatus,
                idTokenExpiresInMinutes = expiresInMinutes,
                gmailOAuthStatus = gmailOAuthStatus,
                isGmailAppInstalled = gmailAppInstalled,
                hasMailtoHandler = mailtoAvailable,
                isPreviewBypassActive = isPreviewBypass,
                summaryMessageEn = msgEn,
                summaryMessageAr = msgAr,
                errorDetails = tokenError
            )
        }

        _report.value = computed
        return computed
    }

    private fun checkGooglePlayServices(): Pair<Boolean, String> {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val code = availability.isGooglePlayServicesAvailable(context)
            when (code) {
                ConnectionResult.SUCCESS -> true to "SUCCESS (Up to date)"
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> false to "UPDATE_REQUIRED"
                ConnectionResult.SERVICE_MISSING -> false to "SERVICE_MISSING"
                ConnectionResult.SERVICE_DISABLED -> false to "SERVICE_DISABLED"
                ConnectionResult.SERVICE_INVALID -> false to "SERVICE_INVALID"
                else -> false to "UNAVAILABLE (Code $code)"
            }
        } catch (e: Exception) {
            false to "UNAVAILABLE (${e.javaClass.simpleName})"
        }
    }

    private fun checkWebClientId(): Pair<Boolean, String> {
        return try {
            val cid = context.getString(R.string.default_web_client_id)
            if (cid.isNotBlank() && cid.contains(".apps.googleusercontent.com")) {
                true to "${cid.take(14)}...apps.googleusercontent.com"
            } else if (cid.isNotBlank()) {
                false to "NON_STANDARD ($cid)"
            } else {
                false to "EMPTY"
            }
        } catch (e: Exception) {
            false to "MISSING_RESOURCE"
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun hasMailtoIntentHandler(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            val handlers = context.packageManager.queryIntentActivities(intent, 0)
            handlers.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        fun detectEmulator(): Boolean {
            return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
        }
    }
}
