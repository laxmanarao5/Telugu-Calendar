package com.telugucalendar.app

import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.CalendarRepository
import com.telugucalendar.app.data.FestFilter
import com.telugucalendar.app.data.YearData
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.Names
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class NamesAndModelTest {
    @Test fun tithiNamesCoverBothPakshas() {
        assertEquals("Prathama", Names.tithi(1, Lang.EN))
        assertEquals("Pournami", Names.tithi(15, Lang.EN))
        assertEquals("Prathama", Names.tithi(16, Lang.EN))
        assertEquals("Amavasya", Names.tithi(30, Lang.EN))
        assertEquals("ఏకాదశి", Names.tithi(11, Lang.TE))
        assertEquals("ఏకాదశి", Names.tithi(26, Lang.TE))
        assertEquals("శుక్ల పక్షం", Names.paksha(15, Lang.TE))
        assertEquals("బహుళ పక్షం", Names.paksha(16, Lang.TE))
    }

    @Test fun everyNumberHasAName() {
        for (i in 1..30) assertTrue(Names.tithi(i, Lang.TE).isNotBlank() && Names.tithi(i, Lang.EN).isNotBlank())
        for (i in 1..27) {
            assertTrue(Names.nakshatra(i, Lang.TE).isNotBlank() && Names.nakshatra(i, Lang.EN).isNotBlank())
            assertTrue(Names.yoga(i, Lang.TE).isNotBlank() && Names.yoga(i, Lang.EN).isNotBlank())
        }
        for (i in 1..11) assertTrue(Names.karana(i, Lang.TE).isNotBlank() && Names.karana(i, Lang.EN).isNotBlank())
        for (m in 1..12) assertTrue(Names.lunarMonth(m, false, Lang.TE).isNotBlank() && Names.gregorianMonth(m, Lang.TE).isNotBlank())
        for (d in 0..6) assertTrue(Names.weekday(d, Lang.TE).isNotBlank() && Names.weekdayShort(d, Lang.EN).isNotBlank())
    }

    @Test fun nakshatraOrderStartsAtAshwiniAndEndsAtRevati() {
        assertEquals("Ashwini", Names.nakshatra(1, Lang.EN))
        assertEquals("Rohini", Names.nakshatra(4, Lang.EN))
        assertEquals("Revati", Names.nakshatra(27, Lang.EN))
    }

    @Test fun seasonsCoverTwoMonthsEach() {
        assertEquals(Names.ritu(1, Lang.EN), Names.ritu(2, Lang.EN))   // Chaitra, Vaisakha: Vasanta
        assertEquals(Names.ritu(11, Lang.EN), Names.ritu(12, Lang.EN)) // Magha, Phalguna: Shishira
        assertTrue(Names.ritu(2, Lang.EN) != Names.ritu(3, Lang.EN))
    }

    private val sample = """
        {"version":1,"year":2026,
         "samvatsara":[{"from":"2026-01-01","te":"విశ్వావసు","en":"Vishvavasu"},{"from":"2026-03-19","te":"పరాభవ","en":"Parabhava"}],
         "days":[
          {"d":"2026-03-18","ms":12,"t":[{"i":30,"e":"2026-03-19T06:52"}],"n":[{"i":27,"e":"2026-03-18T20:00"}]},
          {"d":"2026-03-19","ms":1,"t":[{"i":1,"e":"2026-03-20T04:52"}],"n":[{"i":1,"e":"2026-03-19T21:00"}],
           "f":[{"id":"ugadi","k":"festival","te":"ఉగాది","en":"Ugadi","h":1},{"id":"x","k":"vratam","te":"వ్రతం","en":"Vratam"}],"future_field":1}
         ]}
    """.trimIndent()

    private val allOf2026 = (1..12).map { YearMonth.of(2026, it) }

    @Test fun parsesTheDataFormatAndIgnoresUnknownFields() {
        val y = YearData.parse(sample)
        assertEquals(2026, y.year)
        assertEquals(2, y.days.size)
        assertEquals(LocalDate.of(2026, 3, 19), y.days[1].date)
        assertTrue(y.days[1].isHoliday)
        assertTrue(!y.days[0].isHoliday)
    }

    @Test fun appDataIndexesDaysAndPicksTheSamvatsaraByDate() {
        val app = AppData(listOf(YearData.parse(sample)), allOf2026)
        assertEquals(12, app.months.size)
        assertEquals("Vishvavasu", app.samvatsara(LocalDate.of(2026, 3, 18))?.en)
        assertEquals("Parabhava", app.samvatsara(LocalDate.of(2026, 3, 19))?.en)
        assertEquals(2, app.daysOf(YearMonth.of(2026, 3)).size)
    }

    @Test fun festivalFiltersSeparateKinds() {
        val app = AppData(listOf(YearData.parse(sample)), allOf2026)
        val march = YearMonth.of(2026, 3)
        assertEquals(2, app.festivalsIn(march).size)
        assertEquals(listOf("ugadi"), app.festivalsIn(march, FestFilter.FESTIVAL).map { it.second.id })
        assertEquals(listOf("x"), app.festivalsIn(march, FestFilter.VRATAM).map { it.second.id })
        assertEquals(listOf("ugadi"), app.festivalsIn(march, FestFilter.HOLIDAY).map { it.second.id })
    }

    @Test fun clampKeepsTheAppInsideTheBundledYears() {
        val app = AppData(listOf(YearData.parse(sample)), allOf2026)
        assertEquals(YearMonth.of(2026, 12), app.clamp(YearMonth.of(2028, 5)))
        assertEquals(YearMonth.of(2026, 1), app.clamp(YearMonth.of(2020, 5)))
        assertEquals(YearMonth.of(2026, 6), app.clamp(YearMonth.of(2026, 6)))
    }

    @Test fun aPartialLoadKnowsEveryMonthButOnlyHasTheFirstOnesDays() {
        val first = AppData(listOf(YearData.parse(sample)), allOf2026)
        assertEquals(12, first.months.size)
        assertTrue(first.isLoaded(YearMonth.of(2026, 3)))
        assertTrue(!first.isLoaded(YearMonth.of(2026, 4)))
        assertTrue(!first.complete)
        val rest = YearData.parse("""{"year":2026,"month":4,"days":[{"d":"2026-04-01","ms":1,"t":[{"i":13,"e":"2026-04-01T20:00"}],"n":[{"i":1,"e":"2026-04-01T20:00"}]}]}""")
        val all = first.withMore(listOf(rest))
        assertTrue(!all.complete)   // ten months still unread
        assertTrue(all.isLoaded(YearMonth.of(2026, 4)))
        assertEquals(12, all.months.size)
        assertEquals("Parabhava", all.samvatsara(LocalDate.of(2026, 4, 1))?.en)   // the samvatsara list comes from whichever chunk has it
    }

    @Test fun monthFileNamesAreRecognised() {
        assertEquals(YearMonth.of(2026, 11), CalendarRepository.parseName("2026-11.json"))
        assertEquals(null, CalendarRepository.parseName("2026.json"))
        assertEquals(null, CalendarRepository.parseName("2026-13.json"))
        assertEquals(null, CalendarRepository.parseName("notes.txt"))
    }
}
