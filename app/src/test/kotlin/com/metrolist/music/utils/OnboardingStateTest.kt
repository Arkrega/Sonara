/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStateTest {
    @Test
    fun `fresh install is pending`() {
        assertEquals(
            OnboardingStatus.PENDING,
            resolveOnboardingStatus(storedValue = null, legacyIntroCompleted = false, hasAccountSession = false),
        )
    }

    @Test
    fun `stored guest wins over legacy flag`() {
        assertEquals(
            OnboardingStatus.GUEST,
            resolveOnboardingStatus(
                storedValue = OnboardingStatus.GUEST.value,
                legacyIntroCompleted = false,
                hasAccountSession = false,
            ),
        )
    }

    @Test
    fun `stored authenticated survives a cleared account session`() {
        assertEquals(
            OnboardingStatus.AUTHENTICATED,
            resolveOnboardingStatus(
                storedValue = OnboardingStatus.AUTHENTICATED.value,
                legacyIntroCompleted = true,
                hasAccountSession = false,
            ),
        )
    }

    @Test
    fun `account session without stored state is authenticated`() {
        assertEquals(
            OnboardingStatus.AUTHENTICATED,
            resolveOnboardingStatus(storedValue = null, legacyIntroCompleted = false, hasAccountSession = true),
        )
    }

    @Test
    fun `legacy completed intro maps to guest`() {
        assertEquals(
            OnboardingStatus.GUEST,
            resolveOnboardingStatus(storedValue = null, legacyIntroCompleted = true, hasAccountSession = false),
        )
    }

    @Test
    fun `unknown stored value is ignored`() {
        assertEquals(
            OnboardingStatus.PENDING,
            resolveOnboardingStatus(storedValue = "nonsense", legacyIntroCompleted = false, hasAccountSession = false),
        )
    }
}
