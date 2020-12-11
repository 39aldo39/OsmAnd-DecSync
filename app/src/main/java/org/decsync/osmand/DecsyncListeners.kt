package org.decsync.osmand

import android.util.Log
import kotlinx.serialization.json.*
import org.decsync.library.Decsync
import org.decsync.osmand.model.DecsyncCategory
import org.decsync.osmand.model.DecsyncFavorite
import org.decsync.osmand.model.DefaultDecsyncCategory
import org.decsync.osmand.model.FailedEntry

private const val TAG = "DecsyncListeners"

@ExperimentalStdlibApi
object DecsyncListeners {
    fun favoriteListener(path: List<String>, entries: List<Decsync.Entry>, extra: Extra) {
        Log.d(TAG, "Execute favorite entries in $path: $entries")
        val favId = path[0]
        val prevFavorite = extra.db.favoriteDao().findById(favId)
        val newFavorite = prevFavorite?.copy() ?: DecsyncFavorite.default(favId)
        val added = updateFavorite(newFavorite, entries)

        var categoryAdded: DecsyncCategory? = null
        val getNewCategory = {
            newFavorite.catId?.let { catId ->
                extra.db.categoryDao().findById(catId) ?: run {
                    DecsyncCategory.default(catId).also { category ->
                        extra.db.categoryDao().insert(category)
                        categoryAdded = category
                    }
                }
            } ?: DefaultDecsyncCategory
        }

        var success = true
        if (prevFavorite == null) {
            if (added == true) {
                val newCategory = getNewCategory()
                success = extra.aidlHelper?.addFavorite(
                    newFavorite.lat, newFavorite.lon, newFavorite.name, newFavorite.description, "",
                    newCategory.name, newCategory.colorTag, newCategory.visible
                ) ?: true
                if (success) {
                    extra.db.favoriteDao().insert(newFavorite)
                    extra.observer.applyDiff(
                        insertions = listOf(newFavorite.getMapsFavorite()),
                        isFromDecsyncListener = true
                    )
                } else {
                    Log.w(TAG, "Could not add favorite $newFavorite in OsmAnd")
                }
            } else {
                Log.i(TAG, "Unknown favorite $favId")
            }
        } else {
            val prevCategory = prevFavorite.catId?.let { catId ->
                extra.db.categoryDao().findById(catId) ?: DecsyncCategory.default(catId)
            } ?: DefaultDecsyncCategory
            if (added == false) {
                success = extra.aidlHelper?.removeFavorite(
                    prevFavorite.lat, prevFavorite.lon, prevFavorite.name, prevCategory.name
                ) ?: true
                if (success) {
                    extra.db.favoriteDao().delete(prevFavorite)
                } else {
                    Log.w(TAG, "Could not remove favorite $prevFavorite in OsmAnd")
                }
            } else {
                if (newFavorite != prevFavorite) {
                    val newCategory = getNewCategory()
                    success = extra.aidlHelper?.updateFavorite(
                        prevFavorite.lat, prevFavorite.lon, prevFavorite.name, prevCategory.name,
                        newFavorite.lat, newFavorite.lon, newFavorite.name, newFavorite.description,
                        newCategory.name, newCategory.colorTag, newCategory.visible
                    ) ?: true
                    if (success) {
                        extra.db.favoriteDao().update(newFavorite)
                    } else {
                        Log.w(TAG, "Could not update favorite $newFavorite in OsmAnd")
                    }
                }
            }
        }

        val failedEntries = entries.map { FailedEntry.fromFavoriteEntry(favId, it.key) }
        if (success) {
            extra.db.failedEntryDao().delete(*failedEntries.toTypedArray())

            // Execute properties of new category
            categoryAdded?.let { category ->
                extra.db.categoryDao().insert(category)
                extra.observer.applyDiff(
                    insertions = listOf(category.getMapsCategory()),
                    isFromDecsyncListener = true
                )
            }
        } else {
            extra.db.failedEntryDao().insert(*failedEntries.toTypedArray())
        }
    }

    private fun updateFavorite(favorite: DecsyncFavorite, entries: List<Decsync.Entry>): Boolean? {
        var added: Boolean? = null
        for (entry in entries) {
            when (val key = entry.key.jsonPrimitive.contentOrNull) {
                null -> added = entry.value.jsonPrimitive.boolean
                "position" -> {
                    val position = entry.value.jsonArray
                    favorite.lat = position[0].jsonPrimitive.double
                    favorite.lon = position[1].jsonPrimitive.double
                }
                "name" -> favorite.name = entry.value.jsonPrimitive.content
                "description" -> favorite.description = entry.value.jsonPrimitive.contentOrNull
                "category" -> favorite.catId = entry.value.jsonPrimitive.contentOrNull
                else -> Log.w(TAG, "Unknown key for category: $key")
            }
        }
        return added
    }

    fun categoryListener(path: List<String>, entries: List<Decsync.Entry>, extra: Extra) {
        Log.d(TAG, "Execute category entries in $path: $entries")
        val catId = path[0]
        val prevCategory = extra.db.categoryDao().findById(catId) ?: run {
            Log.i(TAG, "Unknown category")
            return
        }
        val newCategory = prevCategory.copy()
        updateCategory(newCategory, entries)

        var success = true
        if (newCategory != prevCategory) {
            success = extra.aidlHelper?.updateFavoriteGroup(
                prevCategory.name, prevCategory.colorTag, prevCategory.visible,
                newCategory.name, newCategory.colorTag, newCategory.visible
            ) ?: true
            if (success) {
                extra.db.categoryDao().update(newCategory)
            } else {
                Log.w(TAG, "Could not update category $newCategory in OsmAnd")
            }
        }

        val failedEntries = entries.map { FailedEntry.fromCategoryEntry(catId, it.key) }
        if (success) {
            extra.db.failedEntryDao().delete(*failedEntries.toTypedArray())
        } else {
            extra.db.failedEntryDao().insert(*failedEntries.toTypedArray())
        }
    }

    private fun updateCategory(category: DecsyncCategory, entries: List<Decsync.Entry>) {
        for (entry in entries) {
            when (val key = entry.key.jsonPrimitive.content) {
                "name" -> category.name = entry.value.jsonPrimitive.content
                "color" -> {
                    val colorString = entry.value.jsonPrimitive.content
                    category.colorTag = Utils.nearestTagForColorString(colorString)
                }
                "visible" -> category.visible = entry.value.jsonPrimitive.boolean
                else -> Log.w(TAG, "Unknown key for category: $key")
            }
        }
    }
}