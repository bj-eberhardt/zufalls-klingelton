package de.emailspeicher.zufallsklingelton

import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns

class AudioLibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    fun queryTracks(query: String): List<Track> {
        val tracksByUri = linkedMapOf<String, Track>()
        listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.INTERNAL_CONTENT_URI).forEach { mediaUri ->
            resolver.query(
                mediaUri,
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DURATION),
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%", "%$query%", "%$query%"),
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val uri = Uri.withAppendedPath(mediaUri, cursor.getString(idColumn))
                    val title = cursor.getString(titleColumn).orEmpty()
                        .ifBlank { cursor.getString(displayNameColumn).orEmpty() }
                        .ifBlank { appContext.getString(R.string.unknown_track) }
                    tracksByUri.putIfAbsent(
                        uri.toString(),
                        TrackRepository.fromMedia(uri, title, cursor.getString(artistColumn).orEmpty(), cursor.getLong(durationColumn)),
                    )
                }
            }
        }

        runCatching {
            val ringtoneManager = RingtoneManager(appContext).apply { setType(RingtoneManager.TYPE_RINGTONE) }
            ringtoneManager.cursor.use { cursor ->
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                var position = 0
                while (cursor.moveToNext()) {
                    val uri = ringtoneManager.getRingtoneUri(position)
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty()
                        .ifBlank { appContext.getString(R.string.unknown_track) }
                    val artist = if (artistColumn >= 0) cursor.getString(artistColumn).orEmpty() else ""
                    if (uri != null && (title.contains(query, ignoreCase = true) || artist.contains(query, ignoreCase = true))) {
                        tracksByUri.putIfAbsent(uri.toString(), TrackRepository.fromMedia(uri, title, artist, 0L))
                    }
                    position++
                }
            }
        }

        return tracksByUri.values.sortedBy { it.title.lowercase() }
    }

    fun readTrack(uri: Uri): Track? {
        var title = ""
        var duration = 0L
        resolver.query(uri, arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION, OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                title = cursor.stringOrEmpty(MediaStore.Audio.Media.TITLE)
                duration = cursor.longOrZero(MediaStore.Audio.Media.DURATION)
                if (title.isBlank()) title = cursor.stringOrEmpty(OpenableColumns.DISPLAY_NAME)
            }
        }
        if (title.isBlank()) title = uri.lastPathSegment.orEmpty().ifBlank { appContext.getString(R.string.unknown_track) }
        return runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { TrackRepository.fromMedia(uri, title, "", duration) }
        }.getOrNull()
    }

    private fun Cursor.stringOrEmpty(column: String): String {
        val index = getColumnIndex(column)
        return if (index >= 0) getString(index).orEmpty() else ""
    }

    private fun Cursor.longOrZero(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0) getLong(index) else 0L
    }
}
