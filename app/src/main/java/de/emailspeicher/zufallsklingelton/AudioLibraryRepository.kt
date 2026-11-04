package de.emailspeicher.zufallsklingelton

import android.content.Context
import android.content.ContentValues
import android.content.ContentUris
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

internal fun audioExtension(title: String, mimeType: String): String {
    val titleExtension = title.substringAfterLast('.', "")
        .takeIf { it.matches(Regex("[A-Za-z0-9]{1,5}")) }
    return titleExtension?.lowercase() ?: when (mimeType.lowercase()) {
        "audio/mpeg", "audio/mp3" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/mp4" -> "m4a"
        else -> mimeType.substringAfter('/', "mp3")
    }
}

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

    fun readTrack(uri: Uri): Track? = runCatching {
        Log.d(TAG, "Reading selected audio URI: $uri")
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
        Log.d(TAG, "Read metadata: title=$title, durationMs=$duration, authority=${uri.authority}")
        val mediaUri = if (uri.authority == "media") uri else importIntoMediaStore(uri, title)
        Log.d(TAG, "Resolved audio URI: $mediaUri")
        if (mediaUri == null) {
            Log.e(TAG, "Audio import failed: sourceUri=$uri, title=$title, mimeType=${resolver.getType(uri)}")
        }
        mediaUri?.let { TrackRepository.fromMedia(it, title, "", duration, managedByApp = uri.authority != "media") }
    }.onFailure { error ->
        Log.e(TAG, "Unable to import audio track: $uri", error)
    }.getOrNull()

    private fun importIntoMediaStore(sourceUri: Uri, title: String): Uri? = runCatching {
        val mimeType = resolver.getType(sourceUri) ?: "audio/mpeg"
        val extension = audioExtension(title, mimeType)
        val safeTitle = title.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val displayName = if (safeTitle.endsWith(".$extension", ignoreCase = true)) {
            safeTitle
        } else {
            "$safeTitle.$extension"
        }
        Log.d(TAG, "Copying audio to MediaStore: sourceUri=$sourceUri, mimeType=$mimeType, displayName=$displayName")
        val ringtoneDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
        val relativePath = "Ringtones/Zufalls-Klingelton"
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val existingUri = resolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media._ID),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
            } else {
                "${MediaStore.Audio.Media.DATA} = ?"
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(displayName, relativePath)
            } else {
                arrayOf(File(ringtoneDirectory, displayName).absolutePath)
            },
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                ContentUris.withAppendedId(collection, cursor.getLong(0))
            } else {
                null
            }
        }
        if (existingUri != null) {
            Log.d(TAG, "Reusing existing MediaStore audio: $existingUri")
            return@runCatching existingUri
        }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.IS_RINGTONE, 1)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else {
                ringtoneDirectory.mkdirs()
                put(MediaStore.Audio.Media.DATA, File(ringtoneDirectory, displayName).absolutePath)
            }
        }
        val destinationUri = resolver.insert(collection, values)
            ?: return@runCatching null
        Log.d(TAG, "MediaStore insert succeeded: destinationUri=$destinationUri")
        try {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destinationUri)?.use { output ->
                    val copiedBytes = input.copyTo(output)
                    Log.d(TAG, "Copied $copiedBytes byte(s) to $destinationUri")
                }
                    ?: error("Unable to open destination stream")
            } ?: error("Unable to open source stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updatedRows = resolver.update(destinationUri, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }, null, null)
                Log.d(TAG, "MediaStore pending flag cleared: updatedRows=$updatedRows")
            }
            destinationUri
        } catch (error: Throwable) {
            resolver.delete(destinationUri, null, null)
            throw error
        }
    }.onFailure { error ->
        Log.e(TAG, "Unable to copy audio track into MediaStore: $sourceUri", error)
    }.getOrNull()

    private companion object {
        const val TAG = "AudioLibraryRepository"
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
