package com.github.ljarka.immich.android.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AssetEntity::class, MonthBucketEntity::class], version = 11)
abstract class ImagesDatabase : RoomDatabase() {
    abstract fun imagesDao(): ImagesDao
}