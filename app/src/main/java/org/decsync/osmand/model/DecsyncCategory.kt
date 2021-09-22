package org.decsync.osmand.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.decsync.osmand.Utils

sealed class DecsyncCategoryOrDefault {
    abstract val name: String
    abstract val colorTag: String
    abstract val visible: Boolean
}

@Entity(tableName = "categories",
    indices = [(Index(value = ["catId"], unique = true))])
data class DecsyncCategory(
    @PrimaryKey val catId: String,
    override var name: String, override var colorTag: String, override var visible: Boolean
) : DecsyncCategoryOrDefault() {
    companion object {
        fun default(catId: String): DecsyncCategory = DecsyncCategory(catId, catId, "black", true)
    }

    @ExperimentalStdlibApi
    fun getMapsCategory(): Maps.Category {
        val colorString = Utils.colorStringForTag(colorTag)
        return Maps.Category(catId, name, colorString, visible)
    }
}

object DefaultDecsyncCategory : DecsyncCategoryOrDefault() {
    override val name = "Favorites"
    override val colorTag = "yellow"
    override val visible = true
}