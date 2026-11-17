package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import com.telugucalendar.app.ui.components.LoadingMonth
import com.telugucalendar.app.ui.components.MonthPager
import com.telugucalendar.app.ui.components.MoonMark
import com.telugucalendar.app.ui.components.WeekdayColors
import com.telugucalendar.app.ui.theme.LocalCalendarColors
import java.time.LocalDate
import java.time.YearMonth

// The wall page is a printed sheet: it keeps its own paper colours and dark ink in the light and the dark theme.
private val Paper = Color(0xFFFFFBEF)
private val PaperFestival = Color(0xFFFFE9A8)
private val PaperInfo = Color(0xFFFFF3C4)
private val PaperRahu = Color(0xFFE3F1DC)
private val Banner = Color(0xFF3B1F5C)
private val BannerGold = Color(0xFFFFD54F)
private val Ink = Color(0xFF2B1B00)
private val Line = Color(0xFFB9A98A)
private val LabelWidth = 46.dp
private val CellHeight = 68.dp

/**
 * The traditional printed wall calendar: a header with the Telugu year, the month banner and the Rahu Kalam table; then one row per weekday
 * and one column per week, each date with its tithi and nakshatra, festival days tinted, the festivals box in the blank corner, and the month's festival dates underneath.
 */
@Composable
fun WallCalendarScreen(
    app: AppData,
    lang: Lang,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    MonthPager(app.months, month, onMonth, Modifier.fillMaxSize()) { ym ->
        if (app.isLoaded(ym) || app.complete) WallPage(app, ym, lang, onDay) else LoadingMonth()
    }
}

@Composable
private fun WallPage(app: AppData, ym: YearMonth, lang: Lang, onDay: (LocalDate) -> Unit) {
    val days = app.daysOf(ym)
    val frame = LocalCalendarColors.current.headerStart
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = RoundedCornerShape(8.dp), shadowElevation = 4.dp, color = Paper, border = BorderStroke(2.dp, frame)) {
            Column {
                WallHeader(app, ym, days, lang)
                if (days.isEmpty()) {
                    Text(T.noDataForMonth.of(lang), Modifier.padding(24.dp), color = Ink)
                } else {
                    val main = app.festivalsIn(ym).filter { it.second.k != Festival.KIND_VRATAM }
                    WallGrid(app, ym, main, lang, onDay)
                    WallFooter(main, lang)
                }
            }
        }
        if (days.isNotEmpty()) MonthFestivals(app, ym, lang, onDay)
    }
}

@Composable
private fun WallHeader(app: AppData, ym: YearMonth, days: List<DayData>, lang: Lang) {
    val frame = LocalCalendarColors.current.headerStart
    val samvatsara = app.samvatsara(ym.atDay(1))
    val mid = days.getOrNull(days.size / 2)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().background(PaperInfo).border(1.dp, frame).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            samvatsara?.let { Text(Names.samvatsaraTitle(Str(it.te, it.en), lang), color = Ink, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold) }
            if (mid != null) {
                Text(listOf(Names.ayana(mid.ay, lang), Names.ritu(mid.ms, lang)).joinToString(" · "), color = Ink, fontSize = 10.sp, lineHeight = 12.sp)
                Text(CalendarLogic.lunarMonthsLabel(days, lang), color = Ink, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${T.shakaYear.of(lang)} ${CalendarLogic.shakaYear(mid)}", color = Ink, fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().background(Banner).padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(ym.year.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(Names.gregorianMonth(ym.monthValue, lang), color = BannerGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (lang == Lang.TE) Text(Names.gregorianMonth(ym.monthValue, Lang.EN).uppercase(), color = Color.White, fontSize = 10.sp)
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().background(PaperRahu).border(1.dp, frame).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        ) {
            Text(T.rahu.of(lang), color = Ink, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold)
            for (i in 0..6) {
                val range = days.firstOrNull { CalendarLogic.sundayIndex(it.date) == i }?.rk.orEmpty()
                Text("${Names.weekdayShort(i, lang)} ${CalendarLogic.rangeShort(range)}", color = Ink, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WallGrid(app: AppData, ym: YearMonth, main: List<Pair<DayData, Festival>>, lang: Lang, onDay: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val weeks = CalendarLogic.weeksOf(ym)
    val lead = weeks.first().takeWhile { it == null }.size
    val trail = weeks.last().reversed().takeWhile { it == null }.size
    // The festivals box goes into the bigger blank corner (before the 1st, or after the last day), when it has room for at least two rows.
    val boxAtStart = lead >= trail
    val room = maxOf(lead, trail)
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
        Column(Modifier.width(LabelWidth)) {
            for (i in 0..6) WeekdayLabel(i, lang)
        }
        weeks.forEachIndexed { w, week ->
            Column(Modifier.weight(1f)) {
                var i = 0
                while (i < 7) {
                    val date = week[i]
                    if (date != null) {
                        WallCell(date, app.day(date), lang, date == today) { onDay(date) }
                        i++
                    } else {
                        var j = i
                        while (j < 7 && week[j] == null) j++
                        val run = j - i
                        val boxHere = room >= 2 && ((boxAtStart && w == 0 && i == 0) || (!boxAtStart && w == weeks.lastIndex && j == 7))
                        if (boxHere) FestivalsBox(main, run, lang, onDay) else Box(Modifier.fillMaxWidth().height(CellHeight * run).border(0.5.dp, Line))
                        i = j
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabel(index: Int, lang: Lang) {
    Column(
        Modifier.fillMaxWidth().height(CellHeight).padding(1.dp).background(WeekdayColors[index]),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(Names.weekdayShort(index, Lang.EN).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(Names.weekdayShort(index, Lang.TE), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun WallCell(date: LocalDate, day: DayData?, lang: Lang, isToday: Boolean, onClick: () -> Unit) {
    val colors = LocalCalendarColors.current
    val special = day != null && (day.isHoliday || day.f.any { it.k == Festival.KIND_FESTIVAL })
    val numberColor = if (day != null && CalendarLogic.isRed(day)) colors.holidayRed else Ink
    Column(
        Modifier.fillMaxWidth().height(CellHeight)
            .background(if (special) PaperFestival else Paper)
            .border(if (isToday) 2.dp else 0.5.dp, if (isToday) colors.headerStart else Line)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(date.dayOfMonth.toString(), color = numberColor, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            if (day != null) {
                if (CalendarLogic.hasMoonMark(day, "pournami")) MoonMark(full = true, size = 8)
                if (CalendarLogic.hasMoonMark(day, "amavasya")) MoonMark(full = false, size = 8)
            }
        }
        if (day != null) {
            day.t.firstOrNull()?.let {
                Text(Names.tithi(it.i, lang), color = Ink, fontSize = 8.sp, lineHeight = 9.sp, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center)
            }
            day.n.firstOrNull()?.let {
                Text(Names.nakshatra(it.i, lang), color = Ink.copy(alpha = 0.7f), fontSize = 8.sp, lineHeight = 9.sp, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center)
            }
            CalendarLogic.headline(day)?.let { headline ->
                Text(
                    headline.name(lang == Lang.TE),
                    fontSize = 8.sp, lineHeight = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    color = if (headline.k == Festival.KIND_VRATAM) colors.vratam else colors.holidayRed,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** The festivals list that fills the blank corner of the grid (the full list is also printed under the grid). */
@Composable
private fun FestivalsBox(main: List<Pair<DayData, Festival>>, rows: Int, lang: Lang, onDay: (LocalDate) -> Unit) {
    val colors = LocalCalendarColors.current
    Column(
        Modifier.fillMaxWidth().height(CellHeight * rows).background(PaperInfo).border(0.5.dp, Line).padding(3.dp).clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(T.pandugulu.of(lang), color = colors.holidayRed, fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold)
        main.forEach { (day, fest) ->
            Text(
                "${day.date.dayOfMonth} ${fest.name(lang == Lang.TE)}",
                Modifier.clickable { onDay(day.date) },
                color = Ink, fontSize = 8.sp, lineHeight = 9.sp,
            )
        }
    }
}

/** The line printed under the grid: "14-Bhogi, 15-Sankranti, ...". */
@Composable
private fun WallFooter(main: List<Pair<DayData, Festival>>, lang: Lang) {
    if (main.isEmpty()) return
    Text(
        main.joinToString(", ") { (day, fest) -> "${day.date.dayOfMonth}-${fest.name(lang == Lang.TE)}" },
        Modifier.fillMaxWidth().background(PaperInfo).padding(horizontal = 8.dp, vertical = 6.dp),
        color = Ink, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
    )
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
