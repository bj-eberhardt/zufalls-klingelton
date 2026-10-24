package de.emailspeicher.zufallsklingelton

import android.content.Context
import java.time.LocalDate

class RingtoneCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val tracks = TrackRepository(appContext)
    private val playback = PlaybackStore(appContext)
    private val ringtone = RingtoneController(appContext)

    fun pool(): List<Track> = tracks.load()

    fun addTracks(candidates: List<Track>): List<Track> = tracks.addAll(candidates)

    fun previousRingtoneName(): String? = ringtone.previousRingtoneName()

    fun setActive(active: Boolean): List<Track> {
        if (!active) {
            if (playback.active) ringtone.restorePreviousRingtone()
            playback.active = false
            return tracks.load()
        }
        val available = tracks.load()
        if (available.isEmpty()) return available
        ringtone.rememberCurrentRingtone()
        playback.active = true
        setNext(available)
        return available
    }

    fun setMode(mode: SwitchMode) {
        playback.mode = mode
    }

    fun deleteTrack(id: String): List<Track> {
        val remaining = tracks.delete(id)
        if (playback.active && playback.currentTrackId == id) syncActivePool(remaining)
        return remaining
    }

    fun clearTracks(): List<Track> {
        tracks.clear()
        if (playback.active) {
            ringtone.restorePreviousRingtone()
            playback.active = false
        }
        return emptyList()
    }

    fun onStartup() {
        val available = tracks.load()
        if (!playback.active) return
        if (available.isEmpty()) {
            ringtone.restorePreviousRingtone()
            playback.active = false
            return
        }
        val currentSystemUri = ringtone.currentRingtoneUri()?.toString()
        val matchingTrack = available.firstOrNull { it.uri == currentSystemUri }
        if (matchingTrack != null) playback.currentTrackId = matchingTrack.id

        if (shouldSwitchOnStartup(
                active = playback.active,
                hasTracks = available.isNotEmpty(),
                currentSystemMatchesPool = matchingTrack != null,
                storedCurrentTrackExists = playback.currentTrackId != null && available.any { it.id == playback.currentTrackId },
                mode = playback.mode,
                lastSwitchDate = playback.lastSwitchDate,
                today = today(),
            )) {
            setNext(available)
        }
    }

    fun onIncomingCallFinished() {
        if (playback.active && playback.mode == SwitchMode.AFTER_INCOMING_CALL) {
            syncActivePool(tracks.load())
        }
    }

    fun onDateChanged() {
        if (playback.active && playback.mode == SwitchMode.DAILY && shouldSwitchForDate(playback.lastSwitchDate, today())) {
            syncActivePool(tracks.load())
        }
    }

    private fun syncActivePool(available: List<Track>) {
        if (available.isEmpty()) {
            ringtone.restorePreviousRingtone()
            playback.active = false
        } else {
            setNext(available)
        }
    }

    private fun setNext(available: List<Track>) {
        val next = chooseNextTrack(available, playback.currentTrackId) ?: return
        ringtone.setTrack(next)
        playback.lastSwitchDate = today()
    }

    private fun today(): String = LocalDate.now().toString()
}
