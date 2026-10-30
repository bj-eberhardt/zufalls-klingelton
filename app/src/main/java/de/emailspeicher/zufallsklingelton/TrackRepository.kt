package de.emailspeicher.zufallsklingelton

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TrackRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<Track> {
        val stored = runCatching { decode(preferences.getString(TRACKS_KEY, "[]").orEmpty()) }
            .getOrDefault(emptyList())
        val audioRepository = AudioLibraryRepository(context)
        val migrated = stored.mapNotNull { track ->
            if (Uri.parse(track.uri).authority == "media") {
                track
            } else {
                audioRepository.readTrack(Uri.parse(track.uri))?.let { imported ->
                    track.copy(uri = imported.uri, isManagedByApp = true)
                }
            }
        }
        Log.d(TAG, "Loaded ${stored.size} stored track(s), migrated ${migrated.size} track(s)")
        val reachable = migrated.filter(::isReachable)
        Log.d(TAG, "${reachable.size} track(s) remain reachable after validation")
        if (reachable != stored) save(reachable)
        return reachable.sortedBy { it.order }
    }

    fun addAll(candidates: List<Track>): List<Track> {
        val current = mergeTracks(load(), candidates)
        save(current)
        return current
    }

    fun delete(id: String): List<Track> {
        val current = load()
        current.firstOrNull { it.id == id }?.let(::deleteManagedMedia)
        val remaining = current.filterNot { it.id == id }
        save(remaining)
        return remaining
    }

    fun clear() {
        load().filter { it.isManagedByApp }.forEach(::deleteManagedMedia)
        save(emptyList())
    }

    private fun deleteManagedMedia(track: Track) {
        if (track.isManagedByApp && Uri.parse(track.uri).authority == "media") {
            val deletedRows = resolver.delete(Uri.parse(track.uri), null, null)
            Log.d(TAG, "Deleted app-managed media ${track.uri}, rows=$deletedRows")
        }
    }

    fun isReachable(track: Track): Boolean = runCatching {
        resolver.openAssetFileDescriptor(Uri.parse(track.uri), "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun save(tracks: List<Track>) {
        preferences.edit().putString(TRACKS_KEY, encode(tracks)).apply()
    }

    companion object {
        private const val TAG = "TrackRepository"
        private const val PREFERENCES_NAME = "track_pool"
        private const val TRACKS_KEY = "tracks"

        fun fromMedia(
            uri: Uri,
            title: String,
            artist: String,
            durationMs: Long,
            managedByApp: Boolean = false,
        ): Track = Track(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            title = title,
            artist = artist,
            durationMs = durationMs,
            order = 0,
            isManagedByApp = managedByApp,
        )

        fun encode(tracks: List<Track>): String = JSONArray().apply {
            tracks.forEach { track ->
                put(JSONObject().apply {
                    put("id", track.id)
                    put("uri", track.uri)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("durationMs", track.durationMs)
                    put("order", track.order)
                    put("isManagedByApp", track.isManagedByApp)
                })
            }
        }.toString()

        fun decode(json: String): List<Track> {
            val array = JSONArray(json)
            return buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        Track(
                            id = item.getString("id"),
                            uri = item.getString("uri"),
                            title = item.optString("title"),
                            artist = item.optString("artist"),
                            durationMs = item.optLong("durationMs"),
                            order = item.optLong("order", index.toLong()),
                            isManagedByApp = item.optBoolean("isManagedByApp", false),
                        ),
                    )
                }
            }
        }
    }
}
