package com.example.alphacinema.data.model

import com.google.firebase.Timestamp
import java.util.Date

enum class SubscriptionPlan(val id: String) {
    FREE("free"),
    BASIC("basic"),
    COUPLE("couple"),
    PREMIUM("premium");

    companion object {
        fun from(rawPlan: String?): SubscriptionPlan {
            return when (rawPlan?.trim()?.lowercase()) {
                BASIC.id -> BASIC
                COUPLE.id -> COUPLE
                PREMIUM.id -> PREMIUM
                else -> FREE
            }
        }
    }
}

data class PlanEntitlements(
    val plan: SubscriptionPlan,
    val adFree: Boolean,
    val kidsMode: Boolean,
    val playlist: Boolean,
    val canCreateWatchParty: Boolean,
    val maxWatchPartyMembers: Int
)

fun SubscriptionPlan.entitlements(): PlanEntitlements {
    return when (this) {
        SubscriptionPlan.FREE -> PlanEntitlements(
            plan = this,
            adFree = false,
            kidsMode = false,
            playlist = false,
            canCreateWatchParty = false,
            maxWatchPartyMembers = 0
        )
        SubscriptionPlan.BASIC -> PlanEntitlements(
            plan = this,
            adFree = true,
            kidsMode = true,
            playlist = true,
            canCreateWatchParty = false,
            maxWatchPartyMembers = 0
        )
        SubscriptionPlan.COUPLE -> PlanEntitlements(
            plan = this,
            adFree = true,
            kidsMode = true,
            playlist = true,
            canCreateWatchParty = true,
            maxWatchPartyMembers = 2
        )
        SubscriptionPlan.PREMIUM -> PlanEntitlements(
            plan = this,
            adFree = true,
            kidsMode = true,
            playlist = true,
            canCreateWatchParty = true,
            maxWatchPartyMembers = 10
        )
    }
}

fun resolvePlanEntitlements(
    plan: String?,
    status: String?,
    expiresAt: Timestamp?,
    now: Date = Date()
): PlanEntitlements {
    val normalizedPlan = SubscriptionPlan.from(plan)
    if (normalizedPlan == SubscriptionPlan.FREE) {
        return SubscriptionPlan.FREE.entitlements()
    }

    val isActive = status?.trim()?.lowercase() == "active"
    val isUnexpired = expiresAt?.toDate()?.after(now) == true
    return if (isActive && isUnexpired) {
        normalizedPlan.entitlements()
    } else {
        SubscriptionPlan.FREE.entitlements()
    }
}

fun UserProfile?.activeEntitlements(now: Date = Date()): PlanEntitlements {
    return resolvePlanEntitlements(
        plan = this?.subscriptionPlan,
        status = this?.subscriptionStatus,
        expiresAt = this?.subscriptionExpiresAt,
        now = now
    )
}
