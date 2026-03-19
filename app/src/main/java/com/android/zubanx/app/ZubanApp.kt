package com.android.zubanx.app

import android.app.Application
import com.android.zubanx.BuildConfig
import com.android.zubanx.core.di.databaseModule
import com.android.zubanx.core.di.dataStoreModule
import com.android.zubanx.core.di.mlKitModule
import com.android.zubanx.core.di.networkModule
import com.android.zubanx.core.di.repositoryModule
import com.android.zubanx.core.di.securityModule
import com.android.zubanx.core.di.ttsModule
import com.android.zubanx.core.di.useCaseModule
import com.android.zubanx.core.di.utilsModule
import com.android.zubanx.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class ZubanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initKoin()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initKoin() {
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ZubanApp)
            modules(
                networkModule,
                databaseModule,
                dataStoreModule,
                securityModule,
                utilsModule,
                mlKitModule,
                repositoryModule,
                useCaseModule,
                viewModelModule,
                ttsModule
            )
        }
    }
}