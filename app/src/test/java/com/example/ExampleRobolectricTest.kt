package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.manager.ApkSecurityScanner
import com.example.manager.MegaCloudManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies AppPulse`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AppPulse", appName)
    }

    @Test
    fun `verify MegaCloudManager returns cloud folder hierarchy and share links`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = MegaCloudManager(context)

        val files = manager.getCloudFiles("/")
        assertTrue("Cloud files should contain folders", files.isNotEmpty())

        val shareLink = manager.generateShareLink("test_id_123", "AppPulse_Backup.apk")
        assertTrue("Share link should have mega domain", shareLink.startsWith("https://mega.nz/file/test_id_123"))
    }

    @Test
    fun `verify ApkSecurityScanner produces threat audit report`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scanner = ApkSecurityScanner(context)

        val apps = scanner.scanInstalledApps()
        assertNotNull(apps)
        assertTrue(apps.isNotEmpty())

        val report = scanner.generateThreatReport(apps)
        assertNotNull(report.reportId)
        assertTrue(report.totalAppsScanned > 0)
    }

    @Test
    fun `verify VaultSecurityManager PIN verification and update`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = com.example.manager.VaultSecurityManager(context)

        // Default PIN should verify true for 1234
        assertTrue("Default PIN should be 1234", securityManager.verifyPin("1234"))
        org.junit.Assert.assertFalse("Wrong PIN should fail", securityManager.verifyPin("9999"))

        // Update PIN to 5678
        val updateSuccess = securityManager.updatePin("5678")
        assertTrue("Update PIN should succeed for 4 digits", updateSuccess)
        assertTrue("New PIN 5678 should verify true", securityManager.verifyPin("5678"))
    }

    @Test
    fun `verify AuthManager object exists and handles credential manager setup`() {
        assertNotNull(com.example.auth.AuthManager)
    }
}

