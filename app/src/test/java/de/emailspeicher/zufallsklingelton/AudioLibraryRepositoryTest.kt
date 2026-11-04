package de.emailspeicher.zufallsklingelton

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioLibraryRepositoryTest {
    @Test
    fun keepsMp3ExtensionFromSelectedFileName() {
        assertEquals("mp3", audioExtension("Darth-Vader.mp3", "audio/mpeg"))
    }

    @Test
    fun mapsMpegMimeTypeToMp3WhenFileNameHasNoExtension() {
        assertEquals("mp3", audioExtension("Darth-Vader", "audio/mpeg"))
    }
}
