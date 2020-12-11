package org.decsync.osmand.model

import androidx.room.*

@Dao
abstract class FailedEntryDao {
    @get:Query("SELECT * FROM failed_entries")
    abstract val all: List<FailedEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(vararg categories: FailedEntry)

    @Delete
    abstract fun delete(vararg categories: FailedEntry)

    @Query("DELETE FROM failed_entries")
    abstract fun deleteAll()
}