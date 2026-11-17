package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.DayData
import com.telugucalendar.app.data.FestFilter
import com.telugucalendar.app.data.Festival
import com.telugucalendar.app.domain.CalendarLogic
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.Names
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.components.FestivalLine
import com.telugucalendar.app.ui.theme.LocalCalendarColors
import java.time.LocalDate

private sealed interface Row2 {
    data class Header(val month: Int, val year: Int) : Row2
    data class Item(val day: DayData, val fest: Festival) : Row2
}

/** All festivals of a year as a list grouped by month, with filters. Opens scrolled to the next upcoming one. */
@Composable
fun FestivalsScreen(app: AppData, lang: Lang, onDay: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    var year by remember { mutableStateOf(if (today.year in app.sortedYears) today.year else app.sortedYears.lastOrNull() ?: today.year) }
    var filter by remember { mutableStateOf(FestFilter.FESTIVAL) }

    val rows = remember(year, filter, app) {
        val out = ArrayList<Row2>()
        var lastMonth = -1
        app.festivalsInYear(year, filter).forEach { (day, fest) ->
            if (day.date.monthValue != lastMonth) {
                out.add(Row2.Header(day.date.monthValue, day.date.year))
                lastMonth = day.date.monthValue
            }
            out.add(Row2.Item(day, fest))
        }
        out
    }
    val listState = rememberLazyListState()
    LaunchedEffect(year, filter) {
        val upcoming = rows.indexOfFirst { it is Row2.Item && !it.day.date.isBefore(today) }
        if (upcoming > 0) {
            // Land on the month heading just above the next festival so the month name is visible.
            val headerAbove = (upcoming downTo 0).firstOrNull { rows[it] is Row2.Header } ?: upcoming
            listState.scrollToItem(headerAbove)
        } else {
            listState.scrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (app.sortedYears.size > 1) {
            LazyRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(app.sortedYears) { y -> FilterChip(selected = y == year, onClick = { year = y }, label = { Text(y.toString()) }) }
            }
        }
        LazyRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FestFilter.entries) { f ->
                FilterChip(selected = f == filter, onClick = { filter = f }, label = { Text(filterLabel(f, lang)) })
            }
        }
        if (rows.isEmpty()) {
            Text(T.nothingHere.of(lang), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            items(rows) { row ->
                when (row) {
                    is Row2.Header -> Text(
                        "${Names.gregorianMonth(row.month, lang)} ${row.year}",
                        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    )
                    is Row2.Item -> FestivalRow(row.day, row.fest, lang, isPast = row.day.date.isBefore(today)) { onDay(row.day.date) }
                }
            }
        }
    }
}

private fun filterLabel(f: FestFilter, lang: Lang) = when (f) {
    FestFilter.ALL -> T.filterAll.of(lang)
    FestFilter.FESTIVAL -> T.filterFestivals.of(lang)
    FestFilter.HOLIDAY -> T.filterHolidays.of(lang)
    FestFilter.VRATAM -> T.filterVratams.of(lang)
    FestFilter.OTHER -> T.filterOther.of(lang)
}

@Composable
private fun FestivalRow(day: DayData, fest: Festival, lang: Lang, isPast: Boolean, onClick: () -> Unit) {
    val red = LocalCalendarColors.current.holidayRed
    val date = day.date
    Column(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    date.dayOfMonth.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    color = if (fest.h == 1 || CalendarLogic.sundayIndex(date) == 0) red else MaterialTheme.colorScheme.onSurface,
                )
                Text(Names.weekdayShort(CalendarLogic.sundayIndex(date), lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FestivalLine(fest, lang, Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
