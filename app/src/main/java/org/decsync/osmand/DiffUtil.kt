package org.decsync.osmand

import org.decsync.library.Diff
import org.decsync.osmand.model.*
import java.util.*

fun getDiffResults(
    decsyncFavorites: List<DecsyncFavorite>,
    decsyncCategories: List<DecsyncCategory>,
    osmandFavorites: List<OsmandFavorite>
): Pair<Diff.Result<DecsyncFavorite>, Diff.Result<DecsyncCategory>> {
    val (osmandCategories, favoritePairing, categoryPairing) = pairDecsyncOsmand(decsyncFavorites, decsyncCategories, osmandFavorites)

    val categoryInsertions = mutableListOf<DecsyncCategory>()
    val categoryDeletions = decsyncCategories.toMutableList()
    val categoryChanges = mutableListOf<Pair<DecsyncCategory, DecsyncCategory>>()
    for (osmandCategory in osmandCategories) {
        val decsyncCategory = categoryPairing[osmandCategory.name]
        if (decsyncCategory == null) {
            // Insertion
            val newId = UUID.randomUUID().toString()
            val newDecsyncCategory = osmandCategory.toDecsyncCategory(newId)
            categoryPairing[osmandCategory.name] = newDecsyncCategory
            categoryInsertions += newDecsyncCategory
        } else {
            // Change
            categoryDeletions.remove(decsyncCategory)
            val newDecsyncCategory = osmandCategory.toDecsyncCategory(decsyncCategory.catId)
            if (decsyncCategory != newDecsyncCategory) {
                categoryChanges += decsyncCategory to newDecsyncCategory
            }
        }
    }
    val categoryResult = Diff.Result(categoryInsertions, categoryDeletions, categoryChanges)

    val favoriteInsertions = mutableListOf<DecsyncFavorite>()
    val favoriteDeletions = decsyncFavorites.toMutableList()
    val favoriteChanges = mutableListOf<Pair<DecsyncFavorite, DecsyncFavorite>>()
    for (osmandFavorite in osmandFavorites) {
        val decsyncFavorite = favoritePairing[osmandFavorite.name]
        val osmandCategory = osmandFavorite.getCategory()
        val catId = if (osmandCategory is DefaultOsmandCategory) {
            null
        } else {
            categoryPairing[osmandFavorite.catName]!!.catId
        }
        if (decsyncFavorite == null) {
            // Insertion
            val newId = UUID.randomUUID().toString()
            val newDecsyncFavorite = osmandFavorite.toDecsyncFavorite(newId, catId)
            favoriteInsertions += newDecsyncFavorite
        } else {
            // Change
            favoriteDeletions.remove(decsyncFavorite)
            val newDecsyncFavorite = osmandFavorite.toDecsyncFavorite(decsyncFavorite.favId, catId)
            if (decsyncFavorite != newDecsyncFavorite) {
                favoriteChanges += decsyncFavorite to newDecsyncFavorite
            }
        }
    }
    val favoriteResult = Diff.Result(favoriteInsertions, favoriteDeletions, favoriteChanges)
    return Pair(favoriteResult, categoryResult)
}

private data class PairingResult(
    val osmandCategories: List<OsmandCategory>,
    val favoritePairing: MutableMap<String, DecsyncFavorite>,
    val categoryPairing: MutableMap<String, DecsyncCategory>
)

private fun pairDecsyncOsmand(
    decsyncFavorites: List<DecsyncFavorite>,
    decsyncCategories: List<DecsyncCategory>,
    osmandFavorites: List<OsmandFavorite>
): PairingResult {
    val osmandCategories = osmandFavorites.map { it.getCategory() }.filterIsInstance<OsmandCategory>().distinctBy { it.name }

    // We pair favorites based on two criteria:
    // 1. Their location
    // 2. Their name
    // First we pair them when (1) and (2) are equal, then when just (1) is equal and finally when just (2) is equal
    val sameLocation = { osmandFavorite: OsmandFavorite, decsyncFavorite: DecsyncFavorite ->
        osmandFavorite.lat == decsyncFavorite.lat &&
                osmandFavorite.lon == decsyncFavorite.lon
    }
    val sameFavoriteName = { osmandFavorite: OsmandFavorite, decsyncFavorite: DecsyncFavorite ->
        osmandFavorite.name == decsyncFavorite.name
    }
    val favoritePairing = pairTwoPreds(osmandFavorites, decsyncFavorites, { it.name }, sameLocation, sameFavoriteName)

    // We also pair categories based on two criteria:
    // 1. Their name
    // 2. Their color and their children (over half of them remain the same)
    // The order is the same as for favorites: first (1) and (2), then just (1) and finally just (2)
    val sameCategoryName = { osmandCategory: OsmandCategory, decsyncCategory: DecsyncCategory ->
        osmandCategory.name == decsyncCategory.name
    }
    val decsyncChildrenCount = mutableMapOf<String, Int>()
    val bothChildrenCount = mutableMapOf<Pair<String, String>, Int>()
    for (osmandFavorite in osmandFavorites) {
        val osmandCatName = osmandFavorite.catName
        val decsyncCatId = favoritePairing[osmandFavorite.name]?.catId ?: continue
        val key = Pair(osmandCatName, decsyncCatId)
        decsyncChildrenCount[decsyncCatId] = (decsyncChildrenCount[decsyncCatId] ?: 0) + 1
        bothChildrenCount[key] = (bothChildrenCount[key] ?: 0) + 1
    }
    val sameColorAndMostlySameChildren = { osmandCategory: OsmandCategory, decsyncCategory: DecsyncCategory ->
        val decsyncCount = decsyncChildrenCount[decsyncCategory.catId] ?: 0
        val bothCount = bothChildrenCount[Pair(osmandCategory.name, decsyncCategory.catId)] ?: 0
        osmandCategory.colorTag == decsyncCategory.colorTag && 2*bothCount > decsyncCount
    }
    val categoryPairing = pairTwoPreds(osmandCategories, decsyncCategories, { it.name }, sameCategoryName, sameColorAndMostlySameChildren)

    return PairingResult(osmandCategories, favoritePairing, categoryPairing)
}

private fun <T1, T2, V1> pairTwoPreds(
    list1: List<T1>,
    list2: List<T2>,
    getKey: (T1) -> V1,
    predicate1: (T1, T2) -> Boolean,
    predicate2: (T1, T2) -> Boolean,
): MutableMap<V1, T2> {
    val pairing = mutableMapOf<V1, T2>()
    val list1 = list1.toMutableList()
    val list2 = list2.toMutableList()
    pair(list1, list2, pairing, getKey) { oldItem, newItem ->
        predicate1(oldItem, newItem) && predicate2(oldItem, newItem)
    }
    pair(list1, list2, pairing, getKey) { oldItem, newItem ->
        predicate1(oldItem, newItem)
    }
    pair(list1, list2, pairing, getKey) { oldItem, newItem ->
        predicate2(oldItem, newItem)
    }
    return pairing
}

private fun <T1, T2, V1> pair(
    list1: MutableList<T1>,
    list2: MutableList<T2>,
    pairing: MutableMap<V1, T2>,
    getKey: (T1) -> V1,
    predicate: (T1, T2) -> Boolean
) {
    val iterator1 = list1.iterator()
    while (iterator1.hasNext()) {
        val item1 = iterator1.next()
        val iterator2 = list2.iterator()
        while (iterator2.hasNext()) {
            val item2 = iterator2.next()
            if (predicate(item1, item2)) {
                iterator1.remove()
                iterator2.remove()
                pairing[getKey(item1)] = item2
                break
            }
        }
    }
}