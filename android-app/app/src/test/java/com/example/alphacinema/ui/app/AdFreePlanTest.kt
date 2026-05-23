package com.example.alphacinema.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFreePlanTest {
    @Test
    fun paidPlansAreAdFree() {
        assertTrue(isAdFreePlan("basic"))
        assertTrue(isAdFreePlan("couple"))
        assertTrue(isAdFreePlan("premium"))
        assertTrue(isAdFreePlan(" Premium "))
    }

    @Test
    fun freeBlankAndUnknownPlansAreNotAdFree() {
        assertFalse(isAdFreePlan("free"))
        assertFalse(isAdFreePlan(""))
        assertFalse(isAdFreePlan(null))
        assertFalse(isAdFreePlan("trial"))
    }
}
