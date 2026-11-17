/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

/**
 * Completed states of the first-launch flow. A missing preference means the
 * user has not finished onboarding yet, so [PENDING] is never persisted.
 */
enum class OnboardingStatus(val value: String) {
    PENDING("pending"),
    GUEST("guest"),
    AUTHENTICATED("authenticated"),
    ;

    companion object {
        fun fromStorage(value: String?): OnboardingStatus? =
            entries.firstOrNull { it != PENDING && it.value == value }
    }
}

/**
 * Resolves the persisted onboarding result, with two fallbacks for installs
 * created before onboarding state existed:
 * - an InnerTube session means the account was already connected;
 * - the legacy intro flag means the old title card had been completed.
 */
fun resolveOnboardingStatus(
    storedValue: String?,
    legacyIntroCompleted: Boolean,
    hasAccountSession: Boolean,
): OnboardingStatus {
    OnboardingStatus.fromStorage(storedValue)?.let { return it }
    return when {
        hasAccountSession -> OnboardingStatus.AUTHENTICATED
        legacyIntroCompleted -> OnboardingStatus.GUEST
        else -> OnboardingStatus.PENDING
    }
}
