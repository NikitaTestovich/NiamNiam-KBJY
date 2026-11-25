package com.niamniam.kbjy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.niamniam.kbjy.data.RegionalSource
import com.niamniam.kbjy.data.StarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogAcceptanceTest {
    @Test
    fun baselineProductQueriesFindTheReviewedCardsInBundledCatalogs() {
        verifyReviewedCatalog("basic-products-ru.tsv", total = 50, minimum = 45)
    }

    @Test
    fun preparedDishSearchMeetsTheReleaseThreshold() {
        verifyReviewedCatalog("prepared-dishes-ru.tsv", total = 100, minimum = 90)
    }

    private fun verifyReviewedCatalog(asset: String, total: Int, minimum: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expected = InstrumentationRegistry.getInstrumentation().context.assets
            .open(asset).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.map { line ->
                    val (query, name) = line.split('\t', limit = 2)
                    query to name
                }.toList()
            }
        assertEquals(total, expected.size)
        assertEquals(total, expected.map { it.first }.distinct().size)
        val reviewed = expected.filter { (_, name) -> name != "-" }
        assertEquals(reviewed.size, reviewed.map { it.second }.distinct().size)

        val catalogNames = listOf(
            StarterCatalog.loadPackaged(context).map { it.food.name },
            StarterCatalog.load(context).map { it.name },
        ) + RegionalSource.entries.map { source -> StarterCatalog.loadRegional(context, source).map { it.food.name } }
        val misses = reviewed.filterNot { (query, expectedName) ->
            catalogNames.any { source ->
                expectedName in source.asSequence().filter { StarterCatalog.matches(it, query) }.take(100)
            }
        }
        val found = reviewed.size - misses.size
        assertTrue("Only $found/$total reviewed cards found in $asset. Misses: $misses", found >= minimum)
        println("Russian catalog verification ($asset): $found/$total")
    }
}
