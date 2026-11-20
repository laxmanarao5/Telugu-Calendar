package com.telugucalendar.app.data

import android.content.res.AssetManager
import java.time.YearMonth

/**
 * Reads the month files `assets/calendar/<year>-<MM>.json` (made by tools/panchangam). Adding a year is just adding its twelve files.
 * The app reads only what it needs: [first] the month shown at start, [window] the rest of this year and next year, and [load] any other months on demand.
 */
class CalendarRepository(private val assets: AssetManager) {
    private val files: Map<YearMonth, String> = (assets.list("calendar").orEmpty())
        .mapNotNull { name -> parseName(name)?.let { it to name } }
        .sortedBy { it.first }
        .toMap()

    /** Only the month nearest to [want] (it is [want] itself when that month has a file). The pager already knows every month. */
    fun first(want: YearMonth): AppData {
        require(files.isNotEmpty()) { "No calendar data is bundled with the app." }
        val chosen = files.keys.lastOrNull { it <= want } ?: files.keys.first()
        return AppData(load(listOf(chosen)), files.keys)
    }

    /** The months of [centre]'s year and of the year after it, the ones closest to [centre] first. */
    fun window(centre: YearMonth): List<YearMonth> =
        files.keys.filter { it.year == centre.year || it.year == centre.year + 1 }.sortedBy { distance(it, centre) }

    /** Reads the given months (those that have no file are skipped). */
    fun load(months: Collection<YearMonth>): List<YearData> =
        months.filter { it in files }.map { ym ->
            YearData.parse(assets.open("calendar/${files.getValue(ym)}").bufferedReader().use { it.readText() })
        }

    private fun distance(a: YearMonth, b: YearMonth) = kotlin.math.abs(a.year * 12 + a.monthValue - (b.year * 12 + b.monthValue))

    companion object {
        private val NAME = Regex("""(\d{4})-(\d{2})\.json""")
        fun parseName(name: String): YearMonth? {
            val m = NAME.matchEntire(name) ?: return null
            val month = m.groupValues[2].toInt()
            return if (month in 1..12) YearMonth.of(m.groupValues[1].toInt(), month) else null
        }
    }
}
