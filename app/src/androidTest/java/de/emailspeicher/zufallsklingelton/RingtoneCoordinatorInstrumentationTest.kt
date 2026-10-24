package de.emailspeicher.zufallsklingelton

import android.media.RingtoneManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RingtoneCoordinatorInstrumentationTest {
    @get:Rule
    val system = RingtoneSystemRule()

    @Test
    fun activationSetsPoolTrackAndStoresOriginalRingtone() {
        val tracks = addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setActive(true)
        val playback = PlaybackStore(system.context)
        val currentUri = currentRingtone()

        assertTrue(playback.active)
        assertTrue(currentUri?.toString() == tracks[0].uri || currentUri?.toString() == tracks[1].uri)
        assertTrue(playback.currentTrackId != null)
        assertTrue(playback.previousRingtoneUri != null)
    }

    @Test
    fun startupInDailyModeSwitchesAfterPreviousDate() {
        val tracks = addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setActive(true)
        val before = currentRingtone()
        PlaybackStore(system.context).apply {
            mode = SwitchMode.DAILY
            lastSwitchDate = "2000-01-01"
        }

        coordinator.onStartup()

        assertTrue(currentRingtone()?.toString() == tracks[0].uri || currentRingtone()?.toString() == tracks[1].uri)
        assertNotEquals(before, currentRingtone())
    }

    @Test
    fun incomingCallModeSwitchesToAnotherTrack() {
        val tracks = addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setActive(true)
        coordinator.setMode(SwitchMode.AFTER_INCOMING_CALL)
        val before = currentRingtone()

        coordinator.onIncomingCallFinished()

        assertTrue(currentRingtone()?.toString() == tracks[0].uri || currentRingtone()?.toString() == tracks[1].uri)
        assertNotEquals(before, currentRingtone())
    }

    @Test
    fun deletingCurrentTrackImmediatelySelectsTheOtherTrack() {
        val tracks = addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setActive(true)
        val current = coordinator.pool().first { it.uri == currentRingtone().toString() }
        val other = tracks.first { it.uri != current.uri }

        coordinator.deleteTrack(current.id)

        assertTrue(PlaybackStore(system.context).active)
        assertEquals(other.uri, currentRingtone().toString())
        assertEquals(other.id, PlaybackStore(system.context).currentTrackId)
    }

    @Test
    fun clearingPoolDisablesAndRestoresPreviousRingtone() {
        addTwoTracks()
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.setActive(true)
        val storedOriginal = PlaybackStore(system.context).previousRingtoneUri

        coordinator.clearTracks()

        val playback = PlaybackStore(system.context)
        assertFalse(playback.active)
        assertEquals(system.originalRingtoneUri, currentRingtone())
        assertEquals(null, playback.currentTrackId)
        assertEquals(null, playback.previousRingtoneUri)
        assertNotEquals(null, storedOriginal)
    }

    @Test
    fun unreachableTrackIsRemovedAndEmptyPoolDisablesFeature() {
        val track = system.createTrack("unreachable")
        val coordinator = RingtoneCoordinator(system.context)
        coordinator.addTracks(listOf(track.track))
        coordinator.setActive(true)
        system.removeTrack(track.uri)

        coordinator.onStartup()

        assertTrue(coordinator.pool().isEmpty())
        assertFalse(PlaybackStore(system.context).active)
    }

    private fun addTwoTracks(): List<Track> {
        val first = system.createTrack("fixture-one").track
        val second = system.createTrack("fixture-two").track
        RingtoneCoordinator(system.context).addTracks(listOf(first, second))
        return listOf(first, second)
    }

    private fun currentRingtone(): Uri? = RingtoneManager.getActualDefaultRingtoneUri(system.context, RingtoneManager.TYPE_RINGTONE)
}
