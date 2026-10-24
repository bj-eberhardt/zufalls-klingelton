package de.emailspeicher.zufallsklingelton

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSelectionTest {
    private val first = Track("1", "content://one", "One", "Artist", 1_000, 0)
    private val second = Track("2", "content://two", "Two", "Artist", 2_000, 1)

    @Test
    fun mergeTracks_ignoresDuplicateUrisAndAppendsNewTracks() {
        val duplicate = second.copy(id = "duplicate")
        val third = Track("3", "content://three", "Three", "Artist", 3_000, 0)
        val merged = mergeTracks(listOf(first, second), listOf(duplicate, third))

        assertEquals(listOf(first, second, third.copy(order = 2)), merged)
    }

    @Test
    fun chooseNextTrack_excludesCurrentWhenAnotherTrackExists() {
        assertEquals(second, chooseNextTrack(listOf(first, second), first.id, Random(1)))
        assertNotEquals(first.id, chooseNextTrack(listOf(first, second), first.id, Random(4))?.id)
        assertEquals(first, chooseNextTrack(listOf(first), first.id, Random(1)))
        assertNull(chooseNextTrack(emptyList(), null, Random(1)))
    }

    @Test
    fun dailySwitch_onlyHappensWhenDateChanges() {
        assertTrue(shouldSwitchForDate(null, "2026-01-01"))
        assertTrue(shouldSwitchForDate("2025-12-31", "2026-01-01"))
        assertEquals(false, shouldSwitchForDate("2026-01-01", "2026-01-01"))
    }
}
