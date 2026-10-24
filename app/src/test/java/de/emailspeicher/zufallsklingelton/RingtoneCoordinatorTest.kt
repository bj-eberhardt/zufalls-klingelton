package de.emailspeicher.zufallsklingelton

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingtoneCoordinatorTest {
    @Test
    fun startupSwitchesWhenDailyDateChanged() {
        assertTrue(shouldSwitchOnStartup(true, true, true, true, SwitchMode.DAILY, "2026-01-01", "2026-01-02"))
    }

    @Test
    fun startupDoesNotSwitchWhenFeatureInactiveOrPoolEmpty() {
        assertFalse(shouldSwitchOnStartup(false, true, false, false, SwitchMode.DAILY, null, "2026-01-02"))
        assertFalse(shouldSwitchOnStartup(true, false, false, false, SwitchMode.DAILY, null, "2026-01-02"))
    }

    @Test
    fun startupSwitchesWhenSystemRingtoneIsNotFromPool() {
        assertTrue(shouldSwitchOnStartup(true, true, false, true, SwitchMode.AFTER_INCOMING_CALL, "2026-01-02", "2026-01-02"))
    }

    @Test
    fun emptyPoolDecisionIsCoveredByStartupGuard() {
        assertFalse(shouldSwitchOnStartup(true, false, false, false, SwitchMode.AFTER_INCOMING_CALL, null, "2026-01-02"))
    }

    @Test
    fun phoneReceiverFinishesOnlyAfterRingingThenIdle() {
        assertEquals(PhoneStateAction.REMEMBER_RINGING, phoneStateAction(false, PHONE_STATE_RINGING))
        assertEquals(PhoneStateAction.FINISH_INCOMING_CALL, phoneStateAction(true, PHONE_STATE_IDLE))
        assertEquals(PhoneStateAction.IGNORE, phoneStateAction(false, PHONE_STATE_IDLE))
    }

    @Test
    fun bootAndDateReceiversAcceptOnlyTheirOwnActions() {
        assertTrue(isBootCompletedAction(Intent.ACTION_BOOT_COMPLETED))
        assertFalse(isBootCompletedAction(Intent.ACTION_DATE_CHANGED))
        assertTrue(isDateChangedAction(Intent.ACTION_DATE_CHANGED))
        assertFalse(isDateChangedAction(Intent.ACTION_BOOT_COMPLETED))
    }
}
