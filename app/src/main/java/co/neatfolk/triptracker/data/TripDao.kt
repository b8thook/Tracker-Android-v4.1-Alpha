package co.neatfolk.triptracker.data

import androidx.room.*

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startMs DESC")
    suspend fun getAll(): List<Trip>

    @Query("SELECT * FROM trips WHERE date = :date ORDER BY startMs DESC")
    suspend fun getByDate(date: String): List<Trip>

    @Query("SELECT * FROM trips WHERE synced = 0 ORDER BY startMs DESC")
    suspend fun getUnsynced(): List<Trip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("UPDATE trips SET synced = 1")
    suspend fun markAllSynced()

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun count(): Int

    @Query("SELECT SUM(fare) FROM trips WHERE date = :date AND cancelled = 0 AND fare IS NOT NULL")
    suspend fun sumFareForDate(date: String): Double?

    @Query("SELECT COUNT(*) FROM trips WHERE date = :date AND cancelled = 0")
    suspend fun countForDate(date: String): Int
}
