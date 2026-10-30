package de.emailspeicher.zufallsklingelton

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

class RingtoneController(private val context: Context) {
    private val store = PlaybackStore(context)

    fun setTrack(track: Track) {
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE,
            Uri.parse(track.uri),
        )
        store.currentTrackId = track.id
        store.appendHistory(track.id)
    }

    fun currentRingtoneUri(): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)

    fun previousRingtoneName(): String? {
        val storedUri = store.previousRingtoneUri ?: return null
        if (storedUri.isEmpty()) return context.getString(R.string.system_default_ringtone)
        return runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(storedUri))?.getTitle(context)
        }.getOrNull()?.ifBlank { context.getString(R.string.unknown_ringtone) }
            ?: context.getString(R.string.unknown_ringtone)
    }

    fun rememberCurrentRingtone() {
        if (store.previousRingtoneUri == null) {
            val current = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            store.previousRingtoneUri = current?.let { uri ->
                if (uri.authority == "media") {
                    uri.toString()
                } else {
                    AudioLibraryRepository(context).readTrack(uri)?.uri.orEmpty()
                }
            }.orEmpty()
        }
    }

    fun restorePreviousRingtone() {
        val previous = store.previousRingtoneUri?.takeIf { it.isNotEmpty() }?.let(Uri::parse)
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, previous)
        store.clearActivationState()
    }

    fun preview(uri: Uri, onError: () -> Unit, onFinished: () -> Unit): MediaPlayer? =
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(context, uri)
                setOnCompletionListener { onFinished() }
                setOnErrorListener { _, _, _ -> onError(); true }
                prepare()
                start()
            }
        }.getOrElse {
            onError()
            null
        }
}
