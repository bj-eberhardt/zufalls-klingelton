package de.emailspeicher.zufallsklingelton

import android.content.Intent
import android.media.RingtoneManager
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiverInstrumentationTest {
    @get:Rule
    val system = RingtoneSystemRule()

    private val synchronousRunner = ReceiverWorkRunner { _, work -> work() }

    @Test
    fun bootReceiverRunsDailyStartupThroughReceiverDispatch() {
        val tracks = addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setMode(SwitchMode.DAILY)
        coordinator.setActive(true)
        val before = currentRingtone()
        PlaybackStore(system.context).lastSwitchDate = "2000-01-01"

        BootReceiver(synchronousRunner).onReceive(system.context, Intent(Intent.ACTION_BOOT_COMPLETED))

        await("BootReceiver did not select a pool track") {
            currentRingtone() != before && (currentRingtone()?.toString() == tracks[0].uri || currentRingtone()?.toString() == tracks[1].uri)
        }
        assertTrue(PlaybackStore(system.context).currentTrackId != null)
    }

    @Test
    fun dateChangedReceiverSwitchesOnlyWhenFeatureIsActive() {
        addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setMode(SwitchMode.DAILY)
        coordinator.setActive(true)
        val before = currentRingtone()
        PlaybackStore(system.context).lastSwitchDate = "2000-01-01"

        DateChangedReceiver(synchronousRunner).onReceive(system.context, Intent(Intent.ACTION_DATE_CHANGED))

        await("DateChangedReceiver did not switch") { currentRingtone() != before }
        coordinator.setActive(false)
        val restored = currentRingtone()
        DateChangedReceiver(synchronousRunner).onReceive(system.context, Intent(Intent.ACTION_DATE_CHANGED))
        Thread.sleep(250)
        assertEquals(restored, currentRingtone())
    }

    @Test
    fun phoneReceiverSwitchesExactlyOnceAfterRingingThenIdle() {
        val tracks = addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setMode(SwitchMode.AFTER_INCOMING_CALL)
        coordinator.setActive(true)
        val before = currentRingtone()

        sendPhoneState(TelephonyManager.EXTRA_STATE_RINGING)
        await("PhoneStateReceiver did not remember RINGING") { PlaybackStore(system.context).ringing }
        sendPhoneState(TelephonyManager.EXTRA_STATE_RINGING)
        sendPhoneState(TelephonyManager.EXTRA_STATE_IDLE)
        await("PhoneStateReceiver did not finish incoming call") {
            !PlaybackStore(system.context).ringing && currentRingtone() != before
        }
        val afterFirstIdle = currentRingtone()
        sendPhoneState(TelephonyManager.EXTRA_STATE_IDLE)
        Thread.sleep(250)

        assertTrue(afterFirstIdle?.toString() == tracks[0].uri || afterFirstIdle?.toString() == tracks[1].uri)
        assertEquals(afterFirstIdle, currentRingtone())
        assertNotEquals(before, afterFirstIdle)
    }

    @Test
    fun receiverSeamRunsWorkSynchronouslyForDeterministicCoverage() {
        var executed = false
        val runner = ReceiverWorkRunner { _, work -> work(); executed = true }

        BootReceiver(runner).onReceive(system.context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertTrue(executed)
    }

    private fun addTwoTracks(): List<Track> {
        val first = system.createTrack("receiver-one").track
        val second = system.createTrack("receiver-two").track
        RingtoneCoordinator(system.context).addTracks(listOf(first, second))
        return listOf(first, second)
    }

    private fun sendPhoneState(state: String) {
        PhoneStateReceiver(synchronousRunner).onReceive(
            system.context,
            Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                .putExtra(TelephonyManager.EXTRA_STATE, state),
        )
    }

    private fun currentRingtone() = RingtoneManager.getActualDefaultRingtoneUri(system.context, RingtoneManager.TYPE_RINGTONE)

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue(message, condition())
    }
}
