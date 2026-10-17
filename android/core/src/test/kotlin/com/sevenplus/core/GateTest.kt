package com.sevenplus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GateTest {
    @Test fun lengths() {
        assertTrue(Gate.canUseLength(5, false))
        assertTrue(Gate.canUseLength(7, false))
        assertFalse(Gate.canUseLength(10, false))
        assertTrue(Gate.canUseLength(20, true))
    }

    @Test fun customWorkoutLimit() {
        assertTrue(Gate.canCreateCustom(2, false))
        assertFalse(Gate.canCreateCustom(3, false))
        assertTrue(Gate.canCreateCustom(50, true))
    }

    @Test fun upsellOnlyOnceAfterThirdWorkoutAndNeverForPremium() {
        assertFalse(Gate.shouldUpsell(1, false, false))
        assertFalse(Gate.shouldUpsell(2, false, false))
        assertTrue(Gate.shouldUpsell(3, false, false))
        assertFalse(Gate.shouldUpsell(3, false, true))
        assertFalse(Gate.shouldUpsell(3, true, false))
        assertFalse(Gate.shouldUpsell(4, false, false))
    }
}
