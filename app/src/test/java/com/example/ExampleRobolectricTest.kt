package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.auth.AuthDiagnosticVerdict
import com.example.auth.AuthGmailDiagnosticsManager
import com.example.gmail.GmailIntegrationService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app configuration and diagnostics detect unauthenticated preview bypass`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = AuthGmailDiagnosticsManager(context = context, auth = null)

        val report = manager.runFullDiagnostics(forceTokenRefresh = false)

        assertEquals(AuthDiagnosticVerdict.PREVIEW_BYPASS_UNAUTHENTICATED, report.verdict)
        assertFalse(report.isFirebaseSignedIn)
        assertFalse(report.idTokenValid)
        assertTrue(report.isPreviewBypassActive)
        assertTrue(report.idTokenStatus.contains("MISSING"))
        assertFalse(GmailIntegrationService.isGmailAppInstalled(context))
    }
}
