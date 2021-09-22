package org.decsync.osmand

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class PrefUtils {
    companion object {
        const val DECSYNC_ENABLED = "decsync.enabled"
        const val THEME = "theme"
        const val LAST_UPDATE = "last_update"
        const val INTRO_DONE = "intro.done"
        const val OSMAND_DATA_DIR = "osmand_data_dir"

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

        fun getIntroDone(context: Context): Boolean {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            return settings.getBoolean(INTRO_DONE, false)
        }

        fun setIntroDone(context: Context, value: Boolean) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean(INTRO_DONE, value)
            editor.apply()
        }

        fun getOsmandFavoritesUri(context: Context): Uri? {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            return settings.getString(OSMAND_DATA_DIR, null)?.let(Uri::parse)
        }

        fun setOsmandFavoritesUri(context: Context, value: Uri) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString(OSMAND_DATA_DIR, value.toString())
            editor.apply()
        }

        fun removeOsmandFavoritesUri(context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.remove(OSMAND_DATA_DIR)
            editor.apply()
        }
    }
}