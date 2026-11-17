package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.DayData
import com.telugucalendar.app.data.Slot
import com.telugucalendar.app.domain.CalendarLogic
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.Names
import com.telugucalendar.app.domain.Str
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.components.FestivalLine
import java.time.LocalDate

/** The full panchangam for one day: date, Telugu year / month, the day's festivals, then tithi, nakshatra, yoga, karana and the timings. */
@Composable
fun DayPanchangam(day: DayData, app: AppData, lang: Lang, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(CalendarLogic.dateTitle(day.date, lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val samvatsara = app.samvatsara(day.date)
        val heading = listOfNotNull(
            samvatsara?.let { Names.samvatsaraTitle(Str(it.te, it.en), lang) },
            Names.ayana(day.ay, lang),
            Names.ritu(day.ms, lang),
            Names.lunarMonth(day.ms, day.isAdhika, lang),
        ).joinToString(" · ")
        Text(heading, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (day.f.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    day.f.forEach { fest ->
                        Column {
                            FestivalLine(fest, lang)
                            val info = fest.info(lang == Lang.TE)
                            if (info.isNotBlank()) {
                                Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                InfoRow(T.tithi.of(lang), slotLines(day.t, day.date, lang) { Names.paksha(it, lang) + " – " + Names.tithi(it, lang) })
                InfoRow(T.nakshatra.of(lang), slotLines(day.n, day.date, lang) { Names.nakshatra(it, lang) })
                InfoRow(T.yoga.of(lang), slotLines(day.y, day.date, lang) { Names.yoga(it, lang) })
                InfoRow(T.karana.of(lang), slotLines(day.k, day.date, lang) { Names.karana(it, lang) })
                InfoRow(T.sunrise.of(lang), listOf(CalendarLogic.time12(day.sr)))
                InfoRow(T.sunset.of(lang), listOf(CalendarLogic.time12(day.ss)))
                InfoRow(T.rahu.of(lang), listOf(CalendarLogic.range12(day.rk)))
                InfoRow(T.yama.of(lang), listOf(CalendarLogic.range12(day.ym)))
                InfoRow(T.gulika.of(lang), listOf(CalendarLogic.range12(day.gk)))
                InfoRow(T.abhijit.of(lang), listOf(CalendarLogic.range12(day.ab)), last = true)
            }
        }
    }
}

private fun slotLines(slots: List<Slot>, date: LocalDate, lang: Lang, name: (Int) -> String): List<String> =
    slots.map { slot ->
        val until = CalendarLogic.untilText(CalendarLogic.endLabel(slot, date, lang), lang)
        if (until.isBlank()) name(slot.i) else "${name(slot.i)}  ($until)"
    }

@Composable
private fun InfoRow(label: String, lines: List<String>, last: Boolean = false) {
    val shown = lines.filter { it.isNotBlank() }
    if (shown.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(0.38f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(0.62f)) {
            shown.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
        }
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** The day sheet opened by tapping a day in the wall calendar or the festivals list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(day: DayData, app: AppData, lang: Lang, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        DayPanchangam(
            day, app, lang,
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        )
    }
}
