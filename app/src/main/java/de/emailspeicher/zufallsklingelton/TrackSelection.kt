package de.emailspeicher.zufallsklingelton

import kotlin.random.Random

fun chooseNextTrack(tracks: List<Track>, currentTrackId: String?, random: Random = Random.Default): Track? {
    if (tracks.isEmpty()) return null
    return tracks.filterNot { it.id == currentTrackId }.ifEmpty { tracks }.random(random)
}

fun shouldSwitchForDate(lastSwitchDate: String?, today: String): Boolean = lastSwitchDate != today

fun shouldSwitchOnStartup(
    active: Boolean,
    hasTracks: Boolean,
    currentSystemMatchesPool: Boolean,
    storedCurrentTrackExists: Boolean,
    mode: SwitchMode,
    lastSwitchDate: String?,
    today: String,
): Boolean {
    if (!active || !hasTracks) return false
    if (mode == SwitchMode.DAILY && shouldSwitchForDate(lastSwitchDate, today)) return true
    return !currentSystemMatchesPool || !storedCurrentTrackExists
}

enum class PhoneStateAction {
    IGNORE,
    REMEMBER_RINGING,
    FINISH_INCOMING_CALL,
}

const val PHONE_STATE_RINGING = "RINGING"
const val PHONE_STATE_IDLE = "IDLE"

fun phoneStateAction(wasRinging: Boolean, state: String?): PhoneStateAction = when {
    state == PHONE_STATE_RINGING -> PhoneStateAction.REMEMBER_RINGING
    state == PHONE_STATE_IDLE && wasRinging -> PhoneStateAction.FINISH_INCOMING_CALL
    else -> PhoneStateAction.IGNORE
}

fun isBootCompletedAction(action: String?): Boolean = action == android.content.Intent.ACTION_BOOT_COMPLETED

fun isDateChangedAction(action: String?): Boolean = action == android.content.Intent.ACTION_DATE_CHANGED

fun mergeTracks(existing: List<Track>, candidates: List<Track>): List<Track> {
    val result = existing.toMutableList()
    val knownUris = result.mapTo(mutableSetOf()) { it.uri }
    var nextOrder = (result.maxOfOrNull { it.order } ?: -1L) + 1
    candidates.forEach { candidate ->
        if (knownUris.add(candidate.uri)) result += candidate.copy(order = nextOrder++)
    }
    return result.sortedBy { it.order }
}
