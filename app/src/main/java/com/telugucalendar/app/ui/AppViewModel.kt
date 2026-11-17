package com.telugucalendar.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telugucalendar.app.BuildConfig
import com.telugucalendar.app.data.AppData
import com.telugucalendar.app.data.CalendarRepository
import com.telugucalendar.app.data.DefaultView
import com.telugucalendar.app.data.Prefs
import com.telugucalendar.app.data.ThemeMode
import com.telugucalendar.app.data.update.UpdateInfo
import com.telugucalendar.app.data.update.Updater
import com.telugucalendar.app.domain.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

sealed interface DataState {
    data object Loading : DataState
    data class Ready(val data: AppData) : DataState
    data class Failed(val message: String) : DataState
}

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateUi
    data class Downloaded(val info: UpdateInfo, val file: File) : UpdateUi
    data class Failed(val message: String) : UpdateUi
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val updater = Updater(app)

    val updaterInstance get() = updater

    private val _data = MutableStateFlow<DataState>(DataState.Loading)
    val data: StateFlow<DataState> = _data

    val lang: StateFlow<Lang> = prefs.lang.stateIn(viewModelScope, SharingStarted.Eagerly, Lang.TE)
    val theme: StateFlow<ThemeMode> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** The view chosen in Settings to open on. Null only for the instant before the saved choice has been read. */
    val defaultView: StateFlow<DefaultView?> = prefs.defaultView.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The month both calendar views show (they share it, so switching tabs keeps your place). */
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    /** The day highlighted in the modern view and shown in the day sheet. */
    private val _selected = MutableStateFlow(LocalDate.now())
    val selected: StateFlow<LocalDate> = _selected

    private val _sheetOpen = MutableStateFlow(false)
    val sheetOpen: StateFlow<Boolean> = _sheetOpen

    private val _update = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val update: StateFlow<UpdateUi> = _update

    private val _updateDialog = MutableStateFlow(false)
    val updateDialog: StateFlow<Boolean> = _updateDialog

    init {
        viewModelScope.launch {
            // Read just the current month first so the screen can appear at once, then the rest in the background.
            val repo = runCatching { withContext(Dispatchers.IO) { CalendarRepository(app.assets) } }
            val first = repo.mapCatching { r -> withContext(Dispatchers.IO) { r.first(YearMonth.now()) } }
            first.onFailure { _data.value = DataState.Failed(it.message ?: "") }
            first.onSuccess { firstChunk ->
                _month.value = firstChunk.clamp(YearMonth.now())
                _data.value = DataState.Ready(firstChunk)
                runCatching { withContext(Dispatchers.IO) { repo.getOrThrow().rest(firstChunk) } }
                    .onSuccess { _data.value = DataState.Ready(it) }
                    .onFailure { _data.value = DataState.Failed(it.message ?: "") }
            }
        }
        checkForUpdate(manual = false)
    }

    fun setMonth(ym: YearMonth) { _month.value = ym }

    fun goToToday() {
        val today = LocalDate.now()
        val ready = (_data.value as? DataState.Ready)?.data
        _month.value = ready?.clamp(YearMonth.from(today)) ?: YearMonth.from(today)
        _selected.value = today
    }

    fun select(date: LocalDate) { _selected.value = date }

    fun openDay(date: LocalDate) {
        _selected.value = date
        _sheetOpen.value = true
    }

    fun closeSheet() { _sheetOpen.value = false }

    fun setLang(lang: Lang) { viewModelScope.launch { prefs.setLang(lang) } }
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { prefs.setTheme(mode) } }
    fun setDefaultView(view: DefaultView) { viewModelScope.launch { prefs.setDefaultView(view) } }

    /** [manual] false = the quiet check at launch (no message when nothing is new or the network is down). */
    fun checkForUpdate(manual: Boolean) {
        if (_update.value is UpdateUi.Checking || _update.value is UpdateUi.Downloading) return
        viewModelScope.launch {
            _update.value = UpdateUi.Checking
            runCatching { updater.check(BuildConfig.VERSION_CODE) }
                .onSuccess { info ->
                    if (info == null) {
                        _update.value = if (manual) UpdateUi.UpToDate else UpdateUi.Idle
                    } else {
                        _update.value = UpdateUi.Available(info)
                        if (manual || prefs.dismissedUpdate.first() != info.versionCode) _updateDialog.value = true
                    }
                }
                .onFailure { _update.value = if (manual) UpdateUi.Failed(it.message ?: "") else UpdateUi.Idle }
        }
    }

    fun dismissUpdateDialog() {
        val state = _update.value
        _updateDialog.value = false
        if (state is UpdateUi.Available) viewModelScope.launch { prefs.dismissUpdate(state.info.versionCode) }
    }

    fun showUpdateDialog() { _updateDialog.value = true }

    fun downloadUpdate() {
        val info = when (val s = _update.value) {
            is UpdateUi.Available -> s.info
            is UpdateUi.Downloaded -> return
            else -> return
        }
        viewModelScope.launch {
            _update.value = UpdateUi.Downloading(info, 0)
            runCatching { updater.download(info) { p -> _update.update { UpdateUi.Downloading(info, p) } } }
                .onSuccess { _update.value = UpdateUi.Downloaded(info, it) }
                .onFailure { _update.value = UpdateUi.Failed(it.message ?: "") }
        }
    }
}
