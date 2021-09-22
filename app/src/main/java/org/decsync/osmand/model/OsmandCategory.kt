package org.decsync.osmand.model

sealed class OsmandCategoryOrDefault {
    abstract val name: String
    abstract val colorTag: String
    abstract val visible: Boolean
}

data class OsmandCategory(
    override val name: String, override val colorTag: String, override val visible: Boolean
) : OsmandCategoryOrDefault() {
    fun toDecsyncCategory(id: String): DecsyncCategory {
        return DecsyncCategory(id, name, colorTag, visible)
    }
}

object DefaultOsmandCategory : OsmandCategoryOrDefault() {
    override val name = "Favorites"
    override val colorTag = "yellow"
    override val visible = true
}