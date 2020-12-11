package org.decsync.osmand.model

import androidx.room.*

@Dao
abstract class FavoriteDao {
    @get:Query("SELECT * FROM favorites")
    abstract val all: List<DecsyncFavorite>

    @Query("SELECT * FROM favorites where favId = :favId")
    abstract fun findById(favId: String): DecsyncFavorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg favorites: DecsyncFavorite)

    @Update
    abstract fun update(vararg favorites: DecsyncFavorite)

    @Delete
    abstract fun delete(vararg favorites: DecsyncFavorite)

    @Query("DELETE FROM favorites")
    abstract fun deleteAll()
}