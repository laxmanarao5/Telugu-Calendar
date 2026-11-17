package com.telugucalendar.app.data

import android.content.res.AssetManager

/** Reads every `assets/calendar/<year>.json` file (made by tools/panchangam). Adding a year is just adding its file. */
class CalendarRepository(private val assets: AssetManager) {
    fun load(): AppData {
        val names = assets.list("calendar").orEmpty().filter { it.endsWith(".json") }.sorted()
        require(names.isNotEmpty()) { "No calendar data is bundled with the app." }
        val years = names.map { name ->
            val text = assets.open("calendar/$name").bufferedReader().use { it.readText() }
            YearData.parse(text)
        }
        return AppData(years.associateBy { it.year })
    }
}
