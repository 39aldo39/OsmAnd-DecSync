package org.decsync.osmand.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites",
    indices = [(Index(value = ["favId"], unique = true))])
data class DecsyncFavorite(
    @PrimaryKey val favId: String,
    var lat: Double, var lon: Double,
    var name: String, var description: String?,
    var catId: String?
) {
    companion object {
        fun default(favId: String): DecsyncFavorite = DecsyncFavorite(favId, 0.0, 0.0, favId, null, null)
    }

    @ExperimentalStdlibApi
    fun getMapsFavorite(): Maps.Favorite {
        return Maps.Favorite(favId, lat, lon, name, description, catId) { catId }
    }
}