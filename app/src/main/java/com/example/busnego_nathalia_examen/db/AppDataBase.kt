package com.example.busnego_nathalia_examen.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Travel::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract fun daoTravel (): DaoTravel

    companion object{

        @Volatile
        private var DATA_BASE: AppDataBase? = null

        fun getInstance(context : Context) : AppDataBase {

            return DATA_BASE ?: synchronized(this){
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDataBase::class.java,
                    "Viaje.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { DATA_BASE = it }
            }
        }
    }
}
