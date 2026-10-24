package de.emailspeicher.zufallsklingelton

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class RingtoneSystemRule : TestWatcher() {
    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private var originalRingtone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
    val originalRingtoneUri: android.net.Uri?
        get() = originalRingtone
    private var fixture: MediaStoreFixture? = null

    override fun starting(description: Description) {
        clearState()
        originalRingtone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        fixture = MediaStoreFixture(context)
        checkSettingsAccess()
    }

    override fun finished(description: Description) {
        runCatching {
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, originalRingtone)
            clearState()
            fixture?.cleanup()
            fixture = null
        }
    }

    fun createTrack(title: String, artist: String = "Instrumentation Artist"): MediaStoreFixtureTrack =
        fixture?.create(title, artist)
            ?: throw AssertionError("Die RingtoneSystemRule wurde nicht initialisiert.")

    fun removeTrack(uri: android.net.Uri) {
        fixture?.remove(uri)
    }

    private fun checkSettingsAccess() {
        if (!Settings.System.canWrite(context)) {
            fail("WRITE_SETTINGS fehlt. Test-Emulator vorbereiten: adb shell appops set ${context.packageName} android:write_settings allow")
        }
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (context.checkSelfPermission(audioPermission) != PackageManager.PERMISSION_GRANTED) fail("Audio-Berechtigung fehlt: $audioPermission")
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) fail("READ_PHONE_STATE fehlt")
    }

    private fun clearState() {
        context.getSharedPreferences("track_pool", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("playback_state", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
