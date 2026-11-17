package com.telugucalendar.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.telugucalendar.app.data.DefaultView
import com.telugucalendar.app.domain.Lang
import com.telugucalendar.app.domain.T
import com.telugucalendar.app.ui.components.BannerAd
import com.telugucalendar.app.ui.screens.DaySheet
import com.telugucalendar.app.ui.screens.FestivalsScreen
import com.telugucalendar.app.ui.screens.ModernCalendarScreen
import com.telugucalendar.app.ui.screens.MoreScreen
import com.telugucalendar.app.ui.screens.WallCalendarScreen
import com.telugucalendar.app.ui.theme.TeluguCalendarTheme

private enum class Tab(val label: com.telugucalendar.app.domain.Str, val icon: ImageVector) {
    WALL(T.tabWall, Icons.Filled.CalendarViewMonth),
    MODERN(T.tabModern, Icons.Filled.CalendarMonth),
    FESTIVALS(T.tabFestivals, Icons.Filled.Celebration),
    MORE(T.tabMore, Icons.Filled.MoreHoriz),
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    TeluguCalendarTheme(theme) {
        Root(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Root(vm: AppViewModel) {
    val state by vm.data.collectAsStateWithLifecycle()
    val lang by vm.lang.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val sheetOpen by vm.sheetOpen.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val updateDialog by vm.updateDialog.collectAsStateWithLifecycle()
    // The tab the app opens on is the "Opens on" choice from Settings (Wall unless changed); a tap on the bottom bar overrides it for this session.
    val defaultView by vm.defaultView.collectAsStateWithLifecycle()
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    val tab = if (picked >= 0) picked else (defaultView ?: DefaultView.WALL).ordinal
    val app = (state as? DataState.Ready)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(T.appTitle.of(lang), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    if (app != null && tab == Tab.WALL.ordinal) {
                        val index = app.months.indexOf(month)
                        IconButton(onClick = { app.months.getOrNull(index - 1)?.let(vm::setMonth) }, enabled = index > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = T.previous.of(lang))
                        }
                        IconButton(onClick = { app.months.getOrNull(index + 1)?.let(vm::setMonth) }, enabled = index in 0 until app.months.lastIndex) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = T.next.of(lang))
                        }
                        IconButton(onClick = vm::goToToday) { Icon(Icons.Filled.Today, contentDescription = T.today.of(lang)) }
                    }
                    TextButton(onClick = { vm.setLang(if (lang == Lang.TE) Lang.EN else Lang.TE) }) {
                        Text(if (lang == Lang.TE) "EN" else "తె")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                // Google AdMob banner (test ads until ADMOB_* ids are put in secrets.properties).
                BannerAd()
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t.ordinal,
                            onClick = { picked = t.ordinal },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label.of(lang)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is DataState.Loading -> Centered { CircularProgressIndicator() }
                is DataState.Failed -> Centered { Text("${T.noData.of(lang)}\n${s.message}", color = MaterialTheme.colorScheme.error) }
                is DataState.Ready -> when (Tab.entries[tab]) {
                    Tab.WALL -> WallCalendarScreen(s.data, lang, month, vm::setMonth, vm::openDay)
                    Tab.MODERN -> ModernCalendarScreen(s.data, lang, month, selected, vm::setMonth, vm::select, vm::goToToday)
                    Tab.FESTIVALS -> FestivalsScreen(s.data, lang, vm::openDay)
                    Tab.MORE -> MoreScreen(
                        app, lang, theme, defaultView ?: DefaultView.WALL, update,
                        vm::setLang, vm::setTheme, vm::setDefaultView,
                        { vm.checkForUpdate(manual = true) }, vm::showUpdateDialog,
                    )
                }
            }
        }
    }

    val shownDay = if (sheetOpen) app?.day(selected) else null
    if (app != null && shownDay != null) DaySheet(shownDay, app, lang, vm::closeSheet)
    if (updateDialog) UpdateDialog(vm, update, lang)
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun UpdateDialog(vm: AppViewModel, update: UpdateUi, lang: Lang) {
    val context = LocalContext.current
    val updater = vm.updaterInstance
    when (update) {
        is UpdateUi.Available -> AlertDialog(
            onDismissRequest = vm::dismissUpdateDialog,
            title = { Text("${T.updateAvailable.of(lang)}: ${update.info.versionName}") },
            text = {
                if (update.info.notes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(T.whatsNew.of(lang), style = MaterialTheme.typography.titleSmall)
                        Text(update.info.notes)
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::downloadUpdate) { Text(T.updateNow.of(lang)) } },
            dismissButton = { TextButton(onClick = vm::dismissUpdateDialog) { Text(T.later.of(lang)) } },
        )
        is UpdateUi.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("${T.downloading.of(lang)} ${update.percent}%") },
            text = { LinearProgressIndicator(progress = { update.percent / 100f }, Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
        is UpdateUi.Downloaded -> AlertDialog(
            onDismissRequest = vm::dismissUpdateDialog,
            title = { Text("${T.updateAvailable.of(lang)}: ${update.info.versionName}") },
            text = { if (!updater.canInstall()) Text(T.allowInstall.of(lang)) },
            confirmButton = {
                TextButton(onClick = {
                    if (updater.canInstall()) context.startActivity(updater.installIntent(update.file))
                    else context.startActivity(updater.permissionIntent())
                }) { Text(T.install.of(lang)) }
            },
            dismissButton = { TextButton(onClick = vm::dismissUpdateDialog) { Text(T.later.of(lang)) } },
        )
        is UpdateUi.Failed -> AlertDialog(
            onDismissRequest = vm::dismissUpdateDialog,
            title = { Text(T.updateFailed.of(lang)) },
            text = { Text(update.message) },
            confirmButton = { TextButton(onClick = vm::dismissUpdateDialog) { Text(T.close.of(lang)) } },
        )
        else -> Unit
    }
}
