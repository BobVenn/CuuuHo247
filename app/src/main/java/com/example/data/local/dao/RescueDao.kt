package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RescueRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RescueDao {
    @Query("SELECT * FROM rescue_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<RescueRequestEntity>>

    @Query("SELECT * FROM rescue_requests WHERE status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY timestamp DESC LIMIT 1")
    fun getActiveRequest(): Flow<RescueRequestEntity?>

    @Query("SELECT * FROM rescue_requests WHERE id = :id")
    suspend fun getRequestById(id: Long): RescueRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RescueRequestEntity): Long

    @Update
    suspend fun updateRequest(request: RescueRequestEntity)

    @Query("UPDATE rescue_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE rescue_requests SET estimatedDistanceKm = :distance, estimatedMinutes = :minutes WHERE id = :id")
    suspend fun updateTracking(id: Long, distance: Double, minutes: Int)

    @Query("DELETE FROM rescue_requests WHERE id = :id")
    suspend fun deleteById(id: Long)
}
