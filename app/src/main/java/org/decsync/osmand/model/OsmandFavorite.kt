package org.decsync.osmand.model

data class OsmandFavorite(
    val lat: Double, val lon: Double,
    val name: String, val description: String?,
    val catName: String, val colorTag: String, val visible: Boolean
) {
    fun getCategory(): OsmandCategoryOrDefault {
        if (catName == DefaultOsmandCategory.name &&
                colorTag == DefaultOsmandCategory.colorTag &&
                visible == DefaultOsmandCategory.visible) {
            return DefaultOsmandCategory
        }
        return OsmandCategory(catName, colorTag, visible)
    }

    fun toDecsyncFavorite(id: String, catId: String?): DecsyncFavorite {
        return DecsyncFavorite(id, lat, lon, name, description, catId)
    }
}