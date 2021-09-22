package org.decsync.osmand

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.decsync.library.Diff
import org.decsync.osmand.external.Algorithms
import org.decsync.osmand.external.ColorDialogs
import org.decsync.osmand.external.GPXUtilities
import org.decsync.osmand.model.DefaultOsmandCategory
import org.decsync.osmand.model.OsmandFavorite

object Utils {
    fun <T, V> Diff.Result<T>.map(transform: (T) -> V): Diff.Result<V> {
        return Diff.Result(
            insertions.map(transform),
            deletions.map(transform),
            changes.map { (x, y) -> Pair(transform(x), transform(y)) }
        )
    }

    fun getLastOsmandUpdate(context: Context): Long {
        val uri = PrefUtils.getOsmandFavoritesUri(context) ?: throw Exception("No favourites.gpx configured")
        return DocumentFile.fromSingleUri(context, uri)!!.lastModified()
    }

    fun getOsmandFavorites(context: Context): List<OsmandFavorite> {
        val uri = PrefUtils.getOsmandFavoritesUri(context) ?: throw Exception("No favourites.gpx configured")
        val res = context.contentResolver.openInputStream(uri)?.use { input ->
            GPXUtilities.loadGPXFile(input)
        } ?: throw Exception("Could not open input stream for OsmAnd's favourites.gpx")
        if (res.error != null) {
            throw Exception("Failed to load favorites file", res.error)
        }
        return res.points.map { wpt ->
            val catName = wpt.category ?: DefaultOsmandCategory.name
            val colorTag = nearestTagForColorInt(wpt.color)
            val visible = !wpt.extensionsToRead.containsKey("hidden")
            OsmandFavorite(wpt.lat, wpt.lon, wpt.name, wpt.desc, catName, colorTag, visible)
        }
    }

    fun nearestTagForColorString(colorString: String): String {
        val colorInt = Algorithms.parseColor(colorString)
        return nearestTagForColorInt(colorInt)
    }

    fun nearestTagForColorInt(colorInt: Int): String {
        val colorOsmandInt = ColorDialogs.getNearestColor(colorInt, ColorDialogs.pallette)
        val colorOsmandIndex = ColorDialogs.pallette.indexOf(colorOsmandInt)
        return ColorDialogs.paletteColorTags[colorOsmandIndex]
    }

    fun colorStringForTag(colorTag: String): String {
        val colorInt = ColorDialogs.getColorByTag(colorTag)
        return Algorithms.colorToString(colorInt)
    }

    fun appInstalled(activity: Activity, packageName: String): Boolean {
        return try {
            activity.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun installApp(activity: Activity, packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=$packageName")
        if (intent.resolveActivity(activity.packageManager) == null) {
            Toast.makeText(activity, R.string.no_app_store, Toast.LENGTH_SHORT).show()
            return
        }
        activity.startActivity(intent)
    }
}