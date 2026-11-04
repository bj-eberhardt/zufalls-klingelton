package de.emailspeicher.zufallsklingelton

import android.media.RingtoneManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformAcceptanceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val state = context.getSharedPreferences("platform_acceptance", 0)

    @Test
    fun preparePhoneCall() {
        preparePermissionsAndTracks()
        val coordinator = RingtoneCoordinator(context)
        coordinator.setMode(SwitchMode.AFTER_INCOMING_CALL)
        coordinator.setActive(true)
        saveState("phone")
    }

    @Test
    fun assertPhoneCallChangedExactlyOnce() {
        assertPrepared("phone")
        val changed = currentRingtone()
        val tracks = state.getStringSet("tracks", emptySet()).orEmpty()
        assertTrue("Phone acceptance did not select a pool track", changed?.toString() in tracks)
        assertNotEquals("Phone acceptance did not change the ringtone", state.getString("before", null), changed?.toString())
        cleanupState()
    }

    @Test
    fun prepareReboot() {
        preparePermissionsAndTracks()
        val coordinator = RingtoneCoordinator(context)
        coordinator.setMode(SwitchMode.DAILY)
        coordinator.setActive(true)
        PlaybackStore(context).lastSwitchDate = "2000-01-01"
        saveState("reboot")
    }

    @Test
    fun assertRebootDispatchedBootReceiver() {
        assertPrepared("reboot")
        val changed = currentRingtone()
        val tracks = state.getStringSet("tracks", emptySet()).orEmpty()
        assertTrue("Boot acceptance did not select a pool track", changed?.toString() in tracks)
        assertNotEquals("Boot receiver did not switch the ringtone", state.getString("before", null), changed?.toString())
        cleanupState()
    }

    private fun preparePermissionsAndTracks() {
        InstrumentationTestSetup.grantSystemPermissions(context)
        clearState()
        state.edit().clear().commit()
        val fixture = MediaStoreFixture(context)
        val first = fixture.create("platform-one").track
        val second = fixture.create("platform-two").track
        RingtoneCoordinator(context).addTracks(listOf(first, second))
    }

    private fun saveState(kind: String) {
        val pool = RingtoneCoordinator(context).pool()
        state.edit()
            .putString("kind", kind)
            .putString("before", currentRingtone()?.toString())
            .putString("original", PlaybackStore(context).previousRingtoneUri)
            .putStringSet("tracks", pool.map { it.uri }.toSet())
            .commit()
    }

    private fun assertPrepared(expectedKind: String) {
        assertTrue("No platform acceptance state found", state.getString("kind", null) == expectedKind)
        InstrumentationTestSetup.grantSystemPermissions(context)
    }

    private fun cleanupState() {
        state.getStringSet("tracks", emptySet()).orEmpty().forEach { uri ->
            context.contentResolver.delete(Uri.parse(uri), null, null)
        }
        val original = state.getString("original", null)?.let(Uri::parse)
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, original)
        clearState()
        state.edit().clear().commit()
    }

    private fun clearState() {
        context.getSharedPreferences("track_pool", 0).edit().clear().commit()
        context.getSharedPreferences("playback_state", 0).edit().clear().commit()
    }

    private fun currentRingtone(): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
}
