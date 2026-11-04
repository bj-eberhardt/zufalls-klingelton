package de.emailspeicher.zufallsklingelton

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

internal object InstrumentationTestSetup {
    fun grantSystemPermissions(context: Context) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            automation.grantRuntimePermission(context.packageName, audioPermission())
            automation.grantRuntimePermission(context.packageName, Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                automation.grantRuntimePermission(context.packageName, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            runShellCommand("pm grant ${context.packageName} ${audioPermission()}")
            runShellCommand("pm grant ${context.packageName} ${Manifest.permission.READ_PHONE_STATE}")
            runShellCommand("pm grant ${context.packageName} ${Manifest.permission.WRITE_EXTERNAL_STORAGE}")
        }
        runShellCommand("appops set ${context.packageName} android:write_settings allow")
    }

    private fun runShellCommand(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
