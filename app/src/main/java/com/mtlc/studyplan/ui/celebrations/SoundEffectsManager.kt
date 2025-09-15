package com.mtlc.studyplan.ui.celebrations

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Sound effect types for celebrations
 */
enum class SoundEffect(val fileName: String, val volume: Float) {
    TASK_COMPLETION("task_complete.mp3", 0.3f),
    DAILY_GOAL("daily_goal.mp3", 0.5f),
    LEVEL_UP("level_up.mp3", 0.7f),
    MILESTONE_ACHIEVEMENT("milestone.mp3", 1.0f),
    STREAK_MILESTONE("streak_fire.mp3", 0.8f),
    CONFETTI_POP("confetti.mp3", 0.4f),
    SUCCESS_CHIME("success.mp3", 0.6f)
}

/**
 * Sound effects manager for celebration system
 */
class SoundEffectsManager(private val context: Context) {
    private val mediaPlayers = mutableMapOf<SoundEffect, MediaPlayer>()
    private var isSoundEnabled = true
    private var masterVolume = 1.0f

    init {
        // Initialize audio attributes for game sounds
        setupAudioAttributes()
    }

    /**
     * Setup audio attributes for celebration sounds
     */
    private fun setupAudioAttributes() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Pre-load commonly used sound effects
        preloadSoundEffect(SoundEffect.TASK_COMPLETION)
        preloadSoundEffect(SoundEffect.DAILY_GOAL)
        preloadSoundEffect(SoundEffect.CONFETTI_POP)
    }

    /**
     * Preload a sound effect
     */
    private fun preloadSoundEffect(soundEffect: SoundEffect) {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Set data source from assets (would need actual audio files)
                // For now, using system notification sounds as placeholders
                setDataSource(getSystemSoundPath(soundEffect))

                prepareAsync()
                setOnPreparedListener {
                    mediaPlayers[soundEffect] = this
                }
                setOnCompletionListener {
                    seekTo(0) // Reset to beginning for replay
                }
            }
        } catch (e: Exception) {
            // Fallback: disable sound if files not found
            println("Sound effect ${soundEffect.fileName} not found: ${e.message}")
        }
    }

    /**
     * Get system sound path (placeholder implementation)
     */
    private fun getSystemSoundPath(soundEffect: SoundEffect): String {
        // In a real implementation, you would return paths to actual audio files
        // For now, return paths to system notification sounds as placeholders
        return when (soundEffect) {
            SoundEffect.TASK_COMPLETION -> "/system/media/audio/notifications/Chime.ogg"
            SoundEffect.DAILY_GOAL -> "/system/media/audio/notifications/Ding.ogg"
            SoundEffect.LEVEL_UP -> "/system/media/audio/notifications/Trill.ogg"
            SoundEffect.MILESTONE_ACHIEVEMENT -> "/system/media/audio/notifications/Fanfare.ogg"
            SoundEffect.STREAK_MILESTONE -> "/system/media/audio/notifications/Triumph.ogg"
            SoundEffect.CONFETTI_POP -> "/system/media/audio/notifications/Pop.ogg"
            SoundEffect.SUCCESS_CHIME -> "/system/media/audio/notifications/Success.ogg"
        }
    }

    /**
     * Play a sound effect
     */
    fun playSound(soundEffect: SoundEffect, customVolume: Float = -1f) {
        if (!isSoundEnabled) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaPlayer = mediaPlayers[soundEffect] ?: run {
                    preloadSoundEffect(soundEffect)
                    delay(100) // Wait for preload
                    mediaPlayers[soundEffect]
                } ?: return@launch

                val volume = if (customVolume >= 0) customVolume else soundEffect.volume
                val adjustedVolume = volume * masterVolume

                mediaPlayer.setVolume(adjustedVolume, adjustedVolume)

                if (mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(0)
                } else {
                    mediaPlayer.start()
                }
            } catch (e: Exception) {
                println("Error playing sound effect: ${e.message}")
            }
        }
    }

    /**
     * Play sound sequence for complex celebrations
     */
    fun playSoundSequence(sounds: List<Pair<SoundEffect, Long>>) {
        if (!isSoundEnabled) return

        CoroutineScope(Dispatchers.IO).launch {
            sounds.forEach { (sound, delay) ->
                playSound(sound)
                delay(delay)
            }
        }
    }

    /**
     * Enable or disable sound effects
     */
    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
        if (!enabled) {
            stopAllSounds()
        }
    }

    /**
     * Set master volume (0.0 - 1.0)
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    /**
     * Stop all currently playing sounds
     */
    private fun stopAllSounds() {
        mediaPlayers.values.forEach { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0)
                }
            } catch (e: Exception) {
                // Ignore errors when stopping
            }
        }
    }

    /**
     * Check if system audio settings allow game sounds
     */
    fun isSystemAudioEnabled(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        mediaPlayers.values.forEach { player ->
            try {
                player.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        mediaPlayers.clear()
    }
}

/**
 * Predefined sound sequences for celebrations
 */
object SoundSequences {

    val taskCompletion = listOf(
        SoundEffect.TASK_COMPLETION to 0L
    )

    val dailyGoalAchieved = listOf(
        SoundEffect.CONFETTI_POP to 0L,
        SoundEffect.DAILY_GOAL to 500L,
        SoundEffect.SUCCESS_CHIME to 1000L
    )

    val levelUp = listOf(
        SoundEffect.CONFETTI_POP to 0L,
        SoundEffect.LEVEL_UP to 300L,
        SoundEffect.SUCCESS_CHIME to 1200L
    )

    val milestoneReward = listOf(
        SoundEffect.CONFETTI_POP to 0L,
        SoundEffect.CONFETTI_POP to 200L,
        SoundEffect.MILESTONE_ACHIEVEMENT to 500L,
        SoundEffect.SUCCESS_CHIME to 2000L
    )

    val streakMilestone = listOf(
        SoundEffect.STREAK_MILESTONE to 0L,
        SoundEffect.CONFETTI_POP to 800L,
        SoundEffect.SUCCESS_CHIME to 1500L
    )
}

/**
 * ViewModel for managing sound effects
 */
class SoundEffectsViewModel : ViewModel() {
    private var soundManager: SoundEffectsManager? = null

    fun initialize(context: Context) {
        if (soundManager == null) {
            soundManager = SoundEffectsManager(context)
        }
    }

    fun playSound(soundEffect: SoundEffect, volume: Float = -1f) {
        soundManager?.playSound(soundEffect, volume)
    }

    fun playSoundSequence(sounds: List<Pair<SoundEffect, Long>>) {
        soundManager?.playSoundSequence(sounds)
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundManager?.setSoundEnabled(enabled)
    }

    fun setMasterVolume(volume: Float) {
        soundManager?.setMasterVolume(volume)
    }

    fun isSystemAudioEnabled(): Boolean {
        return soundManager?.isSystemAudioEnabled() ?: false
    }

    override fun onCleared() {
        super.onCleared()
        soundManager?.cleanup()
    }
}

/**
 * Composable for managing sound effects in celebrations
 */
@Composable
fun SoundEffectsProvider(
    content: @Composable (SoundEffectsManager) -> Unit
) {
    val context = LocalContext.current
    val soundViewModel: SoundEffectsViewModel = viewModel()

    LaunchedEffect(Unit) {
        soundViewModel.initialize(context)
    }

    // Create a mock manager for the composable
    val mockManager = remember {
        object {
            fun playSound(soundEffect: SoundEffect, volume: Float = -1f) {
                soundViewModel.playSound(soundEffect, volume)
            }

            fun playSoundSequence(sounds: List<Pair<SoundEffect, Long>>) {
                soundViewModel.playSoundSequence(sounds)
            }

            fun setSoundEnabled(enabled: Boolean) {
                soundViewModel.setSoundEnabled(enabled)
            }

            fun setMasterVolume(volume: Float) {
                soundViewModel.setMasterVolume(volume)
            }

            fun isSystemAudioEnabled(): Boolean {
                return soundViewModel.isSystemAudioEnabled()
            }
        }
    }

    content(mockManager as SoundEffectsManager)
}

/**
 * Enhanced celebration manager with sound effects
 */
@Composable
fun CelebrationManagerWithSound(
    celebrations: List<CelebrationEvent>,
    onCelebrationComplete: (String) -> Unit,
    soundEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    SoundEffectsProvider { soundManager ->
        // Set sound preferences
        LaunchedEffect(soundEnabled) {
            soundManager.setSoundEnabled(soundEnabled)
        }

        // Play sounds for new celebrations
        LaunchedEffect(celebrations.size) {
            if (celebrations.isNotEmpty()) {
                val latest = celebrations.last()
                when (latest.type) {
                    is CelebrationType.TaskCompletion -> {
                        soundManager.playSoundSequence(SoundSequences.taskCompletion)
                    }
                    is CelebrationType.DailyGoalAchieved -> {
                        soundManager.playSoundSequence(SoundSequences.dailyGoalAchieved)
                    }
                    is CelebrationType.LevelUp -> {
                        soundManager.playSoundSequence(SoundSequences.levelUp)
                    }
                    is CelebrationType.MilestoneReward -> {
                        when (latest.type.milestoneType) {
                            MilestoneType.STREAK_MILESTONE -> {
                                soundManager.playSoundSequence(SoundSequences.streakMilestone)
                            }
                            else -> {
                                soundManager.playSoundSequence(SoundSequences.milestoneReward)
                            }
                        }
                    }
                }
            }
        }

        // Render celebrations
        CelebrationManager(
            celebrations = celebrations,
            onCelebrationComplete = onCelebrationComplete,
            modifier = modifier
        )
    }
}

/**
 * Settings for sound effects
 */
data class SoundSettings(
    val isEnabled: Boolean = true,
    val masterVolume: Float = 1.0f,
    val respectSystemSettings: Boolean = true
)

/**
 * Sound settings provider
 */
@Composable
fun rememberSoundSettings(
    initialSettings: SoundSettings = SoundSettings()
): MutableState<SoundSettings> {
    return remember { mutableStateOf(initialSettings) }
}