package de.emailspeicher.zufallsklingelton

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream

data class MediaStoreFixtureTrack(val uri: Uri, val track: Track)

class MediaStoreFixture(private val context: Context) {
    private val resolver = context.contentResolver
    private val createdUris = mutableListOf<Uri>()

    fun create(title: String, artist: String = "Instrumentation Artist"): MediaStoreFixtureTrack {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.wav")
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.DURATION, 250L)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values))
        createdUris += uri
        resolver.openOutputStream(uri).use { output -> requireNotNull(output).write(WavFixture.bytes()) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        }
        return MediaStoreFixtureTrack(uri, TrackRepository.fromMedia(uri, title, artist, 250L))
    }

    fun remove(uri: Uri) {
        resolver.delete(uri, null, null)
        createdUris.remove(uri)
    }

    fun cleanup() {
        createdUris.toList().forEach { resolver.delete(it, null, null) }
        createdUris.clear()
    }
}

private object WavFixture {
    fun bytes(sampleRate: Int = 8_000, durationMs: Int = 250): ByteArray {
        val sampleCount = sampleRate * durationMs / 1_000
        val dataSize = sampleCount * 2
        val bytes = ByteArray(44 + dataSize)
        fun putAscii(offset: Int, value: String) = value.toByteArray(Charsets.US_ASCII).copyInto(bytes, offset)
        fun putInt(offset: Int, value: Int) = repeat(4) { bytes[offset + it] = (value shr (it * 8)).toByte() }
        fun putShort(offset: Int, value: Int) = repeat(2) { bytes[offset + it] = (value shr (it * 8)).toByte() }
        putAscii(0, "RIFF"); putInt(4, 36 + dataSize); putAscii(8, "WAVE")
        putAscii(12, "fmt "); putInt(16, 16); putShort(20, 1); putShort(22, 1)
        putInt(24, sampleRate); putInt(28, sampleRate * 2); putShort(32, 2); putShort(34, 16)
        putAscii(36, "data"); putInt(40, dataSize)
        for (sample in 0 until sampleCount) {
            val amplitude = (kotlin.math.sin(2.0 * Math.PI * 440.0 * sample / sampleRate) * 8_000).toInt()
            putShort(44 + sample * 2, amplitude)
        }
        return bytes
    }
}
