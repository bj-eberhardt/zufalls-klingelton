package de.emailspeicher.zufallsklingelton

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionAndPickerE2eTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val robot by lazy { SystemUiRobot() }
    private var fixtureUri: Uri? = null

    @Before
    fun resetAppState() {
        robot.resetApp()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @After
    fun cleanupFixture() {
        fixtureUri?.let { context.contentResolver.delete(it, null, null) }
    }

    @Test
    fun realPermissionFlow_acceptsRuntimeDialogsAndSettingsScreen() {
        requestPermissions()
        robot.acceptRuntimePermissionDialog()
        robot.acceptRuntimePermissionDialog()
        robot.enableWriteSettings()

        composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun runtimePermission_denialKeepsSetupVisible() {
        requestPermissions()
        robot.denyRuntimePermissionDialog()
        composeRule.onNodeWithTag("request_permissions_button").assertIsDisplayed()
    }

    @Test
    fun writeSettings_backWithoutGrantKeepsSetupVisible() {
        requestPermissions()
        robot.acceptRuntimePermissionDialog()
        robot.acceptRuntimePermissionDialog()
        robot.waitForPackage("com.android.settings")
        robot.shell("input keyevent KEYCODE_BACK")
        composeRule.onNodeWithTag("request_permissions_button").assertIsDisplayed()
    }

    @Test
    fun realFilePicker_importsDeterministicWavFromDownloads() {
        completePermissionFlow()
        fixtureUri = createDownloadFixture("zufalls-klingelton-e2e.wav")

        openStoragePicker()
        robot.clickDocumentsFile("zufalls-klingelton-e2e.wav")

        composeRule.onNodeWithText("zufalls-klingelton-e2e.wav").assertIsDisplayed()
    }

    @Test
    fun filePicker_cancelReturnsToHome() {
        completePermissionFlow()
        openStoragePicker()
        robot.waitForPackage("com.google.android.documentsui")
        robot.shell("input keyevent KEYCODE_BACK")
        composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
    }

    private fun completePermissionFlow() {
        requestPermissions()
        robot.acceptRuntimePermissionDialog()
        robot.acceptRuntimePermissionDialog()
        robot.enableWriteSettings()
        composeRule.onNodeWithText(context.getString(R.string.home_title)).assertIsDisplayed()
    }

    private fun requestPermissions() {
        composeRule.onNodeWithTag("request_permissions_button").performClick()
        composeRule.waitForIdle()
    }

    private fun openStoragePicker() {
        composeRule.onNodeWithTag("add_track_button").performClick()
        composeRule.onNodeWithTag("add_from_storage_button").performClick()
        composeRule.waitForIdle()
    }

    private fun createDownloadFixture(displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "audio/wav")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = requireNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
        )
        context.contentResolver.openOutputStream(uri).use { output ->
            requireNotNull(output).write(WavFixture.bytes())
        }
        context.contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null,
        )
        return uri
    }
}
