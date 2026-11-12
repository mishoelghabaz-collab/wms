package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.ActiveOperator
import com.example.model.WarehouseOperatorProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext



class FirebaseAuthService(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _operatorProfile = MutableStateFlow<WarehouseOperatorProfile?>(
        auth.currentUser?.toOperatorProfile()
    )
    val operatorProfile: StateFlow<WarehouseOperatorProfile?> = _operatorProfile.asStateFlow()

    private val _activeOperators = MutableStateFlow<List<ActiveOperator>>(emptyList())
    val activeOperators: StateFlow<List<ActiveOperator>> = _activeOperators.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _operatorProfile.value = user?.toOperatorProfile()
            if (user != null) {
                syncOperatorPresence(user, "P7", true)
            }
        }
    }

    private fun FirebaseUser.toOperatorProfile(): WarehouseOperatorProfile {
        val safeTerminalId = "Terminal #${uid.takeLast(4).uppercase()}"
        return WarehouseOperatorProfile(
            uid = uid,
            displayName = displayName?.ifBlank { null } ?: email?.substringBefore("@") ?: "WMS Operator",
            email = email ?: "operator@nexus-wms.internal",
            photoUrl = photoUrl?.toString(),
            terminalId = safeTerminalId,
            role = "WMS Warehouse Specialist"
        )
    }

    private var failedAttempts = 0
    private var lockoutTimestamp = 0L

    private fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        if (now < lockoutTimestamp) {
            val remainingSec = ((lockoutTimestamp - now) / 1000).coerceAtLeast(1)
            val msg = "⚠️ تم حظر المحاولات مؤقتاً لتكرار الفشل. يرجى الانتظار $remainingSec ثانية."
            _authError.value = msg
            return true
        }
        return false
    }

    fun clearError() {
        _authError.value = null
    }

    /**
     * Send secure expiring password reset link via Firebase Auth
     */
    fun sendPasswordReset(
        email: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (email.isBlank()) {
            val msg = "الرجاء إدخال البريد الإلكتروني لإرسال رابط إعادة التعيين"
            _authError.value = msg
            onError(msg)
            return
        }

        _isLoading.value = true
        scope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                val msg = e.localizedMessage ?: "فشل إرسال رابط إعادة تعيين كلمة المرور"
                _authError.value = msg
                onError(msg)
            }
        }
    }

    /**
     * Sign In with Email & Password (Protected by Rate Limiting)
     */
    fun signInWithEmail(
        email: String,
        pass: String,
        factoryId: String = "P7",
        scope: CoroutineScope,
        onSuccess: (WarehouseOperatorProfile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isRateLimited()) {
            onError(_authError.value ?: "Rate limited")
            return
        }

        if (email.isBlank() || pass.isBlank()) {
            val msg = "الرجاء إدخال البريد الإلكتروني وكلمة المرور"
            _authError.value = msg
            onError(msg)
            return
        }

        _isLoading.value = true
        _authError.value = null

        scope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = result.user
                val profile = user?.toOperatorProfile() ?: WarehouseOperatorProfile(
                    uid = "usr-${System.currentTimeMillis() % 10000}",
                    displayName = email.substringBefore("@"),
                    email = email
                )
                failedAttempts = 0
                lockoutTimestamp = 0L
                _currentUser.value = user
                _operatorProfile.value = profile
                user?.let { syncOperatorPresence(it, factoryId, true) }
                _isLoading.value = false
                onSuccess(profile)
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Email sign-in failed", e)
                failedAttempts++
                if (failedAttempts >= 5) {
                    lockoutTimestamp = System.currentTimeMillis() + 30_000L // 30s lockout
                }
                val msg = when {
                    failedAttempts >= 5 ->
                        "⚠️ تم إدخال بيانات خاطئة 5 مرات متتالية. تم قفل المحاولات لمدة 30 ثانية."
                    e.message?.contains("user-not-found", ignoreCase = true) == true ->
                        "المستخدم غير مسجل، يمكنك إنشاء حساب جديد"
                    e.message?.contains("wrong-password", ignoreCase = true) == true ||
                    e.message?.contains("invalid-credential", ignoreCase = true) == true ->
                        "كلمة المرور أو البريد الإلكتروني غير صحيح (محاولة $failedAttempts من 5)"
                    e.message?.contains("invalid-email", ignoreCase = true) == true ->
                        "صيغة البريد الإلكتروني غير صالحة"
                    else -> e.localizedMessage ?: "حدث خطأ أثناء تسجيل الدخول بالبريد"
                }
                _authError.value = msg
                _isLoading.value = false
                onError(msg)
            }
        }
    }

    /**
     * Register new Operator with Email & Password
     */
    fun registerWithEmail(
        email: String,
        pass: String,
        displayName: String,
        factoryId: String = "P7",
        scope: CoroutineScope,
        onSuccess: (WarehouseOperatorProfile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (email.isBlank() || pass.length < 6) {
            val msg = "كلمة المرور يجب أن لا تقل عن 6 أحرف"
            _authError.value = msg
            onError(msg)
            return
        }

        _isLoading.value = true
        _authError.value = null

        scope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val user = result.user
                if (user != null && displayName.isNotBlank()) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.trim())
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (pe: Exception) {
                        Log.w("FirebaseAuthService", "Could not set display name: ${pe.message}")
                    }
                }
                val profile = user?.toOperatorProfile() ?: WarehouseOperatorProfile(
                    uid = user?.uid ?: "usr-new",
                    displayName = displayName.ifBlank { email.substringBefore("@") },
                    email = email
                )
                _currentUser.value = user
                _operatorProfile.value = profile
                user?.let { syncOperatorPresence(it, factoryId, true) }
                _isLoading.value = false
                onSuccess(profile)
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Email registration failed", e)
                val msg = when {
                    e.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                        "هذا البريد مسجل مسبقاً، يمكنك تسجيل الدخول مباشرة"
                    else -> e.localizedMessage ?: "فشل تسجيل المشغل الجديد"
                }
                _authError.value = msg
                _isLoading.value = false
                onError(msg)
            }
        }
    }

    /**
     * Multi-User Presence Synchronization in Firestore
     */
    fun syncOperatorPresence(user: FirebaseUser, factoryId: String, isOnline: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val terminalId = "Terminal #${user.uid.takeLast(4).uppercase()}"
                val operatorData = hashMapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: "operator@nexus-wms.internal"),
                    "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "WMS Operator"),
                    "role" to "WMS Specialist",
                    "terminalId" to terminalId,
                    "factoryId" to factoryId,
                    "lastSeenTimestamp" to System.currentTimeMillis(),
                    "isOnline" to isOnline
                )

                // Write to factory-scoped active operators
                firestore.collection("factories")
                    .document(factoryId)
                    .collection("operators")
                    .document(user.uid)
                    .set(operatorData, SetOptions.merge())

                // Also update top-level users
                firestore.collection("users")
                    .document(user.uid)
                    .set(operatorData, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("FirebaseAuthService", "Could not update operator presence: ${e.message}")
            }
        }
    }

    /**
     * Observe Active Operators concurrently logged into the factory
     */
    fun observeActiveOperators(factoryId: String): Flow<List<ActiveOperator>> = callbackFlow {
        val listener = firestore.collection("factories")
            .document(factoryId)
            .collection("operators")
            .whereEqualTo("isOnline", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseAuthService", "Operators listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val email = doc.getString("email") ?: ""
                    val displayName = doc.getString("displayName") ?: email.substringBefore("@")
                    val role = doc.getString("role") ?: "WMS Specialist"
                    val terminalId = doc.getString("terminalId") ?: "Terminal"
                    val fact = doc.getString("factoryId") ?: factoryId
                    val lastSeen = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                    val online = doc.getBoolean("isOnline") ?: true
                    ActiveOperator(
                        uid = uid,
                        email = email,
                        displayName = displayName,
                        role = role,
                        terminalId = terminalId,
                        factoryId = fact,
                        lastSeenTimestamp = lastSeen,
                        isOnline = online
                    )
                } ?: emptyList()
                _activeOperators.value = list
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Interactive Google Sign-In using Jetpack CredentialManager
     */
    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        factoryId: String = "P7",
        onSuccess: (WarehouseOperatorProfile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val msg = "Missing default_web_client_id resource."
            _authError.value = msg
            onError(msg)
            return
        }

        _isLoading.value = true
        _authError.value = null

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    val profile = user?.toOperatorProfile() ?: WarehouseOperatorProfile(
                        uid = "usr-unknown",
                        displayName = "WMS Operator",
                        email = "operator@wms.com"
                    )
                    _operatorProfile.value = profile
                    user?.let { syncOperatorPresence(it, factoryId, true) }
                    _isLoading.value = false
                    onSuccess(profile)
                } else {
                    val msg = "Unsupported credential response type"
                    _authError.value = msg
                    _isLoading.value = false
                    onError(msg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("FirebaseAuthService", "Google Sign-In cancelled: ${e.message}", e)
                _isLoading.value = false
            } catch (e: CancellationException) {
                _isLoading.value = false
                throw e
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Authentication failed", e)
                val msg = e.localizedMessage ?: "Sign-in error occurred"
                _authError.value = msg
                _isLoading.value = false
                onError(msg)
            }
        }
    }

    /**
     * Sign out and clear credential state
     */
    fun signOut(scope: CoroutineScope, factoryId: String = "P7", onComplete: () -> Unit = {}) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    _currentUser.value?.let { syncOperatorPresence(it, factoryId, false) }
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("FirebaseAuthService", "Could not clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    _currentUser.value = null
                    _operatorProfile.value = null
                    onComplete()
                }
            }
        }
    }
}
