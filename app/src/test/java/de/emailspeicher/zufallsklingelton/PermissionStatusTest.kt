package de.emailspeicher.zufallsklingelton

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {
    @Test
    fun allGranted_isTrueOnlyWhenEveryPermissionIsGranted() {
        assertTrue(PermissionStatus(true, true, true).allGranted)
        assertFalse(PermissionStatus(true, true, false).allGranted)
        assertFalse(PermissionStatus(false, true, true).allGranted)
        assertFalse(PermissionStatus(true, false, true).allGranted)
    }
}
