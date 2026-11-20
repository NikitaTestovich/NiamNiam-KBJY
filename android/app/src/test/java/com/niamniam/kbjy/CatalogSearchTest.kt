package com.niamniam.kbjy

import com.niamniam.kbjy.data.StarterCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSearchTest {
    @Test
    fun findsRussianWordsInAnyOrderAndIgnoresYoDifference() {
        assertTrue(StarterCatalog.matches("Рис белый варёный без жира", "вареный рис"))
        assertFalse(StarterCatalog.matches("Рис белый варёный без жира", "рис сухой"))
    }
}
