package org.decsync.osmand

import android.os.Environment
import android.util.Log
import org.decsync.library.Diff
import org.decsync.osmand.external.Algorithms
import org.decsync.osmand.external.ColorDialogs
import org.decsync.osmand.external.GPXUtilities
import org.decsync.osmand.model.OsmandFavorite
import java.io.File

object Utils {
    fun <T, V> Diff.Result<T>.map(transform: (T) -> V): Diff.Result<V> {
        return Diff.Result(
            insertions.map(transform),
            deletions.map(transform),
            changes.map { (x, y) -> Pair(transform(x), transform(y)) }
        )
    }

    fun getLastOsmandUpdate(lastOsmandProcessedUpdate: Long): Long {
        return lastOsmandProcessedUpdate + 1 // TODO, used for testing
    }

    fun getOsmandFavorites(): List<OsmandFavorite> {
        val file = File("${Environment.getExternalStorageDirectory()}/DecSync/favourites.gpx") // TODO, used for testing
        if (!file.exists()) {
            throw Exception("Favorites file $file does not exist")
        }
        val res = GPXUtilities.loadGPXFile(file)
        if (res.error != null) {
            throw Exception("Failed to load favorites file", res.error)
        }
        return res.points.map { wpt ->
            val colorTag = nearestTagForColorInt(wpt.color)
            val visible = !wpt.extensionsToRead.containsKey("hidden")
            OsmandFavorite(wpt.lat, wpt.lon, wpt.name, wpt.desc, wpt.category, colorTag, visible)
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
}