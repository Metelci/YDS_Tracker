package com.mtlc.studyplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Bu yardımcı sınıf, PlanViewModel'in nasıl oluşturulacağını tanımlar.
 * Bu sayede ViewModel'i oluşturma mantığını MainActivity'den ayırarak
 * kodumuzu daha temiz ve yeniden kullanılabilir hale getiririz.
 */
class PlanViewModelFactory(private val repository: ProgressRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
