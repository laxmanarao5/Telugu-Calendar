package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.components.LoadingMonth
import com.telugucalendar.app.ui.components.MonthPager
import com.telugucalendar.app.ui.components.MoonPhase
import com.telugucalendar.app.ui.components.WeekdayColors
import com.telugucalendar.app.ui.theme.LocalCalendarColors
import java.time.LocalDate
import java.time.YearMonth

private val LabelWidth = 44.dp
private val CellHeight = 84.dp

/**
 * The modern month table: coloured chips for the day, month, year and the lunar months with the Saka year; then one row per weekday and one column
 * per week, each date with its paksha, tithi number, nakshatra and moon; and the selected day's full panchangam underneath.
 */
@Composable
fun ModernCalendarScreen(
    app: AppData,
    lang: Lang,
    month: YearMonth,
    selected: LocalDate,
    onMonth: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit,
) {
    val index = app.months.indexOf(month)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { app.months.getOrNull(index - 1)?.let(onMonth) }, enabled = index > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = T.previous.of(lang))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AssistChip(onClick = onToday, label = { Text(T.today.of(lang)) })
            }
            IconButton(onClick = { app.months.getOrNull(index + 1)?.let(onMonth) }, enabled = index in 0 until app.months.lastIndex) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = T.next.of(lang))
            }
        }
        MonthPager(app.months, month, onMonth, Modifier.fillMaxSize()) { ym ->
            if (app.isLoaded(ym) || app.complete) ModernPage(app, ym, lang, selected, onSelect) else LoadingMonth()
        }
    }
}

@Composable
private fun ModernPage(app: AppData, ym: YearMonth, lang: Lang, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    // Show the selected day's details when it falls in this page's month, otherwise today (if this is the current month) or the first day.
    val shown = if (YearMonth.from(selected) == ym) selected else if (YearMonth.from(today) == ym) today else ym.atDay(1)
    val days = app.daysOf(ym)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModernChips(app, ym, shown, days, lang)
        ModernGrid(app, ym, shown, today, lang, onSelect)
        val day = app.day(shown)
        if (day != null) DayPanchangam(day, app, lang, Modifier.padding(bottom = 16.dp))
        else Text(T.noDataForMonth.of(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The header row: day, month and year chips and a green one with the lunar months, the Saka year and the Telugu year name. */
@Composable
private fun ModernChips(app: AppData, ym: YearMonth, shown: LocalDate, days: List<DayData>, lang: Lang) {
    val mid = days.getOrNull(days.size / 2)
    val samvatsara = app.samvatsara(shown)
    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Chip(shown.dayOfMonth.toString(), Color(0xFFD81B60), Modifier.width(48.dp), 22)
        Chip(Names.gregorianMonth(ym.monthValue, lang), Color(0xFFE65100), Modifier.weight(1f), 17)
        Chip(ym.year.toString(), Color(0xFFC62828), Modifier.width(66.dp), 20)
        Column(
            Modifier.weight(1.6f).fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFF2E7D32)).padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(CalendarLogic.lunarMonthsLabel(days, lang), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (mid != null) {
                val name = samvatsara?.let { if (lang == Lang.TE) it.te else it.en }.orEmpty()
                Text("${T.shakaYear.of(lang)} ${CalendarLogic.shakaYear(mid)} $name", color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color, modifier: Modifier, size: Int) {
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = size.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun ModernGrid(app: AppData, ym: YearMonth, shown: LocalDate, today: LocalDate, lang: Lang, onSelect: (LocalDate) -> Unit) {
    val weeks = CalendarLogic.weeksOf(ym)
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(LabelWidth)) {
            for (i in 0..6) {
                Column(
                    Modifier.fillMaxWidth().height(CellHeight).padding(1.dp).clip(RoundedCornerShape(8.dp)).background(WeekdayColors[i]),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(Names.weekdayShort(i, Lang.TE), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(Names.weekdayShort(i, Lang.EN).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        weeks.forEach { week ->
            Column(Modifier.weight(1f)) {
                week.forEach { date ->
                    if (date == null) Box(Modifier.fillMaxWidth().height(CellHeight))
                    else ModernCell(date, app.day(date), lang, isSelected = date == shown, isToday = date == today) { onSelect(date) }
                }
            }
        }
    }
}

@Composable
private fun ModernCell(date: LocalDate, day: DayData?, lang: Lang, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val colors = LocalCalendarColors.current
    val primary = MaterialTheme.colorScheme.primary
    val tint = WeekdayColors[CalendarLogic.sundayIndex(date)]
    val shape = RoundedCornerShape(8.dp)
    val numberColor = if (day != null && CalendarLogic.isRed(day)) colors.holidayRed else MaterialTheme.colorScheme.onSurface
    val tithi = day?.t?.firstOrNull()
    Column(
        Modifier.fillMaxWidth().height(CellHeight).padding(1.dp)
            .clip(shape)
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) primary else tint.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (tithi != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Names.pakshaShort(tithi.i, lang), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(16.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                    Text(((tithi.i - 1) % 15 + 1).toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        day?.n?.firstOrNull()?.let {
            Text(Names.nakshatra(it.i, lang), fontSize = 8.sp, lineHeight = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(date.dayOfMonth.toString(), color = numberColor, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
            if (tithi != null) MoonPhase(tithi.i, 13.dp)
        }
        day?.let { CalendarLogic.headline(it) }?.let { headline ->
            Text(
                headline.name(lang == Lang.TE),
                fontSize = 8.sp, lineHeight = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                color = if (headline.k == Festival.KIND_VRATAM) colors.vratam else colors.holidayRed, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
