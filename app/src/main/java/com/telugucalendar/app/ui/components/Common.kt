package com.telugucalendar.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telugucalendar.app.data.Festival
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.theme.LocalCalendarColors
import java.time.YearMonth

/**
 * A pager with one page per month. [month] is the shared current month: swiping calls [onMonth], and changing [month]
 * from outside (the arrows, "Today") scrolls the pager.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthPager(
    months: List<YearMonth>,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (YearMonth) -> Unit,
) {
    val index = months.indexOf(month).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = index) { months.size }
    LaunchedEffect(pager.settledPage) {
        months.getOrNull(pager.settledPage)?.let { if (it != month) onMonth(it) }
    }
    LaunchedEffect(index) {
        if (pager.currentPage != index) pager.animateScrollToPage(index)
    }
    HorizontalPager(state = pager, modifier = modifier, beyondViewportPageCount = 0) { page -> content(months[page]) }
}

/** A full moon (outlined) or new moon (filled) mark for Pournami and Amavasya. */
@Composable
fun MoonMark(full: Boolean, modifier: Modifier = Modifier, size: Int = 9) {
    val ink = MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (full) Color(0xFFFFE082) else ink)
            .border(1.dp, ink, CircleShape),
    )
}

/** The small coloured label for the kind of day item: Holiday / Optional holiday. */
@Composable
fun HolidayTag(level: Int, lang: Lang, modifier: Modifier = Modifier) {
    if (level <= 0) return
    val red = LocalCalendarColors.current.holidayRed
    Text(
        if (level == 1) T.holiday.of(lang) else T.optionalHoliday.of(lang),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(red.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = red,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/** One festival line: name in the chosen language, the other language underneath, and its tag. */
@Composable
fun FestivalLine(festival: Festival, lang: Lang, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(festival.name(lang == Lang.TE), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            HolidayTag(festival.h, lang)
        }
        val other = if (lang == Lang.TE) festival.en else festival.te
        if (other.isNotBlank() && other != festival.name(lang == Lang.TE)) {
            Text(other, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
