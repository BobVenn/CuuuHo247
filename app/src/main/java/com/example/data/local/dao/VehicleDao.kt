package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavedVehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM saved_vehicles ORDER BY isDefault DESC, id DESC")
    fun getAllVehicles(): Flow<List<SavedVehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: SavedVehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: SavedVehicleEntity)

    @Query("DELETE FROM saved_vehicles WHERE id = :id")
    suspend fun deleteVehicle(id: Long)

    @Query("UPDATE saved_vehicles SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE saved_vehicles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultVehicle(id: Long)
}
