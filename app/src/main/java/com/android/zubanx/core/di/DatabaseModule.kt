package com.android.zubanx.core.di

import androidx.room.Room
import com.android.zubanx.data.local.db.ZubanDatabase
import com.android.zubanx.data.local.db.migration.MIGRATION_1_2
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<ZubanDatabase> {
        Room.databaseBuilder(
            androidContext(),
            ZubanDatabase::class.java,
            "zuban_database"
        ).addMigrations(MIGRATION_1_2).build()
    }
    single { get<ZubanDatabase>().translationDao() }
    single { get<ZubanDatabase>().favouriteDao() }
    single { get<ZubanDatabase>().dictionaryDao() }
    single { get<ZubanDatabase>().offlineLanguagePackDao() }
}
