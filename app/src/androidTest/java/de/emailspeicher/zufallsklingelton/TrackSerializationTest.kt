package de.emailspeicher.zufallsklingelton

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSerializationTest {
    @Test
    fun serializingTracks_preservesAllFieldsAndOrder() {
        val tracks = listOf(
            Track("1", "content://one", "One", "Artist", 1_000, 0),
            Track("2", "content://two", "Two", "Artist", 2_000, 1),
        )
        assertEquals(tracks, TrackRepository.decode(TrackRepository.encode(tracks)))
    }
}
