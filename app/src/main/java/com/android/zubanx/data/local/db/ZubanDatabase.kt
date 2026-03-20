package com.android.zubanx.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.zubanx.data.local.db.converter.ListStringConverter
import com.android.zubanx.data.local.db.dao.*
import com.android.zubanx.data.local.db.entity.*

@Database(
    entities = [
        TranslationEntity::class,
        FavouriteEntity::class,
        DictionaryEntity::class,
        OfflineLanguagePackEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(ListStringConverter::class)
abstract class ZubanDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun offlineLanguagePackDao(): OfflineLanguagePackDao
}
