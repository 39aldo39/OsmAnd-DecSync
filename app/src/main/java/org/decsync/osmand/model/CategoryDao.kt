package org.decsync.osmand.model

import androidx.room.*

@Dao
abstract class CategoryDao {
    @get:Query("SELECT * FROM categories")
    abstract val all: List<DecsyncCategory>

    @Query("SELECT * FROM categories where catId = :catId")
    abstract fun findById(catId: String): DecsyncCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg categories: DecsyncCategory)

    @Update
    abstract fun update(vararg categories: DecsyncCategory)

    @Delete
    abstract fun delete(vararg categories: DecsyncCategory)

    @Query("DELETE FROM categories")
    abstract fun deleteAll()
}