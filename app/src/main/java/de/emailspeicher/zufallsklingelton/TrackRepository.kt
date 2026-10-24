package de.emailspeicher.zufallsklingelton

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TrackRepository(context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<Track> {
        val stored = runCatching { decode(preferences.getString(TRACKS_KEY, "[]").orEmpty()) }
            .getOrDefault(emptyList())
        val reachable = stored.filter(::isReachable)
        if (reachable.size != stored.size) save(reachable)
        return reachable.sortedBy { it.order }
    }

    fun addAll(candidates: List<Track>): List<Track> {
        val current = mergeTracks(load(), candidates)
        save(current)
        return current
    }

    fun delete(id: String): List<Track> {
        val remaining = load().filterNot { it.id == id }
        save(remaining)
        return remaining
    }

    fun clear() {
        save(emptyList())
    }

    fun isReachable(track: Track): Boolean = runCatching {
        resolver.openAssetFileDescriptor(Uri.parse(track.uri), "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun save(tracks: List<Track>) {
        preferences.edit().putString(TRACKS_KEY, encode(tracks)).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "track_pool"
        private const val TRACKS_KEY = "tracks"

        fun fromMedia(
            uri: Uri,
            title: String,
            artist: String,
            durationMs: Long,
        ): Track = Track(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            title = title,
            artist = artist,
            durationMs = durationMs,
            order = 0,
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
                        ),
                    )
                }
            }
        }
    }
}
