package de.emailspeicher.zufallsklingelton

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val audioLibraryGranted: Boolean,
    val phoneStateGranted: Boolean,
    val ringtoneChangeGranted: Boolean,
    val mediaStoreWriteGranted: Boolean = true,
) {
    val allGranted: Boolean
        get() = audioLibraryGranted && phoneStateGranted && ringtoneChangeGranted && mediaStoreWriteGranted
}

fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

fun readPermissionStatus(context: Context): PermissionStatus = PermissionStatus(
    audioLibraryGranted = context.hasPermission(audioPermission()),
    phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE),
    ringtoneChangeGranted = Settings.System.canWrite(context),
    mediaStoreWriteGranted = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
        context.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE),
)

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
