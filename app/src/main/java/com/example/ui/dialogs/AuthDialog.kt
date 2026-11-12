package com.example.ui.dialogs

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.auth.FirebaseAuthService
import com.example.data.WarehouseStrings
import com.example.model.ActiveOperator
import com.example.model.WarehouseOperatorProfile
import com.example.ui.components.TabularText
import com.example.ui.components.minTouchTarget
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun AuthDialog(
    authService: FirebaseAuthService,
    scope: CoroutineScope,
    isArabic: Boolean,
    currentFactoryId: String = "P7",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentUser by authService.currentUser.collectAsState()
    val operatorProfile by authService.operatorProfile.collectAsState()
    val isLoading by authService.isLoading.collectAsState()
    val authError by authService.authError.collectAsState()
    val activeOperators by authService.activeOperators.collectAsState()

    var authTab by remember { mutableStateOf(0) } // 0: Sign In, 1: Register New
    var emailInput by remember { mutableStateOf("mishoelghabaz@gmail.com") }
    var passwordInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }

    // Start observing concurrent operators
    LaunchedEffect(currentFactoryId) {
        authService.observeActiveOperators(currentFactoryId).collect { }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("auth_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Auth",
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = if (isArabic) "تسجيل الدخول وإدارة المشغلين المتزامنين" else "Operator Authentication & Concurrency",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Error Banner
                if (authError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CoralRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authError ?: "",
                            color = CoralRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (currentUser != null && operatorProfile != null) {
                    // Profile Info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "🟢 ONLINE & SYNCED",
                                        color = EmeraldGreenLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = operatorProfile?.terminalId ?: "",
                                    color = AmberOrange,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = operatorProfile?.displayName ?: "WMS Operator",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = operatorProfile?.email ?: "",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Concurrent Logged-in Operators in the same Factory
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkNavyCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "المشغلون المتصلون بالمصنع الآن:" else "Concurrent Operators Online:",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeonCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${activeOperators.size} متصل",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (activeOperators.isEmpty()) {
                                Text(
                                    text = if (isArabic) "أنت المشغل الوحيد المتصل حالياً بمصنع $currentFactoryId" else "You are the only operator currently connected to $currentFactoryId",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    activeOperators.forEach { op ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (op.isOnline) EmeraldGreen else TextMuted)
                                                )
                                                Text(
                                                    text = op.displayName.ifBlank { op.email.substringBefore("@") },
                                                    color = TextWhite,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = op.terminalId,
                                                color = AmberOrange,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sign Out Button
                    Button(
                        onClick = {
                            authService.signOut(scope, currentFactoryId) {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().minTouchTarget().testTag("sign_out_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(if (isArabic) "تسجيل الخروج من النظام" else "Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Sign In / Register Tab Selector
                    TabRow(
                        selectedTabIndex = authTab,
                        containerColor = Color.Transparent,
                        contentColor = NeonCyan
                    ) {
                        Tab(
                            selected = authTab == 0,
                            onClick = { authTab = 0 },
                            text = { Text(if (isArabic) "تسجيل الدخول" else "Sign In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = authTab == 1,
                            onClick = { authTab = 1 },
                            text = { Text(if (isArabic) "حساب جديد" else "Register", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (authTab == 1) {
                        // Display Name
                        OutlinedTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it },
                            label = { Text(if (isArabic) "اسم المشغل / الفني" else "Operator Name", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(if (isArabic) "كلمة المرور" else "Password", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Submit Email Button
                    Button(
                        onClick = {
                            if (authTab == 0) {
                                authService.signInWithEmail(
                                    email = emailInput,
                                    pass = passwordInput,
                                    factoryId = currentFactoryId,
                                    scope = scope,
                                    onSuccess = { onDismiss() }
                                )
                            } else {
                                authService.registerWithEmail(
                                    email = emailInput,
                                    pass = passwordInput,
                                    displayName = displayNameInput,
                                    factoryId = currentFactoryId,
                                    scope = scope,
                                    onSuccess = { onDismiss() }
                                )
                            }
                        },
                        enabled = !isLoading && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("email_auth_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkNavyBase,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (authTab == 0) {
                                    if (isArabic) "دخول بالبريد الإلكتروني" else "Log In with Email"
                                } else {
                                    if (isArabic) "إنشاء وتفعيل حساب المشغل" else "Create Operator Account"
                                },
                                color = DarkNavyBase,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = TextMuted.copy(alpha = 0.3f))
                        Text(text = if (isArabic) "أو" else "OR", color = TextMuted, fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = TextMuted.copy(alpha = 0.3f))
                    }

                    // Sign In With Google Button
                    OutlinedButton(
                        onClick = {
                            activity?.let { act ->
                                authService.signInWithGoogle(
                                    activity = act,
                                    scope = scope,
                                    factoryId = currentFactoryId,
                                    onSuccess = { onDismiss() }
                                )
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("sign_in_google_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isArabic) "تسجيل الدخول عبر Google" else "Sign In with Google",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WarehouseStrings.close(isArabic), color = TextMuted)
                }
            }
        }
    }
}
