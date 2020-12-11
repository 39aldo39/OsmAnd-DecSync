package org.decsync.osmand

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class PrefUtils {
    companion object {
        const val DECSYNC_ENABLED = "decsync.enabled"
        const val THEME = "theme"
        const val LAST_UPDATE = "last_update"

        fun getDecsyncEnabled(context: Context): Boolean {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            return settings.getBoolean(DECSYNC_ENABLED, false)
        }

        fun setDecsyncEnabled(context: Context, value: Boolean) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean(DECSYNC_ENABLED, value)
            editor.apply()
        }

        fun notifyTheme(context: Context) {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            val mode = Integer.parseInt(settings.getString(THEME, null) ?: "-1")
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        fun getLastProcessedOsmandUpdate(context: Context): Long {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            return settings.getLong(LAST_UPDATE, 0)
        }

        fun setLastProcessedOsmandUpdate(context: Context, time: Long) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(LAST_UPDATE, time)
            editor.apply()
        }
    }
}