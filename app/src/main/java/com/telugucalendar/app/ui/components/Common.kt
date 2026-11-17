package com.telugucalendar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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

/** Shown for a month whose data is still being read in the background. */
@Composable
fun LoadingMonth() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** One colour per weekday, Sunday first, for the weekday labels of the wall and modern views. */
val WeekdayColors = listOf(
    Color(0xFFC62828), Color(0xFF455A64), Color(0xFFE65100), Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFFAD1457),
)

/** The moon as it looks on a tithi (1..15 waxing to the full moon, 16..30 waning to the new moon). */
@Composable
fun MoonPhase(tithi: Int, diameter: Dp, modifier: Modifier = Modifier) {
    val dark = Color(0xFF37474F)
    val lit = Color(0xFFFFD54F)
    Canvas(modifier.size(diameter)) {
        val r = this.size.minDimension / 2f
        val centre = Offset(r, r)
        val waxing = tithi <= 15
        val fraction = if (waxing) tithi / 15f else (30 - tithi) / 15f
        drawCircle(dark, r, centre)
        when {
            fraction >= 0.99f -> drawCircle(lit, r, centre)
            fraction <= 0.01f -> Unit
            else -> {
                // The lit half (right when waxing, left when waning), then the terminator: an ellipse that is dark for a crescent and lit for a gibbous moon.
                drawArc(lit, startAngle = if (waxing) -90f else 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset.Zero, size = Size(2 * r, 2 * r))
                val rx = kotlin.math.abs(1f - 2f * fraction) * r
                drawOval(if (fraction < 0.5f) dark else lit, topLeft = Offset(r - rx, 0f), size = Size(2 * rx, 2 * r))
            }
        }
    }
}
