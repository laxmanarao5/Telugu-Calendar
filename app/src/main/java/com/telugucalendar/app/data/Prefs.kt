package com.telugucalendar.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.telugucalendar.app.domain.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The tab the app opens on. The ordinals match the bottom-bar order (Wall, Modern, Festivals). Wall is the default. */
enum class DefaultView { WALL, MODERN, FESTIVALS }

class Prefs(private val context: Context) {
    private val langKey = stringPreferencesKey("lang")
    private val themeKey = stringPreferencesKey("theme")
    private val viewKey = stringPreferencesKey("default_view")
    private val dismissedKey = stringPreferencesKey("dismissed_update")

    val lang: Flow<Lang> = context.store.data.map { Lang.from(it[langKey]) }
    val theme: Flow<ThemeMode> = context.store.data.map { p -> ThemeMode.entries.firstOrNull { it.name == p[themeKey] } ?: ThemeMode.SYSTEM }

    val defaultView: Flow<DefaultView> = context.store.data.map { p -> DefaultView.entries.firstOrNull { it.name == p[viewKey] } ?: DefaultView.WALL }

    /** versionCode of an update the user chose "Later" for, so the launch check does not nag again for the same one. */
    val dismissedUpdate: Flow<Int> = context.store.data.map { it[dismissedKey]?.toIntOrNull() ?: 0 }

    suspend fun setLang(lang: Lang) { context.store.edit { it[langKey] = lang.code } }
    suspend fun setTheme(mode: ThemeMode) { context.store.edit { it[themeKey] = mode.name } }
    suspend fun setDefaultView(view: DefaultView) { context.store.edit { it[viewKey] = view.name } }
    suspend fun dismissUpdate(versionCode: Int) { context.store.edit { it[dismissedKey] = versionCode.toString() } }
}
