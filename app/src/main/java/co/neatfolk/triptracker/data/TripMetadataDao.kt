package co.neatfolk.triptracker.data

import androidx.room.*

@Dao
interface TripMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: TripMetadata): Long

    @Update
    suspend fun update(metadata: TripMetadata)

    @Query("SELECT * FROM trip_metadata ORDER BY capturedAtMs DESC LIMIT 1")
    suspend fun getLatest(): TripMetadata?

    @Query("SELECT * FROM trip_metadata WHERE capturedAtMs BETWEEN :fromMs AND :toMs ORDER BY capturedAtMs DESC LIMIT 1")
    suspend fun findByTimeWindow(fromMs: Long, toMs: Long): TripMetadata?

    @Query("SELECT * FROM trip_metadata WHERE mergedToTripId IS NULL ORDER BY capturedAtMs DESC")
    suspend fun getUnmerged(): List<TripMetadata>

    @Query("SELECT * FROM trip_metadata WHERE fareConfirmed = 0 ORDER BY capturedAtMs DESC LIMIT 1")
    suspend fun getLatestUnconfirmed(): TripMetadata?

    @Query("UPDATE trip_metadata SET actualFare = :fare, fareConfirmed = 1 WHERE id = :id")
    suspend fun confirmFare(id: Long, fare: Double)

    @Query("UPDATE trip_metadata SET mergedToTripId = :tripId WHERE id = :metadataId")
    suspend fun markMerged(metadataId: Long, tripId: Long)

    @Query("SELECT * FROM trip_metadata ORDER BY capturedAtMs DESC")
    suspend fun getAll(): List<TripMetadata>

    // v4.0-alpha: wipe all AS-captured metadata (trip records unaffected)
    @Query("DELETE FROM trip_metadata")
    suspend fun deleteAll()
}
