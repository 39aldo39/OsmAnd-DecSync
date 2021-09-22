package org.decsync.osmand

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.decsync.library.*
import org.decsync.osmand.Utils.map
import org.decsync.osmand.external.OsmAndAidlHelper
import org.decsync.osmand.model.AppDatabase

private const val TAG = "DecsyncUtils"
private val ownAppId = getAppId("OsmAnd")
private const val ERROR_NOTIFICATION_ID = 1

@ExperimentalStdlibApi
data class Extra(
    val db: AppDatabase,
    val aidlHelper: OsmAndAidlHelper?,
    val observer: MyDecsyncObserver
)

object OsmandMissingException : Exception()

@ExperimentalStdlibApi
class MyDecsyncObserver(
    private val decsync: Decsync<Extra>,
    db: AppDatabase,
    aidlHelper: OsmAndAidlHelper?
) : DecsyncObserver() {
    private val extra = Extra(db, aidlHelper, this)
    override fun isDecsyncEnabled(): Boolean = true // Observer only used for applyDiff

    override fun setEntries(entries: List<Decsync.EntryWithPath>) {
        decsync.setEntries(entries)
    }

    override fun executeStoredEntries(storedEntries: List<Decsync.StoredEntry>) {
        decsync.executeStoredEntries(storedEntries, extra)
    }

    fun initStoredEntries() {
        decsync.initStoredEntries()
    }

    fun executeAllStoredEntries() {
        decsync.executeStoredEntriesForPathPrefix(emptyList(), extra)
    }

    fun executeAllNewEntries() {
        decsync.executeAllNewEntries(extra)
    }
}

@ExperimentalStdlibApi
class DecsyncWorker(private val context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (!PrefUtils.getDecsyncEnabled(context)) return Result.success()
        val isInitSync = inputData.getBoolean(KEY_IS_INIT_SYNC, false)
        Log.d(TAG, "Sync started")
        try {
            val decsyncDir = DecsyncPrefUtils.getDecsyncDir(context) ?: throw Exception(context.getString(R.string.settings_decsync_dir_not_configured))
            val decsync = Decsync<Extra>(context, decsyncDir, "maps", null, ownAppId).apply {
                addMultiListenerWithSuccess(listOf("favorites"), DecsyncListeners::favoriteListener)
                addMultiListenerWithSuccess(listOf("categories"), DecsyncListeners::categoryListener)
            }
            val db = AppDatabase.createDatabase(context)
            val aidlHelper = OsmAndAidlHelper(context) {
                throw OsmandMissingException
            }
            // TODO: check whether the plugin is enabled
            val myDecsyncObserver = MyDecsyncObserver(decsync, db, aidlHelper)

            if (isInitSync) {
                Log.d(TAG, "Executing init sync")

                // Delete all OsmAnd data, so we start fresh
                db.favoriteDao().deleteAll()
                db.categoryDao().deleteAll()

                // Populate the database with the DecSync data, so we can get the right mapping
                // between the ids, otherwise all OsmAnd data would be considered insertions.
                MyDecsyncObserver(decsync, db, null /* Only sync the database */).apply {
                    initStoredEntries()
                    executeAllStoredEntries()
                }

                // Update the database to reflect the OsmAnd state, but only write insertions to DecSync
                writeOsmandUpdates(db, myDecsyncObserver, true)

                // Execute all the DecSync updates
                myDecsyncObserver.executeAllStoredEntries()

            } else {
                try {
                    // Write the OsmAnd updates
                    writeOsmandUpdates(db, myDecsyncObserver)

                    // Execute the DecSync updates
                    myDecsyncObserver.executeAllNewEntries()
                } catch (e: Exception) {
                    Log.w(TAG, e)
                    return Result.failure()
                }
            }
            return Result.success()
        } catch (e: OsmandMissingException) {
            val intent = Intent()
            intent.data = Uri.parse("market://details?id=net.osmand.plus")
            showException(Exception(context.getString(R.string.install_osmand_dialog_message)), intent)
            return Result.failure()
        } catch (e: Exception) {
            showException(e, Intent(context, SettingsActivity::class.java))
            return Result.failure()
        }
    }

    private fun writeOsmandUpdates(db: AppDatabase, myDecsyncObserver: MyDecsyncObserver, isInitSync: Boolean = false) {
        val lastOsmandProcessedUpdate = PrefUtils.getLastProcessedOsmandUpdate(context)
        val lastOsmandUpdate = Utils.getLastOsmandUpdate(context)
        if (!isInitSync && lastOsmandProcessedUpdate >= lastOsmandUpdate) return
        val osmandFavorites = Utils.getOsmandFavorites(context)

        val decsyncFavorites = db.favoriteDao().all
        val decsyncCategories = db.categoryDao().all
        val (favoriteResult, categoryResult) = getDiffResults(
            decsyncFavorites,
            decsyncCategories,
            osmandFavorites
        )
        Log.d(TAG, "Updating favorite db")
        db.favoriteDao().insert(*favoriteResult.insertions.toTypedArray())
        db.favoriteDao().delete(*favoriteResult.deletions.toTypedArray())
        db.favoriteDao().update(*favoriteResult.changes.map { it.second }.toTypedArray())
        val mapsFavoriteResult = favoriteResult.map { it.getMapsFavorite() }
        if (isInitSync) {
            Log.d(TAG, "Processing favorite insertions: ${favoriteResult.insertions}")
            myDecsyncObserver.applyDiff(insertions = mapsFavoriteResult.insertions)
        } else {
            Log.d(TAG, "Processing favoriteResult: $favoriteResult")
            myDecsyncObserver.applyDiff(mapsFavoriteResult)
        }
        Log.d(TAG, "Updating category db")
        db.categoryDao().insert(*categoryResult.insertions.toTypedArray())
        db.categoryDao().delete(*categoryResult.deletions.toTypedArray())
        db.categoryDao().update(*categoryResult.changes.map { it.second }.toTypedArray())
        val mapsCategoryResult = categoryResult.map { it.getMapsCategory() }
        if (isInitSync) {
            Log.d(TAG, "Processing category insertions: ${categoryResult.insertions}")
            myDecsyncObserver.applyDiff(insertions = mapsCategoryResult.insertions)
        } else {
            Log.d(TAG, "Processing categoryResult: $categoryResult")
            myDecsyncObserver.applyDiff(mapsCategoryResult)
        }

        PrefUtils.setLastProcessedOsmandUpdate(context, lastOsmandUpdate)
    }

    private fun showException(e: Exception, intent: Intent) {
        Log.e(TAG, "", e)
        PrefUtils.setDecsyncEnabled(context, false)

        val channelId = "channel_error"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.channel_error_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.mipmap.ic_launcher
                )
            )
            .setContentTitle(context.getString(R.string.decsync_disabled))
            .setContentText(e.localizedMessage)
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
            .build()
        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_IS_INIT_SYNC = "KEY_IS_INIT_SYNC"
    }
}