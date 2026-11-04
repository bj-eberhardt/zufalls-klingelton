package de.emailspeicher.zufallsklingelton

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExistingMediaStoreImportInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private var sourceUri: Uri? = null
    private var existingTargetUri: Uri? = null

    @Before
    fun clearPool() {
        context.getSharedPreferences("track_pool", 0).edit().clear().commit()
    }

    @After
    fun cleanup() {
        sourceUri?.let { resolver.delete(it, null, null) }
        existingTargetUri?.let { resolver.delete(it, null, null) }
        context.getSharedPreferences("track_pool", 0).edit().clear().commit()
    }

    @Test
    fun existingTargetFileIsReusedAndAddedToPool() {
        assumeTrue("Test requires MediaStore volume and relative paths", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val fileName = "existing-import.mp3"
        existingTargetUri = createTargetFile(fileName)
        sourceUri = createDownloadFile(fileName)

        val imported = requireNotNull(AudioLibraryRepository(context).readTrack(sourceUri!!))
        assertEquals(existingTargetUri.toString(), imported.uri)

        RingtoneCoordinator(context).addTracks(listOf(imported))

        assertTrue(RingtoneCoordinator(context).pool().any { it.uri == existingTargetUri.toString() })
        assertEquals(1, countTargetFiles(fileName))
    }

    private fun createTargetFile(fileName: String): Uri {
        val uri = requireNotNull(resolver.insert(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.TITLE, "existing-import")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Ringtones/Zufalls-Klingelton")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            },
        ))
        resolver.openOutputStream(uri).use { output -> requireNotNull(output).write(WavFixture.bytes()) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun createDownloadFile(fileName: String): Uri {
        val uri = requireNotNull(resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Downloads.IS_PENDING, 1)
            },
        ))
        resolver.openOutputStream(uri).use { output -> requireNotNull(output).write(WavFixture.bytes()) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun countTargetFiles(fileName: String): Int = resolver.query(
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        arrayOf(MediaStore.Audio.Media._ID),
        "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?",
        arrayOf(fileName, "Ringtones/Zufalls-Klingelton"),
        null,
    )?.use { cursor -> cursor.count } ?: 0
}
