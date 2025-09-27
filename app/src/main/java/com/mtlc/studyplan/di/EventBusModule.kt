package com.mtlc.studyplan.di

import com.mtlc.studyplan.eventbus.AppEventBus
import com.mtlc.studyplan.eventbus.EventBus
import com.mtlc.studyplan.eventbus.ReactiveEventBus
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.module

val koinEventBusModule = module {
    single<EventBus> { ReactiveEventBus(get<CoroutineScope>(named("ApplicationScope"))) }
    single { AppEventBus() }
}
