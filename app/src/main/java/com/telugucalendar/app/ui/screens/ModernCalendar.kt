package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.DayData
import com.telugucalendar.app.data.Festival
import com.telugucalendar.app.domain.CalendarLogic
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.Names
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.components.MonthPager
import com.telugucalendar.app.ui.theme.LocalCalendarColors
import java.time.LocalDate
import java.time.YearMonth

/**
 * The modern view: a clean month grid with a dot under days that have something, and the panchangam of the selected day right below.
 * Swipe sideways or use the arrows to change month.
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { app.months.getOrNull(index - 1)?.let(onMonth) }, enabled = index > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = T.previous.of(lang))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${Names.gregorianMonth(month.monthValue, lang)} ${month.year}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                )
                Text(
                    CalendarLogic.lunarMonthsLabel(app.daysOf(month), lang),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(onClick = onToday, label = { Text(T.today.of(lang)) })
            IconButton(onClick = { app.months.getOrNull(index + 1)?.let(onMonth) }, enabled = index in 0 until app.months.lastIndex) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = T.next.of(lang))
            }
        }
        MonthPager(app.months, month, onMonth, Modifier.fillMaxSize()) { ym ->
            ModernPage(app, ym, lang, selected, onSelect)
        }
    }
}

@Composable
private fun ModernPage(app: AppData, ym: YearMonth, lang: Lang, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val red = LocalCalendarColors.current.holidayRed
    val today = LocalDate.now()
    // Show the selected day's details when it falls in this page's month, otherwise the first day of the month (or today).
    val shown = if (YearMonth.from(selected) == ym) selected else if (YearMonth.from(today) == ym) today else ym.atDay(1)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                Text(
                    Names.weekdayShort(i, lang), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (i == 0) red else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CalendarLogic.monthGrid(ym).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(0.95f), contentAlignment = Alignment.Center) {
                        if (date != null) ModernCell(date, app.day(date), isSelected = date == shown, isToday = date == today) { onSelect(date) }
                    }
                }
            }
        }
        val day = app.day(shown)
        if (day != null) DayPanchangam(day, app, lang, Modifier.padding(bottom = 16.dp))
        else Text(T.noDataForMonth.of(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModernCell(date: LocalDate, day: DayData?, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val colors = LocalCalendarColors.current
    val primary = MaterialTheme.colorScheme.primary
    val numberColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        day != null && CalendarLogic.isRed(day) -> colors.holidayRed
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(if (isSelected) primary else androidx.compose.ui.graphics.Color.Transparent)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, primary, CircleShape) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(date.dayOfMonth.toString(), color = numberColor, fontSize = 16.sp, fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal)
        }
        // One dot: red = festival or holiday, gold = vratam / other special day, nothing when the day is plain.
        val dot = when {
            day == null -> null
            day.f.any { it.k == Festival.KIND_FESTIVAL || it.k == Festival.KIND_HOLIDAY || it.h > 0 } -> colors.holidayRed
            day.f.isNotEmpty() -> colors.vratam
            else -> null
        }
        Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(dot ?: androidx.compose.ui.graphics.Color.Transparent))
    }
}
