package org.decsync.osmand.model

import kotlinx.serialization.json.*
import org.decsync.library.Decsync.StoredEntry
import org.decsync.library.DecsyncItem
import org.decsync.library.DecsyncItem.Value.Normal
import org.decsync.library.DecsyncItem.Value.Reference

@ExperimentalStdlibApi
object Maps {
    class Favorite(
        favId: String,
        lat: Double,
        lon: Double,
        name: String,
        description: String?,
        internalCatId: Any?,
        category: () -> String?
    ) : DecsyncItem {
        override val type = "MapsFavorite"
        override val id = favId
        override val idStoredEntry = StoredEntry(listOf("favorites", favId), JsonNull)

        private val positionValue = buildJsonArray {
            add(lat)
            add(lon)
        }
        private val defaultPosition = buildJsonArray {
            add(0)
            add(0)
        }
        override val entries = mapOf(
            StoredEntry(listOf("favorites", favId), JsonPrimitive("position")) to
                    Normal(positionValue, defaultPosition),
            StoredEntry(listOf("favorites", favId), JsonPrimitive("name")) to
                    Normal(JsonPrimitive(name), JsonPrimitive(favId)),
            StoredEntry(listOf("favorites", favId), JsonPrimitive("description")) to
                    Normal(JsonPrimitive(description), JsonNull),
            StoredEntry(listOf("favorites", favId), JsonPrimitive("category")) to
                    Reference(internalCatId) { JsonPrimitive(category()) }
        )
    }

    class Category(
        catId: String,
        name: String,
        color: String,
        visible: Boolean
    ) : DecsyncItem {
        override val type = "RssCategory"
        override val id = catId
        override val idStoredEntry: StoredEntry? = null
        override val entries = mapOf(
            StoredEntry(listOf("categories", catId), JsonPrimitive("name")) to
                    Normal(JsonPrimitive(name), JsonPrimitive(catId)),
            StoredEntry(listOf("categories", catId), JsonPrimitive("color")) to
                    Normal(JsonPrimitive(color), JsonPrimitive("#000000")),
            StoredEntry(listOf("categories", catId), JsonPrimitive("visible")) to
                    Normal(JsonPrimitive(visible), JsonPrimitive(true))
        )
    }
}