package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Domain representation of an authenticated user
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null
)

fun FirebaseUser.toDomainUser(): User = User(
    uid = uid,
    email = email,
    displayName = displayName?.ifBlank { null } ?: email?.substringBefore("@"),
    photoUrl = photoUrl?.toString()
)

/**
 * Repository encapsulating Firebase Auth and Google Sign-In with Credential Manager API.
 */
class AuthenticationRepository(
    private val context: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<User?>(auth.currentUser?.toDomainUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _rawFirebaseUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val rawFirebaseUser: StateFlow<FirebaseUser?> = _rawFirebaseUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            _rawFirebaseUser.value = firebaseUser
            _currentUser.value = firebaseUser?.toDomainUser()
        }
    }

    /**
     * Signs in using Google via Android Credential Manager API
     */
    suspend fun signInWithGoogle(activity: Activity): Result<User> = withContext(Dispatchers.IO) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val msg = "Missing default_web_client_id resource."
            _authError.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }

        _isLoading.value = true
        _authError.value = null

        try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user ?: throw IllegalStateException("FirebaseUser is null after Google Sign-In")
                val domainUser = firebaseUser.toDomainUser()

                _rawFirebaseUser.value = firebaseUser
                _currentUser.value = domainUser
                _isLoading.value = false
                Result.success(domainUser)
            } else {
                val msg = "Unsupported credential response type: ${credential::class.java.name}"
                _authError.value = msg
                _isLoading.value = false
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("AuthRepo", "User cancelled Google Sign-In: ${e.message}")
            _isLoading.value = false
            Result.failure(e)
        } catch (e: CancellationException) {
            _isLoading.value = false
            throw e
        } catch (e: Exception) {
            Log.e("AuthRepo", "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Google Sign-In failed"
            _authError.value = msg
            _isLoading.value = false
            Result.failure(e)
        }
    }

    /**
     * Sign out current user and clear Credential Manager state
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("AuthRepo", "Error clearing credential state: ${e.message}")
            } finally {
                auth.signOut()
                _rawFirebaseUser.value = null
                _currentUser.value = null
            }
            Result.success(Unit)
        }
    }

    fun clearError() {
        _authError.value = null
    }
}
