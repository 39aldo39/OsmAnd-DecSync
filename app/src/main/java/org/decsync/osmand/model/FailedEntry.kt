package org.decsync.osmand.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.decsync.library.Decsync

@Entity(tableName = "failed_entries",
    primaryKeys = ["path0", "path1", "key"])
data class FailedEntry(
    val path0: String,
    val path1: String,
    val key: String
) {
    @ExperimentalStdlibApi
    fun getStoredEntry(): Decsync.StoredEntry {
        val path = listOf(path0, path1)
        return Decsync.StoredEntry(path, Json.parseToJsonElement(key))
    }

    companion object {
        fun fromFavoriteEntry(favId: String, key: JsonElement): FailedEntry {
            return FailedEntry("favorites", favId, key.toString())
        }

        fun fromCategoryEntry(catId: String, key: JsonElement): FailedEntry {
            return FailedEntry("categories", catId, key.toString())
        }
    }
}