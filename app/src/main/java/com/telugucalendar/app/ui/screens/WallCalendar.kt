package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.DayData
import com.telugucalendar.app.data.Festival
import com.telugucalendar.app.domain.CalendarLogic
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.Names
import com.telugucalendar.app.domain.Str
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.components.MoonMark
import com.telugucalendar.app.ui.components.MonthPager
import com.telugucalendar.app.ui.theme.LocalCalendarColors
import java.time.LocalDate
import java.time.YearMonth

/** The traditional wall-calendar page: big header with the Telugu year and months, a boxed grid with the tithi under each date, and the month's festivals below. */
@Composable
fun WallCalendarScreen(
    app: AppData,
    lang: Lang,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    MonthPager(app.months, month, onMonth, Modifier.fillMaxSize()) { ym ->
        WallPage(app, ym, lang, onDay)
    }
}

@Composable
private fun WallPage(app: AppData, ym: YearMonth, lang: Lang, onDay: (LocalDate) -> Unit) {
    val days = app.daysOf(ym)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = RoundedCornerShape(14.dp), shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                Rings()
                WallHeader(app, ym, days, lang)
                if (days.isEmpty()) {
                    Text(T.noDataForMonth.of(lang), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    WallGrid(app, ym, lang, onDay)
                }
            }
        }
        if (days.isNotEmpty()) MonthFestivals(app, ym, lang, onDay)
    }
}

@Composable
private fun Rings() {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        repeat(2) {
            Box(
                Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

@Composable
private fun WallHeader(app: AppData, ym: YearMonth, days: List<DayData>, lang: Lang) {
    val c = LocalCalendarColors.current
    val first = ym.atDay(1)
    val samvatsara = app.samvatsara(first)
    Box(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(c.headerStart, c.headerEnd))).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Names.gregorianMonth(ym.monthValue, lang), color = c.onHeader, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(ym.year.toString(), color = c.onHeader, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            if (lang == Lang.TE) {
                Text(Names.gregorianMonth(ym.monthValue, Lang.EN), color = c.onHeader.copy(alpha = 0.85f), fontSize = 12.sp)
            }
            if (days.isNotEmpty()) {
                val mid = days[days.size / 2]
                samvatsara?.let {
                    Text(
                        Names.samvatsaraTitle(Str(it.te, it.en), lang),
                        color = c.onHeader, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    )
                }
                Text(
                    listOf(Names.ayana(mid.ay, lang), Names.ritu(mid.ms, lang)).joinToString(" · "),
                    color = c.onHeader.copy(alpha = 0.92f), fontSize = 13.sp, textAlign = TextAlign.Center,
                )
                Text(
                    CalendarLogic.lunarMonthsLabel(days, lang),
                    color = c.onHeader, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WallGrid(app: AppData, ym: YearMonth, lang: Lang, onDay: (LocalDate) -> Unit) {
    val red = LocalCalendarColors.current.holidayRed
    val today = LocalDate.now()
    Column {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                Text(
                    Names.weekdayShort(i, lang),
                    Modifier.weight(1f).padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (i == 0) red else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        CalendarLogic.monthGrid(ym).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val cellModifier = Modifier.weight(1f).height(74.dp)
                    if (date == null) Box(cellModifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
                    else WallCell(date, app.day(date), lang, date == today, { onDay(date) }, cellModifier)
                }
            }
        }
    }
}

@Composable
private fun WallCell(date: LocalDate, day: DayData?, lang: Lang, isToday: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val red = LocalCalendarColors.current.holidayRed
    val numberColor = if (day != null && CalendarLogic.isRed(day)) red else MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(date.dayOfMonth.toString(), color = numberColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (day != null) {
                if (CalendarLogic.hasMoonMark(day, "pournami")) MoonMark(full = true, size = 8)
                if (CalendarLogic.hasMoonMark(day, "amavasya")) MoonMark(full = false, size = 8)
            }
        }
        if (day != null) {
            val tithi = day.t.firstOrNull()
            if (tithi != null) {
                Text(
                    Names.tithi(tithi.i, lang),
                    fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val headline = CalendarLogic.headline(day)
            if (headline != null) {
                Text(
                    headline.name(lang == Lang.TE),
                    fontSize = 8.sp, lineHeight = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    color = if (headline.k == Festival.KIND_VRATAM) LocalCalendarColors.current.vratam else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** The list under the wall page: date and name of each festival / holiday / special day, then one line per recurring vratam. */
@Composable
private fun MonthFestivals(app: AppData, ym: YearMonth, lang: Lang, onDay: (LocalDate) -> Unit) {
    val all = app.festivalsIn(ym)
    val main = all.filter { it.second.k != Festival.KIND_VRATAM }
    // One line per kind of vratam ("ఏకాదశి: 8, 23"); named ekadashis are folded into one "Ekadashi" line.
    val vratams = all.filter { it.second.k == Festival.KIND_VRATAM }
        .groupBy({ vratamLabel(it.second, lang) }, { it.first.date.dayOfMonth })
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(T.festivalsThisMonth.of(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (main.isEmpty() && vratams.isEmpty()) {
                Text(T.noFestivals.of(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            main.forEach { (day, fest) ->
                Row(Modifier.fillMaxWidth().clickable { onDay(day.date) }, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        "%d %s".format(day.date.dayOfMonth, Names.weekdayShort(CalendarLogic.sundayIndex(day.date), lang)),
                        Modifier.padding(top = 1.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (fest.h == 1) LocalCalendarColors.current.holidayRed else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    )
                    Text(fest.name(lang == Lang.TE), Modifier.weight(1f), fontSize = 14.sp)
                }
            }
            if (vratams.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                vratams.forEach { (label, dayNumbers) ->
                    Text("$label: ${dayNumbers.joinToString(", ")}", fontSize = 13.sp, color = LocalCalendarColors.current.vratam)
                }
            }
        }
    }
}

/** Generic label for a recurring vratam. Named ekadashis ("Putrada Ekadashi") are shortened to the kind so the month line stays compact. */
private fun vratamLabel(fest: Festival, lang: Lang): String = when {
    fest.id.contains("ekadasi", ignoreCase = true) || fest.id.contains("ekadashi", ignoreCase = true) -> T.ekadashiLabel.of(lang)
    else -> fest.name(lang == Lang.TE)
}
