package com.mtlc.studyplan.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.PlanDurationSettings
import com.mtlc.studyplan.data.PlanSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class SkillWeights(
    val grammar: Float = 1.0f,
    val reading: Float = 1.0f,
    val listening: Float = 1.0f,
    val vocab: Float = 1.0f,
)

class OnboardingViewModel(
    private val onboardingRepo: OnboardingRepository,
    private val planSettings: PlanSettingsStore,
) : ViewModel() {
    private val _examDate = MutableStateFlow(LocalDate.now().plusWeeks(4))
    val examDate: StateFlow<LocalDate> = _examDate

    private val _availability = MutableStateFlow(
        DayOfWeek.values().associateWith { day ->
            when (day) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 120
                else -> 60
            }
        }
    )
    val availability: StateFlow<Map<DayOfWeek, Int>> = _availability

    private val _skillWeights = MutableStateFlow(SkillWeights())
    val skillWeights: StateFlow<SkillWeights> = _skillWeights

    fun setExamDate(date: LocalDate) { _examDate.value = date }
    fun setAvailability(day: DayOfWeek, minutes: Int) {
        _availability.value = _availability.value.toMutableMap().apply { this[day] = minutes.coerceIn(0, 180) }
    }
    fun setSkillWeights(next: SkillWeights) { _skillWeights.value = next }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            // Persist plan settings
            val avail = _availability.value
            planSettings.update { cur ->
                cur.copy(
                    startEpochDay = LocalDate.now().toEpochDay(),
                    endEpochDay = _examDate.value.toEpochDay(),
                    totalWeeks = 30,
                    monMinutes = avail[DayOfWeek.MONDAY] ?: 60,
                    tueMinutes = avail[DayOfWeek.TUESDAY] ?: 60,
                    wedMinutes = avail[DayOfWeek.WEDNESDAY] ?: 60,
                    thuMinutes = avail[DayOfWeek.THURSDAY] ?: 60,
                    friMinutes = avail[DayOfWeek.FRIDAY] ?: 60,
                    satMinutes = avail[DayOfWeek.SATURDAY] ?: 120,
                    sunMinutes = avail[DayOfWeek.SUNDAY] ?: 120,
                )
            }
            // Persist profile bits to onboarding repo
            onboardingRepo.saveProfile(
                examEpochDay = _examDate.value.toEpochDay(),
                weights = _skillWeights.value
            )
            onboardingRepo.markOnboardingCompleted()
            onDone()
        }
    }
}

