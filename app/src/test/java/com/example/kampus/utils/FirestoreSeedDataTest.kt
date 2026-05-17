package com.example.kampus.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreSeedDataTest {

    @Test
    fun deriveDisplayName_prefersAuthenticatedDisplayName() {
        val result = FirestoreSeedData.deriveDisplayName("Real Name", "reth.chanrith.2823@rupp.edu.kh")

        assertEquals("Real Name", result)
    }

    @Test
    fun deriveDisplayName_buildsNameFromEmailWhenAuthNameMissing() {
        val result = FirestoreSeedData.deriveDisplayName(null, "reth.chanrith.2823@rupp.edu.kh")

        assertEquals("Reth Chanrith", result)
    }

    @Test
    fun deriveDisplayName_fallsBackToUserWhenEmailPrefixNotUseful() {
        val result = FirestoreSeedData.deriveDisplayName(null, "12345@example.com")

        assertEquals("User", result)
    }

    @Test
    fun createHandle_normalizesAndCompactsDisplayName() {
        val result = FirestoreSeedData.createHandle("  Reth Chanrith  ")

        assertEquals("@rethchanrith", result)
    }

    @Test
    fun createHandle_fallsBackToUserWhenNameBecomesEmpty() {
        val result = FirestoreSeedData.createHandle("!!!")

        assertEquals("@user", result)
    }
}
