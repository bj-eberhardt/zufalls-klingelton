package de.emailspeicher.zufallsklingelton

import android.app.Instrumentation
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

class SystemUiRobot(
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
) {
    private val device = UiDevice.getInstance(instrumentation)
    private val targetContext: Context = instrumentation.targetContext

    fun resetApp() {
        listOf(
            audioPermission(),
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).forEach { permission ->
            shell("pm revoke ${targetContext.packageName} $permission")
        }
        targetContext.getSharedPreferences("track_pool", Context.MODE_PRIVATE).edit().clear().commit()
        targetContext.getSharedPreferences("playback_state", Context.MODE_PRIVATE).edit().clear().commit()
        targetContext.getSharedPreferences("platform_acceptance", Context.MODE_PRIVATE).edit().clear().commit()
        shell("appops set ${targetContext.packageName} android:write_settings ignore")
    }

    fun launchApp() {
        device.pressHome()
        shell("am start -n ${targetContext.packageName}/.MainActivity")
        device.waitForIdle()
    }

    fun clickAppText(text: String) {
        waitForText(text)
        device.findObject(By.text(text)).click()
        device.waitForIdle()
    }

    fun waitForText(text: String, timeoutMs: Long = 5_000): UiObject2 {
        return requireNotNull(device.wait(Until.findObject(By.text(text)), timeoutMs)) {
            "Text not found: $text"
        }
    }

    fun acceptRuntimePermissionDialog() {
        val permissionController = "com.android.permissioncontroller"
        val allow = device.wait(
            Until.findObject(By.res(permissionController, "permission_allow_button")),
            5_000,
        ) ?: device.wait(Until.findObject(By.textContains("Allow")), 2_000)
            ?: device.wait(Until.findObject(By.textContains("Zulassen")), 2_000)
        requireNotNull(allow) { "Runtime permission dialog did not appear" }.click()
        device.waitForIdle()
    }

    fun denyRuntimePermissionDialog() {
        val permissionController = "com.android.permissioncontroller"
        val deny = device.wait(
            Until.findObject(By.res(permissionController, "permission_deny_button")),
            5_000,
        ) ?: device.wait(Until.findObject(By.textContains("Don\u2019t allow")), 2_000)
            ?: device.wait(Until.findObject(By.textContains("Don't allow")), 2_000)
            ?: device.wait(Until.findObject(By.textContains("Nicht zulassen")), 2_000)
        requireNotNull(deny) { "Runtime permission deny action did not appear" }.click()
        device.waitForIdle()
    }

    fun enableWriteSettings() {
        require(device.wait(Until.hasObject(By.pkg("com.android.settings")), 5_000)) {
            "Android Settings did not open"
        }
        val switch = device.findObject(By.res("android", "switch_widget"))
            ?: device.findObject(By.clazz("android.widget.Switch"))
        requireNotNull(switch) { "WRITE_SETTINGS switch not found" }
        if (switch.isChecked != true) switch.click()
        device.pressBack()
        device.waitForIdle()
    }

    fun waitForPackage(packageName: String, timeoutMs: Long = 5_000) {
        require(device.wait(Until.hasObject(By.pkg(packageName)), timeoutMs)) {
            "Package did not become visible: $packageName"
        }
    }

    fun clickDocumentsFile(displayName: String) {
        clickDocumentsFiles(listOf(displayName))
    }

    fun clickDocumentsFiles(displayNames: List<String>) {
        waitForPackage("com.google.android.documentsui")
        displayNames.forEach { displayName ->
            require(device.wait(Until.hasObject(By.text(displayName)), 5_000)) {
                "DocumentsUI file not found: $displayName"
            }
            device.findObject(By.text(displayName)).click()
        }
        val open = device.wait(Until.findObject(By.res("com.google.android.documentsui", "action")), 2_000)
            ?: device.wait(Until.findObject(By.textContains("Open")), 2_000)
            ?: device.wait(Until.findObject(By.textContains("Öffnen")), 2_000)
        open?.click()
        device.waitForIdle()
    }

    fun waitForBootCompleted(timeoutMs: Long = 120_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (shell("getprop sys.boot_completed").trim() == "1") return
            Thread.sleep(1_000)
        }
        error("Emulator did not finish booting within ${timeoutMs}ms")
    }

    fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun apiLevel(): Int = Build.VERSION.SDK_INT
}
