package org.decsync.osmand

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar

@ExperimentalStdlibApi
class SettingsActivity : AppCompatActivity() {
    private var syncNowMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        PrefUtils.notifyTheme(this)
        super.onCreate(savedInstanceState)

        if (!PrefUtils.getIntroDone(this)) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_activity, menu)
        syncNowMenuItem = menu.findItem(R.id.sync_now)
        syncNowMenuItem?.isEnabled = PrefUtils.getDecsyncEnabled(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sync_now -> {
                if (PrefUtils.getDecsyncEnabled(this)) {
                    val workManager = WorkManager.getInstance(this)
                    val workRequest = OneTimeWorkRequest.Builder(DecsyncWorker::class.java).build()
                    workManager.enqueue(workRequest)
                    Snackbar.make(findViewById(R.id.settings), R.string.settings_synchronizing_now, Snackbar.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.decsync_disabled, Toast.LENGTH_LONG).show()
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>(PrefUtils.DECSYNC_ENABLED)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    val intent = Intent(requireActivity(), IntroActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                    false
                } else {
                    (requireActivity() as SettingsActivity).syncNowMenuItem?.isEnabled = false
                    val workManager = WorkManager.getInstance(requireContext())
                    workManager.cancelAllWork()
                    true
                }
            }

            findPreference<Preference>(PrefUtils.THEME)?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            findPreference<Preference>(PrefUtils.THEME)?.setOnPreferenceChangeListener { _, newValue ->
                val mode = Integer.parseInt(newValue as String)
                AppCompatDelegate.setDefaultNightMode(mode)
                true
            }
        }
    }
}