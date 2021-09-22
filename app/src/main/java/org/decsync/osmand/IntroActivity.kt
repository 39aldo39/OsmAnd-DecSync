package org.decsync.osmand

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.appintro.AppIntro2
import com.github.appintro.SlidePolicy
import kotlinx.android.synthetic.main.activity_intro_configure_osmand.*
import kotlinx.android.synthetic.main.activity_intro_directory.*
import org.decsync.library.DecsyncPrefUtils
import org.decsync.library.InsufficientAccessException
import org.decsync.library.checkUriPermissions
import java.util.concurrent.TimeUnit

@ExperimentalStdlibApi
class IntroActivity : AppIntro2() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isWizardMode = true
        showStatusBar(true)
        setBarColor(ContextCompat.getColor(this, R.color.bg_intro_bottom_bar))

        val osmandFavoritesUri = PrefUtils.getOsmandFavoritesUri(this)
        if (osmandFavoritesUri != null) {
            try {
                checkUriPermissions(this, osmandFavoritesUri)
            } catch (e: InsufficientAccessException) {
                PrefUtils.removeOsmandFavoritesUri(this)
            }
        }
        val decsyncDir = DecsyncPrefUtils.getDecsyncDir(this)
        if (decsyncDir != null) {
            try {
                checkUriPermissions(this, decsyncDir)
            } catch (e: InsufficientAccessException) {
                // TODO
                //DecsyncPrefUtils.removeDecsyncDir(this)
                val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                editor.remove(DecsyncPrefUtils.DECSYNC_DIRECTORY)
                editor.apply()
            }
        }

        if (!PrefUtils.getIntroDone(this)) {
            addSlide(SlideWelcome())
        }
        if (!SlideInstallOsmand.isPolicyRespected(this)) {
            addSlide(SlideInstallOsmand())
        }
        addSlide(SlideConfigureOsmand())
        addSlide(SlideDirectory())
    }

    override fun onIntroFinished() {
        super.onIntroFinished()

        PrefUtils.setIntroDone(this, true)
        PrefUtils.setDecsyncEnabled(this, true)

        val workManager = WorkManager.getInstance(this)
        val inputData = Data.Builder()
            .putBoolean(DecsyncWorker.KEY_IS_INIT_SYNC, true)
            .build()
        val workRequest = OneTimeWorkRequest.Builder(DecsyncWorker::class.java)
            .setInputData(inputData)
            .build()
        workManager.enqueue(workRequest)

        val periodicWorkRequest = PeriodicWorkRequest.Builder(DecsyncWorker::class.java, 1, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        workManager.enqueue(periodicWorkRequest)

        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }
}

class SlideWelcome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_intro_welcome, container, false)
    }
}

class SlideInstallOsmand : Fragment(), SlidePolicy {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_intro_install_osmand, container, false)

        val installOsmandButton = view.findViewById<Button>(R.id.intro_install_osmand_button)
        installOsmandButton.setOnClickListener {
            Utils.installApp(requireActivity(), "net.osmand.plus")
        }

        return view
    }

    override val isPolicyRespected: Boolean
        get() = isPolicyRespected(requireActivity())

    override fun onUserIllegallyRequestedNextPage() {
        Toast.makeText(requireActivity(), R.string.intro_install_osmand_select, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun isPolicyRespected(activity: Activity): Boolean {
            return Utils.appInstalled(activity, "net.osmand.plus")
        }
    }
}

class SlideConfigureOsmand : Fragment(), SlidePolicy {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_intro_configure_osmand, container, false)

        val button = view.findViewById<Button>(R.id.intro_configure_osmand_button)
        val uri = PrefUtils.getOsmandFavoritesUri(requireActivity())
        if (uri != null) {
            val name = DecsyncPrefUtils.getNameFromUri(requireActivity(), uri)
            button.text = name
        }
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= 26) {
                val volumeId = "primary"
                val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/$volumeId%3Aosmand")
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
            startActivityForResult(intent, CHOOSE_OSMAND_FAVORITES_URI)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CHOOSE_OSMAND_FAVORITES_URI -> {
                val uri = data?.data
                if (resultCode == Activity.RESULT_OK && uri != null) {
                    val name = DecsyncPrefUtils.getNameFromUri(requireActivity(), uri)
                    if (name != "favourites.gpx") {
                        Toast.makeText(requireActivity(), R.string.intro_configure_osmand_incorrect_file, Toast.LENGTH_SHORT).show()
                        return
                    }
                    val cr = requireActivity().contentResolver
                    cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    PrefUtils.setOsmandFavoritesUri(requireActivity(), uri)
                    intro_configure_osmand_button.text = name
                }
            }
        }
    }

    override val isPolicyRespected: Boolean
        get() = PrefUtils.getOsmandFavoritesUri(requireActivity()) != null

    override fun onUserIllegallyRequestedNextPage() {
        Toast.makeText(requireActivity(), R.string.intro_configure_osmand_select, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val CHOOSE_OSMAND_FAVORITES_URI = 1
    }
}

@ExperimentalStdlibApi
class SlideDirectory : Fragment(), SlidePolicy {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_intro_directory, container, false)

        val button = view.findViewById<Button>(R.id.intro_directory_button)
        val decsyncDir = DecsyncPrefUtils.getDecsyncDir(requireActivity())
        if (decsyncDir != null) {
            val name = DecsyncPrefUtils.getNameFromUri(requireActivity(), decsyncDir)
            button.text = name
        }
        button.setOnClickListener {
            DecsyncPrefUtils.chooseDecsyncDir(this)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        DecsyncPrefUtils.chooseDecsyncDirResult(requireActivity(), requestCode, resultCode, data) { uri ->
            val name = DecsyncPrefUtils.getNameFromUri(requireActivity(), uri)
            intro_directory_button.text = name
        }
    }

    override val isPolicyRespected: Boolean
        get() = DecsyncPrefUtils.getDecsyncDir(requireActivity()) != null

    override fun onUserIllegallyRequestedNextPage() {
        Toast.makeText(requireActivity(), R.string.intro_directory_select, Toast.LENGTH_SHORT).show()
    }
}