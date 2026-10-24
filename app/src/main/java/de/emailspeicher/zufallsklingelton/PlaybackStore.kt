package de.emailspeicher.zufallsklingelton

import android.content.Context

class PlaybackStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var active: Boolean
        get() = preferences.getBoolean(ACTIVE_KEY, false)
        set(value) = preferences.edit().putBoolean(ACTIVE_KEY, value).apply()

    var mode: SwitchMode
        get() = runCatching { SwitchMode.valueOf(preferences.getString(MODE_KEY, null).orEmpty()) }
            .getOrDefault(SwitchMode.DAILY)
        set(value) = preferences.edit().putString(MODE_KEY, value.name).apply()

    var currentTrackId: String?
        get() = preferences.getString(CURRENT_TRACK_KEY, null)
        set(value) = preferences.edit().putString(CURRENT_TRACK_KEY, value).apply()

    var lastSwitchDate: String?
        get() = preferences.getString(LAST_SWITCH_DATE_KEY, null)
        set(value) = preferences.edit().putString(LAST_SWITCH_DATE_KEY, value).apply()

    var previousRingtoneUri: String?
        get() = preferences.getString(PREVIOUS_RINGTONE_KEY, null)
        set(value) = preferences.edit().putString(PREVIOUS_RINGTONE_KEY, value).apply()

    var ringing: Boolean
        get() = preferences.getBoolean(RINGING_KEY, false)
        set(value) = preferences.edit().putBoolean(RINGING_KEY, value).apply()

    fun appendHistory(trackId: String) {
        val history = preferences.getStringSet(HISTORY_KEY, emptySet()).orEmpty().toMutableSet()
        history += trackId
        preferences.edit().putStringSet(HISTORY_KEY, history).apply()
    }

    fun clearActivationState() {
        preferences.edit()
            .remove(CURRENT_TRACK_KEY)
            .remove(LAST_SWITCH_DATE_KEY)
            .remove(PREVIOUS_RINGTONE_KEY)
            .remove(HISTORY_KEY)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "playback_state"
        private const val ACTIVE_KEY = "active"
        private const val MODE_KEY = "mode"
        private const val CURRENT_TRACK_KEY = "current_track_id"
        private const val LAST_SWITCH_DATE_KEY = "last_switch_date"
        private const val PREVIOUS_RINGTONE_KEY = "previous_ringtone_uri"
        private const val HISTORY_KEY = "history"
        private const val RINGING_KEY = "ringing"
    }
}
