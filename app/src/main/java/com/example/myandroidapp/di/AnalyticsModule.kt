package com.example.myandroidapp.di

import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.analytics.CompositeAnalyticsHelper
import com.example.myandroidapp.analytics.FirebaseAnalyticsHelper
import com.example.myandroidapp.analytics.TimberAnalyticsHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun bindAnalyticsHelper(
        firebase: FirebaseAnalyticsHelper,
        timber: TimberAnalyticsHelper,
    ): AnalyticsHelper {
        return CompositeAnalyticsHelper(listOf(firebase, timber))
    }
}
