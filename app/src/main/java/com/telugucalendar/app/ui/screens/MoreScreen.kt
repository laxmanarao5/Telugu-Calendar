package com.telugucalendar.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telugucalendar.app.BuildConfig
import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.DefaultView
import com.telugucalendar.app.data.ThemeMode
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.UpdateUi

@Composable
fun MoreScreen(
    app: AppData?,
    lang: Lang,
    theme: ThemeMode,
    defaultView: DefaultView,
    update: UpdateUi,
    onLang: (Lang) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDefaultView: (DefaultView) -> Unit,
    onCheckUpdate: () -> Unit,
    onShowUpdate: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section(T.language.of(lang)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = lang == Lang.TE, onClick = { onLang(Lang.TE) }, label = { Text(T.teluguName.te) })
                FilterChip(selected = lang == Lang.EN, onClick = { onLang(Lang.EN) }, label = { Text(T.englishName.en) })
            }
        }
        Section(T.defaultView.of(lang)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = defaultView == DefaultView.WALL, onClick = { onDefaultView(DefaultView.WALL) }, label = { Text(T.tabWall.of(lang)) })
                FilterChip(selected = defaultView == DefaultView.MODERN, onClick = { onDefaultView(DefaultView.MODERN) }, label = { Text(T.tabModern.of(lang)) })
                FilterChip(selected = defaultView == DefaultView.FESTIVALS, onClick = { onDefaultView(DefaultView.FESTIVALS) }, label = { Text(T.tabFestivals.of(lang)) })
            }
        }
        Section(T.theme.of(lang)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = theme == ThemeMode.SYSTEM, onClick = { onTheme(ThemeMode.SYSTEM) }, label = { Text(T.themeSystem.of(lang)) })
                FilterChip(selected = theme == ThemeMode.LIGHT, onClick = { onTheme(ThemeMode.LIGHT) }, label = { Text(T.themeLight.of(lang)) })
                FilterChip(selected = theme == ThemeMode.DARK, onClick = { onTheme(ThemeMode.DARK) }, label = { Text(T.themeDark.of(lang)) })
            }
        }
        Section(T.about.of(lang)) {
            Text("${T.version.of(lang)}: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            if (app != null) {
                Text("${T.dataYears.of(lang)}: ${app.sortedYears.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            }
            UpdateRow(update, lang, onCheckUpdate, onShowUpdate)
            Text(T.dataNote.of(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(T.adsNote.of(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpdateRow(update: UpdateUi, lang: Lang, onCheck: () -> Unit, onShow: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (update) {
            is UpdateUi.Checking -> Text(T.checking.of(lang))
            is UpdateUi.UpToDate -> Text(T.upToDate.of(lang), color = MaterialTheme.colorScheme.tertiary)
            is UpdateUi.Failed -> Text("${T.updateFailed.of(lang)}: ${update.message}", color = MaterialTheme.colorScheme.error)
            is UpdateUi.Available -> Text("${T.updateAvailable.of(lang)}: ${update.info.versionName}", fontWeight = FontWeight.SemiBold)
            is UpdateUi.Downloading -> {
                Text("${T.downloading.of(lang)} ${update.percent}%")
                LinearProgressIndicator(progress = { update.percent / 100f }, Modifier.fillMaxWidth())
            }
            is UpdateUi.Downloaded -> Text("${T.updateAvailable.of(lang)}: ${update.info.versionName}", fontWeight = FontWeight.SemiBold)
            is UpdateUi.Idle -> Unit
        }
        val hasUpdate = update is UpdateUi.Available || update is UpdateUi.Downloaded || update is UpdateUi.Downloading
        Button(onClick = if (hasUpdate) onShow else onCheck, enabled = update !is UpdateUi.Checking) {
            Text(if (hasUpdate) T.updateNow.of(lang) else T.checkUpdate.of(lang))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
