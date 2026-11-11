
package com.ajaz.calculator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {

    @Insert
    suspend fun insertCalculation(
        calculation: Calculation
    )

    @Query(
        "SELECT * FROM calculations ORDER BY timestamp DESC"
    )
    fun getAllCalculations(): Flow<List<Calculation>>

    @Query("DELETE FROM calculations")
    suspend fun deleteAllCalculations()

    @Query("DELETE FROM calculations WHERE id = :id")
    suspend fun deleteCalculation(id: Int)
}
