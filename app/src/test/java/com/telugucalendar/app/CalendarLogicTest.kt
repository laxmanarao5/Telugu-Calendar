package com.telugucalendar.app

import com.telugucalendar.app.data.DayData
import com.telugucalendar.app.data.Festival
import com.telugucalendar.app.data.Slot
import com.telugucalendar.app.domain.CalendarLogic
import com.telugucalendar.app.domain.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarLogicTest {
    @Test fun sundayIndexCountsSundayAsZero() {
        assertEquals(0, CalendarLogic.sundayIndex(LocalDate.of(2026, 11, 1)))   // a Sunday
        assertEquals(1, CalendarLogic.sundayIndex(LocalDate.of(2026, 11, 2)))
        assertEquals(6, CalendarLogic.sundayIndex(LocalDate.of(2026, 11, 7)))   // a Saturday
    }

    @Test fun monthGridStartsOnTheRightWeekday() {
        // 1 Jan 2026 is a Thursday: four blanks, then the days, padded to whole weeks.
        val jan = CalendarLogic.monthGrid(YearMonth.of(2026, 1))
        assertEquals(35, jan.size)
        assertEquals(listOf(null, null, null, null), jan.take(4))
        assertEquals(LocalDate.of(2026, 1, 1), jan[4])
        assertEquals(LocalDate.of(2026, 1, 31), jan.last { it != null })
    }

    @Test fun monthGridHasNoBlanksWhenFebruaryFitsFourWeeks() {
        // Feb 2026 starts on a Sunday and has 28 days: exactly four rows.
        val feb = CalendarLogic.monthGrid(YearMonth.of(2026, 2))
        assertEquals(28, feb.size)
        assertTrue(feb.none { it == null })
    }

    @Test fun monthGridAlwaysWholeWeeks() {
        for (m in 1..12) assertEquals(0, CalendarLogic.monthGrid(YearMonth.of(2027, m)).size % 7)
    }

    @Test fun time12() {
        assertEquals("6:48 AM", CalendarLogic.time12("06:48"))
        assertEquals("12:00 PM", CalendarLogic.time12("12:00"))
        assertEquals("12:05 AM", CalendarLogic.time12("00:05"))
        assertEquals("5:56 PM", CalendarLogic.time12("17:56"))
        assertEquals("", CalendarLogic.time12(""))
    }

    @Test fun rangeNeedsTwoTimes() {
        assertEquals("3:00 PM – 4:30 PM", CalendarLogic.range12(listOf("15:00", "16:30")))
        assertEquals("", CalendarLogic.range12(emptyList()))
    }

    @Test fun endLabelShowsTheDateOnlyWhenItIsAnotherDay() {
        val day = LocalDate.of(2026, 1, 1)
        assertEquals("7:05 PM", CalendarLogic.endLabel(Slot(12, "2026-01-01T19:05"), day, Lang.EN))
        assertEquals("2 Jan 5:12 AM", CalendarLogic.endLabel(Slot(12, "2026-01-02T05:12"), day, Lang.EN))
        assertEquals("", CalendarLogic.endLabel(Slot(12, ""), day, Lang.EN))
    }

    @Test fun untilTextFollowsEachLanguagesWordOrder() {
        assertEquals("7:05 PM వరకు", CalendarLogic.untilText("7:05 PM", Lang.TE))
        assertEquals("until 7:05 PM", CalendarLogic.untilText("7:05 PM", Lang.EN))
        assertEquals("", CalendarLogic.untilText("", Lang.EN))
    }

    private fun day(d: String, ms: Int = 1, ad: Int = 0, vararg f: Festival) = DayData(d = d, ms = ms, ad = ad, f = f.toList())

    @Test fun headlinePrefersAFestivalOverAVratam() {
        val d = day("2026-03-19", 1, 0, Festival("ekadasi", "vratam", "ఏకాదశి", "Ekadashi"), Festival("ugadi", "festival", "ఉగాది", "Ugadi", 1))
        assertEquals("ugadi", CalendarLogic.headline(d)?.id)
        assertNull(CalendarLogic.headline(day("2026-03-20")))
    }

    @Test fun lunarMonthsLabelListsEachMonthOnce() {
        val days = listOf(day("2026-01-18", 10), day("2026-01-19", 10), day("2026-01-20", 11))
        assertEquals("పుష్యం – మాఘం", CalendarLogic.lunarMonthsLabel(days, Lang.TE))
        assertEquals("Pushya – Magha", CalendarLogic.lunarMonthsLabel(days, Lang.EN))
    }

    @Test fun adhikaMonthsAreLabelledAndKeptApart() {
        val days = listOf(day("2026-05-16", 3, 0), day("2026-05-17", 3, 1))
        assertEquals("Jyeshtha – Adhika Jyeshtha", CalendarLogic.lunarMonthsLabel(days, Lang.EN))
    }

    @Test fun sundaysAndHolidaysAreRed() {
        assertTrue(CalendarLogic.isRed(day("2026-11-01")))                                        // Sunday
        assertTrue(!CalendarLogic.isRed(day("2026-11-02")))                                       // plain Monday
        assertTrue(CalendarLogic.isRed(day("2026-11-03", 8, 0, Festival("x", "holiday", "", "X", 1))))   // holiday on a Tuesday
        assertTrue(!CalendarLogic.isRed(day("2026-11-04", 8, 0, Festival("x", "holiday", "", "X", 2)))) // optional holiday is not red
    }

    @Test fun dateTitleReadsWeekdayDayMonthYear() {
        assertEquals("ఆదివారం, 1 నవంబర్ 2026", CalendarLogic.dateTitle(LocalDate.of(2026, 11, 1), Lang.TE))
        assertEquals("Sunday, 1 November 2026", CalendarLogic.dateTitle(LocalDate.of(2026, 11, 1), Lang.EN))
    }
}
