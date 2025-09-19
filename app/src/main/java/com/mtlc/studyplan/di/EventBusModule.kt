package com.mtlc.studyplan.di

import com.mtlc.studyplan.eventbus.EventBus
import com.mtlc.studyplan.eventbus.ReactiveEventBus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventBusModule {

    @Binds
    @Singleton
    abstract fun bindEventBus(
        reactiveEventBus: ReactiveEventBus
    ): EventBus
}