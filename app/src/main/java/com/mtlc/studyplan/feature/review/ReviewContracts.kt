package com.mtlc.studyplan.feature.review

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SectionStatUi(
    val section: String,
    val correct: Int,
    val total: Int,
    val avgSecPerQ: Int
)

@Immutable
@Serializable
data class MockResultUi(
    val correct: Int,
    val total: Int,
    val avgSecPerQ: Int,
    val perSection: List<SectionStatUi>,
    val wrongIds: List<Int>
)

