package com.mtlc.studyplan.performance

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mtlc.studyplan.notifications.PushNotificationManager
import com.mtlc.studyplan.offline.OfflineManager
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Battery optimization manager that handles Doze mode and low power scenarios
 * Adjusts app behavior to conserve battery when device is in power-saving modes
 */
@Singleton
class BatteryOptimizationManager @Inject constructor(
    private val context: Context,
    private val powerManager: PowerManager,
    private val performanceOptimizer: PerformanceOptimizer,
    private val pushNotificationManager: PushNotificationManager,
    private val offlineManager: OfflineManager,
    private val settingsManager: SettingsManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "BatteryOptimization"
        private const val DOZE_CHECK_INTERVAL = 30000L // 30 seconds
        private const val LOW_BATTERY_THRESHOLD = 20 // 20%
        private const val CRITICAL_BATTERY_THRESHOLD = 10 // 10%
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val batteryReceiver = BatteryOptimizationReceiver()

    // Battery and power state flows
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _powerSaveMode = MutableStateFlow(false)
    val powerSaveMode: StateFlow<Boolean> = _powerSaveMode.asStateFlow()

    private val _dozeMode = MutableStateFlow(false)
    val dozeMode: StateFlow<Boolean> = _dozeMode.asStateFlow()

    data class BatteryState(
        val level: Int = 100,
        val isCharging: Boolean = false,
        val isLowBattery: Boolean = false,
        val isCriticalBattery: Boolean = false,
        val temperature: Float = 25.0f, // Celsius
        val voltage: Int = 0
    )

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerBatteryReceiver()
        startDozeMonitoring()
        updateBatteryState()
    }

    /**
     * Register battery status receiver
     */
    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(batteryReceiver, filter)
    }

    /**
     * Start monitoring Doze mode state
     */
    private fun startDozeMonitoring() {
        scope.launch {
            while (true) {
                val isInDozeMode = powerManager.isDeviceIdleMode
                if (_dozeMode.value != isInDozeMode) {
                    _dozeMode.value = isInDozeMode
                    onDozeModeChanged(isInDozeMode)
                }
                kotlinx.coroutines.delay(DOZE_CHECK_INTERVAL)
            }
        }
    }

    /**
     * Update current battery state
     */
    private fun updateBatteryState() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else 50

            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL

            val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250) / 10.0f
            val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

            val newState = BatteryState(
                level = batteryLevel,
                isCharging = isCharging,
                isLowBattery = batteryLevel <= LOW_BATTERY_THRESHOLD,
                isCriticalBattery = batteryLevel <= CRITICAL_BATTERY_THRESHOLD,
                temperature = temperature,
                voltage = voltage
            )

            if (_batteryState.value != newState) {
                _batteryState.value = newState
                onBatteryStateChanged(newState)
            }
        }
    }

    /**
     * Handle Doze mode changes
     */
    private fun onDozeModeChanged(isInDozeMode: Boolean) {
        Log.i(TAG, "Doze mode changed: $isInDozeMode")

        if (isInDozeMode) {
            applyDozeOptimizations()
        } else {
            restoreNormalOperation()
        }
    }

    /**
     * Handle battery state changes
     */
    private fun onBatteryStateChanged(newState: BatteryState) {
        Log.i(TAG, "Battery state changed: ${newState.level}%, charging: ${newState.isCharging}")

        when {
            newState.isCriticalBattery -> applyCriticalBatteryOptimizations()
            newState.isLowBattery -> applyLowBatteryOptimizations()
            else -> restoreNormalOperation()
        }
    }

    /**
     * Apply optimizations when device enters Doze mode
     */
    private fun applyDozeOptimizations() {
        scope.launch {
            try {
                Log.i(TAG, "Applying Doze mode optimizations")

                // Reduce performance monitoring frequency
                performanceOptimizer.setMonitoringEnabled(false)

                // Disable push notifications during Doze
                pushNotificationManager.setDozeMode(true)

                // Pause non-essential background work
                offlineManager.pauseBackgroundSync()

                // Clear performance caches to free memory
                performanceOptimizer.performManualCleanup()

                // Reduce animation frame rates
                performanceOptimizer.setReducedFrameRate(true)

            } catch (e: SecurityException) {
                Log.e(TAG, "Security error applying Doze optimizations", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state when applying Doze optimizations", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid argument when applying Doze optimizations", e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Doze optimization was cancelled", e)
            }
        }
    }

    /**
     * Apply optimizations for low battery
     */
    private fun applyLowBatteryOptimizations() {
        scope.launch {
            try {
                Log.i(TAG, "Applying low battery optimizations")

                // Reduce background sync frequency
                offlineManager.setLowPowerMode(true)

                // Disable non-essential notifications
                pushNotificationManager.setLowPowerMode(true)

                // Reduce performance monitoring
                performanceOptimizer.setReducedMonitoring(true)

                // Clear caches more aggressively
                performanceOptimizer.performAggressiveCleanup()

            } catch (e: SecurityException) {
                Log.e(TAG, "Security error applying low battery optimizations", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state when applying low battery optimizations", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid argument when applying low battery optimizations", e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Low battery optimization was cancelled", e)
            }
        }
    }

    /**
     * Apply optimizations for critical battery level
     */
    private fun applyCriticalBatteryOptimizations() {
        scope.launch {
            try {
                Log.i(TAG, "Applying critical battery optimizations")

                // Disable all background operations
                offlineManager.setCriticalPowerMode(true)

                // Only allow essential notifications
                pushNotificationManager.setCriticalPowerMode(true)

                // Disable all performance monitoring
                performanceOptimizer.setMonitoringEnabled(false)

                // Clear all caches
                performanceOptimizer.clearAllCaches()

                // Disable animations
                performanceOptimizer.setAnimationsEnabled(false)

            } catch (e: SecurityException) {
                Log.e(TAG, "Security error applying critical battery optimizations", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state when applying critical battery optimizations", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid argument when applying critical battery optimizations", e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Critical battery optimization was cancelled", e)
            }
        }
    }

    /**
     * Restore normal operation when power conditions improve
     */
    private fun restoreNormalOperation() {
        scope.launch {
            try {
                Log.i(TAG, "Restoring normal operation")

                // Re-enable performance monitoring
                performanceOptimizer.setMonitoringEnabled(true)
                performanceOptimizer.setReducedMonitoring(false)

                // Restore normal notification behavior
                pushNotificationManager.setDozeMode(false)
                pushNotificationManager.setLowPowerMode(false)
                pushNotificationManager.setCriticalPowerMode(false)

                // Resume background operations
                offlineManager.setLowPowerMode(false)
                offlineManager.setCriticalPowerMode(false)
                offlineManager.resumeBackgroundSync()

                // Restore animations and frame rates
                performanceOptimizer.setReducedFrameRate(false)
                performanceOptimizer.setAnimationsEnabled(true)

            } catch (e: SecurityException) {
                Log.e(TAG, "Security error restoring normal operation", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state when restoring normal operation", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid argument when restoring normal operation", e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Normal operation restoration was cancelled", e)
            }
        }
    }

    /**
     * Check if device is currently in power save mode
     */
    fun isInPowerSaveMode(): Boolean {
        return powerManager.isPowerSaveMode
    }

    /**
     * Check if device is currently in Doze mode
     */
    fun isInDozeMode(): Boolean {
        return powerManager.isDeviceIdleMode
    }

    /**
     * Get battery optimization recommendations
     */
    fun getOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val batteryState = _batteryState.value

        if (batteryState.isCriticalBattery) {
            recommendations.add("Battery critically low - essential functions only")
            recommendations.add("Background sync disabled")
            recommendations.add("Non-essential notifications disabled")
        } else if (batteryState.isLowBattery) {
            recommendations.add("Battery low - reducing background activity")
            recommendations.add("Reduced sync frequency")
            recommendations.add("Limited notifications")
        }

        if (_dozeMode.value) {
            recommendations.add("Device in Doze mode - conserving power")
            recommendations.add("Performance monitoring paused")
            recommendations.add("Push notifications suspended")
        }

        if (_powerSaveMode.value) {
            recommendations.add("Power save mode active")
        }

        if (batteryState.temperature > 40.0f) {
            recommendations.add("Device temperature high - additional cooling recommended")
        }

        return recommendations
    }

    /**
     * Battery status broadcast receiver
     */
    private inner class BatteryOptimizationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    updateBatteryState()
                }
                Intent.ACTION_BATTERY_LOW -> {
                    Log.w(TAG, "Battery low warning received")
                    updateBatteryState()
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    Log.i(TAG, "Battery level OK")
                    updateBatteryState()
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val powerSaveMode = powerManager.isPowerSaveMode
                    _powerSaveMode.value = powerSaveMode
                    Log.i(TAG, "Power save mode changed: $powerSaveMode")
                }
            }
        }
    }

    /**
     * Lifecycle observer methods
     */
    override fun onStart(owner: LifecycleOwner) {
        // App moved to foreground - can resume some operations
        if (!_dozeMode.value && !_batteryState.value.isCriticalBattery) {
            restoreNormalOperation()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App moved to background - apply additional optimizations
        if (_dozeMode.value || _batteryState.value.isLowBattery) {
            applyDozeOptimizations()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid argument when unregistering battery receiver", e)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Runtime error unregistering battery receiver", e)
        }
    }
}