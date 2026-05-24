package com.example.alphacinema.ui.app

import com.example.alphacinema.data.model.SubscriptionPlan
import com.example.alphacinema.data.model.resolvePlanEntitlements
import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class AdFreePlanTest {
    private val now = Date(1_000_000L)
    private val future = Timestamp(Date(2_000_000L))
    private val past = Timestamp(Date(500_000L))

    @Test
    fun freePlanHasNoPaidEntitlements() {
        val entitlements = resolvePlanEntitlements("free", "inactive", null, now)

        assertEquals(SubscriptionPlan.FREE, entitlements.plan)
        assertFalse(entitlements.adFree)
        assertFalse(entitlements.kidsMode)
        assertFalse(entitlements.playlist)
        assertFalse(entitlements.canCreateWatchParty)
        assertEquals(0, entitlements.maxWatchPartyMembers)
    }

    @Test
    fun basicPlanUnlocksAdFreeKidsModeAndPlaylistOnly() {
        val entitlements = resolvePlanEntitlements("basic", "active", future, now)

        assertEquals(SubscriptionPlan.BASIC, entitlements.plan)
        assertTrue(entitlements.adFree)
        assertTrue(entitlements.kidsMode)
        assertTrue(entitlements.playlist)
        assertFalse(entitlements.canCreateWatchParty)
        assertEquals(0, entitlements.maxWatchPartyMembers)
    }

    @Test
    fun couplePlanUnlocksTwoPersonWatchParty() {
        val entitlements = resolvePlanEntitlements("couple", "active", future, now)

        assertEquals(SubscriptionPlan.COUPLE, entitlements.plan)
        assertTrue(entitlements.adFree)
        assertTrue(entitlements.kidsMode)
        assertTrue(entitlements.playlist)
        assertTrue(entitlements.canCreateWatchParty)
        assertEquals(2, entitlements.maxWatchPartyMembers)
    }

    @Test
    fun premiumPlanUnlocksTenPersonWatchParty() {
        val entitlements = resolvePlanEntitlements(" Premium ", "active", future, now)

        assertEquals(SubscriptionPlan.PREMIUM, entitlements.plan)
        assertTrue(entitlements.canCreateWatchParty)
        assertEquals(10, entitlements.maxWatchPartyMembers)
    }

    @Test
    fun expiredOrInactivePaidPlanFallsBackToFree() {
        val expired = resolvePlanEntitlements("premium", "active", past, now)
        val inactive = resolvePlanEntitlements("premium", "inactive", future, now)
        val missingExpiry = resolvePlanEntitlements("premium", "active", null, now)

        assertEquals(SubscriptionPlan.FREE, expired.plan)
        assertEquals(SubscriptionPlan.FREE, inactive.plan)
        assertEquals(SubscriptionPlan.FREE, missingExpiry.plan)
        assertFalse(expired.adFree)
        assertFalse(inactive.playlist)
        assertFalse(missingExpiry.canCreateWatchParty)
    }
}
