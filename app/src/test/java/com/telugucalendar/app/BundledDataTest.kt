package com.telugucalendar.app

import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.YearData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

// Checks the real bundled year files in app/src/main/assets/calendar, which the tests read straight from the source tree.
class BundledDataTest {
    private val files = File("src/main/assets/calendar").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
    private val years = files.map { YearData.parse(it.readText()) }

    @Test fun thereIsAtLeast2026() {
        assertTrue("no calendar files found in src/main/assets/calendar", files.isNotEmpty())
        assertTrue(years.any { it.year == 2026 })
    }

    @Test fun fileNamesMatchTheirYear() {
        files.zip(years).forEach { (f, y) -> assertEquals(f.name, "${y.year}.json") }
    }

    @Test fun everyYearHasEveryDayInOrder() {
        for (y in years) {
            val first = LocalDate.of(y.year, 1, 1)
            assertEquals("days in ${y.year}", first.lengthOfYear(), y.days.size)
            y.days.forEachIndexed { i, d -> assertEquals(first.plusDays(i.toLong()), d.date) }
        }
    }

    @Test fun everyDayHasThePanchangamBasics() {
        for (y in years) for (d in y.days) {
            val at = "${d.d}"
            assertTrue("sunrise $at", d.sr.matches(Regex("\\d\\d:\\d\\d")))
            assertTrue("sunset $at", d.ss.matches(Regex("\\d\\d:\\d\\d")))
            assertTrue("tithi $at", d.t.isNotEmpty() && d.t.all { it.i in 1..30 })
            assertTrue("nakshatra $at", d.n.isNotEmpty() && d.n.all { it.i in 1..27 })
            assertTrue("yoga $at", d.y.isNotEmpty() && d.y.all { it.i in 1..27 })
            assertTrue("karana $at", d.k.isNotEmpty() && d.k.all { it.i in 1..11 })
            assertTrue("month $at", d.ms in 1..12)
            assertTrue("ayana $at", d.ay == 1 || d.ay == 2)
            assertEquals("rahu $at", 2, d.rk.size)
            assertEquals("yama $at", 2, d.ym.size)
            assertEquals("gulika $at", 2, d.gk.size)
            // Abhijit is left out on Wednesdays by tradition; otherwise it has a start and an end.
            assertTrue("abhijit $at", d.ab.isEmpty() || d.ab.size == 2)
            (d.t + d.n + d.y + d.k).forEach { slot -> assertNotNull("end time $at", runCatching { LocalDateTime.parse(slot.e) }.getOrNull()) }
        }
    }

    @Test fun tithiRunsOnWithoutGapsFromDayToDay() {
        for (y in years) y.days.zipWithNext().forEach { (a, b) ->
            val last = a.t.last().i
            val next = b.t.first().i
            // The tithi at the next sunrise is the one the day ended on, or a following one (a tithi can be skipped between two sunrises).
            val step = (next - last + 30) % 30
            assertTrue("tithi ${a.d} -> ${b.d}: $last then $next", step in 0..1)
        }
    }

    @Test fun festivalsHaveNamesAndSaneKinds() {
        val kinds = setOf("festival", "vratam", "holiday", "sankranti", "eclipse", "other")
        for (y in years) for (d in y.days) for (f in d.f) {
            assertTrue("${d.d} ${f.id}", f.id.isNotBlank() && f.k in kinds && f.te.isNotBlank() && f.en.isNotBlank() && f.h in 0..2)
        }
    }

    @Test fun twentyTwentySixKeyFestivalsAreOnTheirPublishedDates() {
        val y = years.first { it.year == 2026 }
        fun dateOf(id: String) = y.days.filter { d -> d.f.any { it.id == id } }.map { it.d }
        assertEquals(listOf("2026-03-19"), dateOf("ugadi"))
        assertEquals(listOf("2026-03-27"), dateOf("sri-rama-navami"))
        assertEquals(listOf("2026-09-14"), dateOf("vinayaka-chavithi"))
        assertEquals(listOf("2026-10-20"), dateOf("vijayadasami"))
        assertEquals(listOf("2026-11-08"), dateOf("deepavali"))
        assertEquals(listOf("2026-02-15"), dateOf("maha-shivaratri"))
        assertEquals(listOf("2026-01-14"), dateOf("makara-sankranti"))
    }

    @Test fun ugadiStartsTheNewTeluguYear() {
        val y = years.first { it.year == 2026 }
        assertEquals("Parabhava", y.samvatsara.last().en)
        assertEquals("2026-03-19", y.samvatsara.last().from)
    }

    @Test fun appDataLoadsEveryBundledYear() {
        val app = AppData(years.associateBy { it.year })
        assertEquals(years.size * 12, app.months.size)
    }
}
