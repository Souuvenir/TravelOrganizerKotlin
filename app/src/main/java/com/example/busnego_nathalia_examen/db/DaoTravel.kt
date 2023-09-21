package com.example.busnego_nathalia_examen.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DaoTravel {
    @Query("SELECT * FROM Travel ORDER BY orden")
    fun select(): List<Travel>

    @Query("SELECT count(*) FROM Travel")
    fun countAllDestinos(): Int

    @Insert
    fun insert(travel: Travel):Long

    @Delete
    fun delete(travel: Travel)
}