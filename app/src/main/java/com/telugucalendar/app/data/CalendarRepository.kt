package com.telugucalendar.app.data

import android.content.res.AssetManager
import java.time.YearMonth

/**
 * Reads the month files `assets/calendar/<year>-<MM>.json` (made by tools/panchangam). Adding a year is just adding its twelve files.
 * [first] reads only the month the screen needs, [rest] the others, so the app can draw before everything is parsed.
 */
class CalendarRepository(private val assets: AssetManager) {
    private val files: Map<YearMonth, String> = (assets.list("calendar").orEmpty())
        .mapNotNull { name -> parseName(name)?.let { it to name } }
        .sortedBy { it.first }
        .toMap()

    /** Only the month nearest to [want] (it is [want] itself when that month has a file). */
    fun first(want: YearMonth): AppData {
        require(files.isNotEmpty()) { "No calendar data is bundled with the app." }
        val chosen = files.keys.lastOrNull { it <= want } ?: files.keys.first()
        return AppData(listOf(read(chosen)), files.keys, complete = false)
    }

    /** Every other month, the ones closest to [first]'s month read first. */
    fun rest(first: AppData): AppData {
        val have = first.chunks.map { YearMonth.of(it.year, it.month) }.toSet()
        val centre = have.first()
        val more = files.keys.filter { it !in have }
            .sortedBy { kotlin.math.abs(it.year * 12 + it.monthValue - (centre.year * 12 + centre.monthValue)) }
            .map { read(it) }
        return first.withMore(more)
    }

    private fun read(ym: YearMonth): YearData =
        YearData.parse(assets.open("calendar/${files.getValue(ym)}").bufferedReader().use { it.readText() })

    companion object {
        private val NAME = Regex("""(\d{4})-(\d{2})\.json""")
        fun parseName(name: String): YearMonth? {
            val m = NAME.matchEntire(name) ?: return null
            val month = m.groupValues[2].toInt()
            return if (month in 1..12) YearMonth.of(m.groupValues[1].toInt(), month) else null
        }
    }
}
