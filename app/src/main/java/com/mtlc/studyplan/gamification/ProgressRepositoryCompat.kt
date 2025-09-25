package com.mtlc.studyplan.gamification

// Bridge to repository-layer singleton used by achievement/gamification subsystems
val progressRepository: com.mtlc.studyplan.repository.ProgressRepository
    get() = com.mtlc.studyplan.repository.progressRepository

