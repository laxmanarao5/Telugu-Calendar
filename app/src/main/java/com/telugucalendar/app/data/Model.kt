package com.telugucalendar.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth

/** One tithi / nakshatra / yoga / karana in force on a day: [i] is its number, [e] the local time it ends (yyyy-MM-ddTHH:mm). See DATA_FORMAT.md. */
@Serializable
data class Slot(val i: Int, val e: String = "")

/** A festival, vratam, holiday, sankranti or eclipse on a day. */
@Serializable
data class Festival(
    val id: String,
    val k: String = KIND_FESTIVAL,
    val te: String = "",
    val en: String = "",
    /** 1 = general holiday, 2 = optional holiday, 0 = none. */
    val h: Int = 0,
    val ti: String = "",
    val ei: String = "",
) {
    fun name(telugu: Boolean) = (if (telugu) te else en).ifBlank { te.ifBlank { en } }
    fun info(telugu: Boolean) = (if (telugu) ti else ei).ifBlank { if (telugu) ei else ti }

    companion object {
        const val KIND_FESTIVAL = "festival"
        const val KIND_VRATAM = "vratam"
        const val KIND_HOLIDAY = "holiday"
        const val KIND_SANKRANTI = "sankranti"
        const val KIND_ECLIPSE = "eclipse"
        const val KIND_OTHER = "other"
    }
}

@Serializable
data class DayData(
    val d: String,
    val sr: String = "",
    val ss: String = "",
    /** Telugu (amanta) lunar month 1 Chaitra .. 12 Phalguna. */
    val ms: Int = 1,
    /** 1 when the month is an Adhika (extra) month. */
    val ad: Int = 0,
    /** 1 Uttarayana, 2 Dakshinayana. */
    val ay: Int = 1,
    val t: List<Slot> = emptyList(),
    val n: List<Slot> = emptyList(),
    val y: List<Slot> = emptyList(),
    val k: List<Slot> = emptyList(),
    val rk: List<String> = emptyList(),
    val ym: List<String> = emptyList(),
    val gk: List<String> = emptyList(),
    val ab: List<String> = emptyList(),
    val f: List<Festival> = emptyList(),
) {
    @Transient val date: LocalDate = LocalDate.parse(d)

    /** A general public holiday (shown in red like a Sunday). */
    @Transient val isHoliday: Boolean = f.any { it.h == 1 }

    val isAdhika get() = ad == 1
}

@Serializable
data class Samvatsara(val from: String, val te: String, val en: String)

@Serializable
data class YearData(
    val version: Int = 1,
    val year: Int,
    /** 1..12 for a month file (the days of that month only); 0 for a whole-year file. */
    val month: Int = 0,
    val samvatsara: List<Samvatsara> = emptyList(),
    val days: List<DayData> = emptyList(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        fun parse(text: String): YearData = json.decodeFromString(serializer(), text)
    }
}

enum class FestFilter { ALL, FESTIVAL, HOLIDAY, VRATAM, OTHER }

fun Festival.matches(filter: FestFilter) = when (filter) {
    FestFilter.ALL -> true
    FestFilter.FESTIVAL -> k == Festival.KIND_FESTIVAL
    FestFilter.HOLIDAY -> h > 0 || k == Festival.KIND_HOLIDAY
    FestFilter.VRATAM -> k == Festival.KIND_VRATAM
    FestFilter.OTHER -> k == Festival.KIND_SANKRANTI || k == Festival.KIND_ECLIPSE || k == Festival.KIND_OTHER
}

/**
 * Everything the app knows, indexed by date. The data comes as one chunk per month (`assets/calendar/<year>-<MM>.json`).
 * [knownMonths] lists every month that has a file, so the pager is complete even while only the first chunk is loaded;
 * [complete] is true once every chunk has been read.
 */
class AppData(
    val chunks: List<YearData>,
    knownMonths: Collection<YearMonth> = emptyList(),
    val complete: Boolean = true,
) {
    private val days: List<DayData> = chunks.flatMap { it.days }.sortedBy { it.date }
    private val byDate: Map<LocalDate, DayData> = days.associateBy { it.date }
    private val loadedMonths: Set<YearMonth> = days.mapTo(HashSet()) { YearMonth.from(it.date) }

    /** Every month of every bundled year, in order: the pages of the month pager. */
    val months: List<YearMonth> = (knownMonths + days.map { YearMonth.from(it.date) }).distinct().sorted()
    val sortedYears: List<Int> = months.map { it.year }.distinct()

    /** This data plus [more] chunks, now complete. */
    fun withMore(more: List<YearData>) = AppData(chunks + more, months, complete = true)

    fun day(date: LocalDate): DayData? = byDate[date]

    /** True when the data of [ym] has been read (it may still be loading in the background). */
    fun isLoaded(ym: YearMonth) = ym in loadedMonths

    fun daysOf(ym: YearMonth): List<DayData> = (1..ym.lengthOfMonth()).mapNotNull { byDate[ym.atDay(it)] }

    /** The Telugu year (samvatsara) that applies on [date]. */
    fun samvatsara(date: LocalDate): Samvatsara? {
        val list = chunks.firstOrNull { it.year == date.year && it.samvatsara.isNotEmpty() }?.samvatsara ?: return null
        return list.lastOrNull { LocalDate.parse(it.from) <= date } ?: list.firstOrNull()
    }

    fun festivalsIn(ym: YearMonth, filter: FestFilter = FestFilter.ALL): List<Pair<DayData, Festival>> =
        daysOf(ym).flatMap { day -> day.f.filter { it.matches(filter) }.map { day to it } }

    fun festivalsInYear(year: Int, filter: FestFilter = FestFilter.ALL): List<Pair<DayData, Festival>> =
        days.filter { it.date.year == year }.flatMap { day -> day.f.filter { it.matches(filter) }.map { day to it } }

    /** The nearest month that has data (the app opens on the current month, or on the closest one when that year is missing). */
    fun clamp(ym: YearMonth): YearMonth = when {
        months.isEmpty() -> ym
        ym < months.first() -> months.first()
        ym > months.last() -> months.last()
        else -> ym
    }
}
