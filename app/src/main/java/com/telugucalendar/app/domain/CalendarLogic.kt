package com.telugucalendar.app.domain

import com.telugucalendar.app.data.DayData
import com.telugucalendar.app.data.Festival
import com.telugucalendar.app.data.Slot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Pure calendar rules (no Android), so they can be unit tested. */
object CalendarLogic {

    /** 0 = Sunday .. 6 = Saturday (java.time counts Monday as 1 and Sunday as 7). */
    fun sundayIndex(date: LocalDate) = date.dayOfWeek.value % 7

    /** The month as Sunday-first weeks: leading and trailing blanks are null; the size is always a multiple of 7. */
    fun monthGrid(ym: YearMonth): List<LocalDate?> {
        val lead = sundayIndex(ym.atDay(1))
        val cells = ArrayList<LocalDate?>()
        repeat(lead) { cells.add(null) }
        for (d in 1..ym.lengthOfMonth()) cells.add(ym.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    /** The Telugu lunar months a Gregorian month touches, in order, e.g. Pushya then Magha. Each is (month 1..12, adhika). */
    fun lunarMonthsOf(days: List<DayData>): List<Pair<Int, Boolean>> =
        days.map { it.ms to it.isAdhika }.distinct()

    fun lunarMonthsLabel(days: List<DayData>, lang: Lang): String =
        lunarMonthsOf(days).joinToString(" – ") { Names.lunarMonth(it.first, it.second, lang) }

    private val priority = listOf(
        Festival.KIND_FESTIVAL, Festival.KIND_HOLIDAY, Festival.KIND_ECLIPSE, Festival.KIND_SANKRANTI, Festival.KIND_VRATAM, Festival.KIND_OTHER,
    )

    /** The one item to show in a small calendar cell: a festival first, then a holiday, eclipse, sankranti, vratam. */
    fun headline(day: DayData): Festival? =
        day.f.minWithOrNull(compareBy<Festival> { priority.indexOf(it.k).let { i -> if (i < 0) priority.size else i } })

    /** True for the days drawn in red: Sundays and general holidays. */
    fun isRed(day: DayData) = sundayIndex(day.date) == 0 || day.isHoliday

    fun hasMoonMark(day: DayData, id: String) = day.f.any { it.id == id }

    /** "7:05 PM" from "19:05". Blank for blank input. */
    fun time12(hhmm: String): String {
        val parts = hhmm.split(":")
        if (parts.size != 2) return hhmm
        val h = parts[0].toIntOrNull() ?: return hhmm
        val m = parts[1].toIntOrNull() ?: return hhmm
        val suffix = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, suffix)
    }

    fun range12(range: List<String>): String = if (range.size == 2) "${time12(range[0])} – ${time12(range[1])}" else ""

    /**
     * When a slot ends, e.g. "7:05 PM" for the same day or "3 Jan 5:12 AM" when it runs into another date.
     * [dayDate] is the day the slot is shown on.
     */
    fun endLabel(slot: Slot, dayDate: LocalDate, lang: Lang): String {
        if (slot.e.length < 16) return ""
        val end = runCatching { LocalDateTime.parse(slot.e) }.getOrNull() ?: return ""
        val time = time12("%02d:%02d".format(end.hour, end.minute))
        return if (end.toLocalDate() == dayDate) time else "${end.dayOfMonth} ${Names.gregorianMonthShort(end.monthValue, lang)} $time"
    }

    /** "ends at" in the language's word order: "7:05 PM వరకు" or "until 7:05 PM". */
    fun untilText(label: String, lang: Lang) = if (label.isBlank()) "" else if (lang == Lang.TE) "$label ${T.until.te}" else "${T.until.en} $label"

    /** "శనివారం, 17 నవంబర్ 2026". */
    fun dateTitle(date: LocalDate, lang: Lang) =
        "${Names.weekday(sundayIndex(date), lang)}, ${date.dayOfMonth} ${Names.gregorianMonth(date.monthValue, lang)} ${date.year}"

    /** The tithi in force at sunrise, as a name with its paksha, e.g. "శుక్ల పక్షం – ఏకాదశి". */
    fun tithiTitle(day: DayData, lang: Lang): String {
        val t = day.t.firstOrNull() ?: return ""
        return "${Names.paksha(t.i, lang)} – ${Names.tithi(t.i, lang)}"
    }
}
