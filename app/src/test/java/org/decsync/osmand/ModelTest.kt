package org.decsync.osmand

import org.junit.Assert.assertEquals
import org.decsync.library.Diff
import org.decsync.osmand.model.DecsyncCategory
import org.decsync.osmand.model.DecsyncFavorite
import org.decsync.osmand.model.OsmandFavorite
import org.junit.Test
import java.util.*

@ExperimentalStdlibApi
class ModelTest {
    @Test
    fun identical() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001"),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", "bar1", "foo", "black", true),
            OsmandFavorite(2.2, 22.2, "foo2", "bar2", "foo", "black", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val expectedFavoriteResult = Diff.Result<DecsyncFavorite>(
            emptyList(),
            emptyList(),
            emptyList()
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result<DecsyncCategory>(
            emptyList(),
            emptyList(),
            emptyList()
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun favoriteInsertionAndDeletion() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(2.2, 22.2, "foo2", "bar2", "foo", "black", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val newFavId = favoriteResult.insertions[0].favId // Randomly generated, value irrelevant
        val expectedFavoriteResult = Diff.Result(
            listOf(DecsyncFavorite(newFavId, 2.2, 22.2, "foo2", "bar2", "catID00001")),
            listOf(DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001")),
            emptyList()
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result<DecsyncCategory>(
            emptyList(),
            emptyList(),
            emptyList()
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun favoriteChangeLocationName() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001"),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(3.3, 33.3, "foo1", "bar1", "foo", "black", true),
            OsmandFavorite(2.2, 22.2, "foo3", "bar2", "foo", "black", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val expectedFavoriteResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001") to
                        DecsyncFavorite("favID00001", 3.3, 33.3, "foo1", "bar1", "catID00001"),
                DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001") to
                        DecsyncFavorite("favID00002", 2.2, 22.2, "foo3", "bar2", "catID00001")
            )
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result<DecsyncCategory>(
            emptyList(),
            emptyList(),
            emptyList()
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun categoryInsertionAndDeletion() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", "bar1", "new", "white", false)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val newCatId = categoryResult.insertions[0].catId
        val expectedFavoriteResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001") to
                    DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", newCatId)
            ),
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result(
            listOf(DecsyncCategory(newCatId, "new", "white", false)),
            listOf(DecsyncCategory("catID00001", "foo", "black", true)),
            emptyList()
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun categoryChangeName() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001"),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", "bar1", "baz", "black", true),
            OsmandFavorite(2.2, 22.2, "foo2", "bar2", "baz", "black", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val expectedFavoriteResult = Diff.Result<DecsyncFavorite>(
            emptyList(),
            emptyList(),
            emptyList()
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncCategory("catID00001", "foo", "black", true) to DecsyncCategory("catID00001", "baz", "black", true))
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun categoryChangeNameAndColor() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001"),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", "bar1", "baz", "white", true),
            OsmandFavorite(2.2, 22.2, "foo2", "bar2", "baz", "white", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val newCatId = categoryResult.insertions[0].catId
        val expectedFavoriteResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", "catID00001") to
                    DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", newCatId),
                DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001") to
                        DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", newCatId))
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result(
            listOf(DecsyncCategory(newCatId, "baz", "white", true)),
            listOf(DecsyncCategory("catID00001", "foo", "black", true)),
            emptyList()
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun categoryChangeChildren() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", null, "catID00001"),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", null, "catID00001"),
            DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", null, "catID00001"),
            DecsyncFavorite("favID00004", 4.4, 44.4, "foo4", null, "catID00002")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "bar1", "black", true),
            DecsyncCategory("catID00002", "bar2", "white", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", null, "baz1", "black", true),
            OsmandFavorite(2.2, 22.2, "foo2", null, "baz1", "black", true),
            OsmandFavorite(3.3, 33.3, "foo3", null, "baz2", "white", true),
            OsmandFavorite(4.4, 44.4, "foo4", null, "baz2", "white", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val expectedFavoriteResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", null, "catID00001") to
                    DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", null, "catID00002"))
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncCategory("catID00001", "bar1", "black", true) to
                    DecsyncCategory("catID00001", "baz1", "black", true),
                DecsyncCategory("catID00002", "bar2", "white", true) to
                        DecsyncCategory("catID00002", "baz2", "white", true))
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun categoryChangeTooManyChildren() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", null, "catID00001"),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", null, "catID00001"),
            DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", null, "catID00001"),
            DecsyncFavorite("favID00004", 4.4, 44.4, "foo4", null, "catID00002")
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "bar1", "black", true),
            DecsyncCategory("catID00002", "bar2", "white", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", null, "baz1", "black", true),
            OsmandFavorite(2.2, 22.2, "foo2", null, "baz2", "white", true),
            OsmandFavorite(3.3, 33.3, "foo3", null, "baz2", "white", true),
            OsmandFavorite(4.4, 44.4, "foo4", null, "baz2", "white", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val newCatId = categoryResult.insertions[0].catId
        val expectedFavoriteResult = Diff.Result(
            emptyList(),
            emptyList(),
            listOf(DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", null, "catID00001") to
                    DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", null, newCatId),
                DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", null, "catID00001") to
                    DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", null, "catID00002"),
                DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", null, "catID00001") to
                        DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", null, "catID00002"))
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result(
            listOf(DecsyncCategory(newCatId, "baz1", "black", true)),
            listOf(DecsyncCategory("catID00001", "bar1", "black", true)),
            listOf(DecsyncCategory("catID00002", "bar2", "white", true) to
                        DecsyncCategory("catID00002", "baz2", "white", true))
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }

    @Test
    fun nullCategory() {
        val decsyncFavorites = listOf(
            DecsyncFavorite("favID00001", 1.1, 11.1, "foo1", "bar1", null),
            DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", null),
            DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", "bar3", "catID00001"),
        )
        val decsyncCategories = listOf(
            DecsyncCategory("catID00001", "foo", "black", true)
        )
        val osmandFavorites = listOf(
            OsmandFavorite(1.1, 11.1, "foo1", "bar1", "default", "black", true),
            OsmandFavorite(2.2, 22.2, "foo2", "bar2", "foo", "black", true),
            OsmandFavorite(3.3, 33.3, "foo3", "bar3", "default", "black", true)
        )
        val (favoriteResult, categoryResult) = getDiffResults(decsyncFavorites, decsyncCategories, osmandFavorites)
        val expectedFavoriteResult = Diff.Result<DecsyncFavorite>(
            emptyList(),
            emptyList(),
            listOf(
                DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", null) to
                        DecsyncFavorite("favID00002", 2.2, 22.2, "foo2", "bar2", "catID00001"),
                DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", "bar3", "catID00001") to
                        DecsyncFavorite("favID00003", 3.3, 33.3, "foo3", "bar3", null)
            )
        )
        assertEquals(expectedFavoriteResult, favoriteResult)
        val expectedCategoryResult = Diff.Result<DecsyncCategory>(
            emptyList(),
            emptyList(),
            emptyList()
        )
        assertEquals(expectedCategoryResult, categoryResult)
    }
}