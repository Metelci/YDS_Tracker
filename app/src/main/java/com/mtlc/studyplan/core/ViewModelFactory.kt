package com.mtlc.studyplan.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Generic ViewModel factory that eliminates unchecked casts and provides type safety.
 *
 * Usage:
 * ```kotlin
 * class MyViewModelFactory(
 *     private val dependency1: Dep1,
 *     private val dependency2: Dep2
 * ) : ViewModelFactory<MyViewModel>({ MyViewModel(dependency1, dependency2) })
 * ```
 */
abstract class ViewModelFactory<T : ViewModel>(
    private val viewModelCreator: () -> T
) : ViewModelProvider.Factory {

    private val viewModelInstance: T by lazy { viewModelCreator() }

    final override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
        return when {
            modelClass.isInstance(viewModelInstance) -> {
                viewModelInstance as VM
            }
            else -> {
                throw IllegalArgumentException(
                    "Unknown ViewModel class: ${modelClass.name}. " +
                    "Expected: ${viewModelInstance::class.java.name}"
                )
            }
        }
    }
}

/**
 * Simplified factory for ViewModels with no dependencies.
 */
abstract class SimpleViewModelFactory<T : ViewModel>(
    private val viewModelClass: Class<T>,
    private val creator: () -> T
) : ViewModelProvider.Factory {

    final override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
        return when {
            modelClass.isAssignableFrom(viewModelClass) -> {
                creator() as VM
            }
            else -> {
                throw IllegalArgumentException(
                    "Unknown ViewModel class: ${modelClass.name}. " +
                    "Expected: ${viewModelClass.name}"
                )
            }
        }
    }
}

/**
 * Extension function for cleaner factory creation.
 */
inline fun <reified T : ViewModel> viewModelFactory(
    noinline creator: () -> T
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return when {
                modelClass.isAssignableFrom(T::class.java) -> {
                    creator() as VM
                }
                else -> {
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.name}. Expected: ${T::class.java.name}"
                    )
                }
            }
        }
    }
}