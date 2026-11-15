package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    ClientEntity::class,
    PetEntity::class,
    ServiceEntity::class,
    ServicePriceEntity::class,
    AppointmentEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class PetSpaDatabase : RoomDatabase() {
  abstract fun petSpaDao(): PetSpaDao

  companion object {
    @Volatile
    private var INSTANCE: PetSpaDatabase? = null

    fun getInstance(context: Context): PetSpaDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          PetSpaDatabase::class.java,
          "pet_spa_database",
        ).build()
        INSTANCE = instance
        instance
      }
    }
  }
}
