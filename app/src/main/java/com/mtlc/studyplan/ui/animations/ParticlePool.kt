package com.mtlc.studyplan.ui.animations

import android.view.View
import androidx.compose.runtime.mutableStateListOf
import kotlin.math.min

/**
 * Object pool for particle animations to prevent garbage collection stutters
 * This reduces memory allocation during particle effects like flame particles and confetti
 */
class ParticlePool(private val initialSize: Int = 20, private val maxSize: Int = 50) {
    private val availableParticles = mutableStateListOf<ParticleView>()
    private var createdCount = 0

    init {
        // Pre-allocate particles to avoid runtime creation
        repeat(initialSize) {
            availableParticles.add(ParticleView())
        }
        createdCount = initialSize
    }

    /**
     * Acquire a particle from the pool or create a new one if needed
     */
    fun acquire(): ParticleView {
        return if (availableParticles.isNotEmpty()) {
            availableParticles.removeAt(availableParticles.size - 1).reset()
        } else if (createdCount < maxSize) {
            createdCount++
            ParticleView().reset()
        } else {
            // Pool is full, reuse oldest one
            ParticleView().reset()
        }
    }

    /**
     * Return a particle to the pool for reuse
     */
    fun release(particle: ParticleView) {
        if (availableParticles.size < maxSize) {
            particle.cleanup()
            availableParticles.add(particle)
        }
    }

    /**
     * Clear all pooled particles
     */
    fun clear() {
        availableParticles.forEach { it.cleanup() }
        availableParticles.clear()
    }

    fun getPoolSize(): Int = availableParticles.size
    fun getCreatedCount(): Int = createdCount
}

/**
 * Reusable particle view for animations
 */
class ParticleView {
    var x: Float = 0f
    var y: Float = 0f
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var alpha: Float = 1f
    var scale: Float = 1f
    var rotation: Float = 0f
    var text: String = ""
    var lifespan: Long = 0L
    var startTime: Long = 0L

    fun reset(): ParticleView {
        x = 0f
        y = 0f
        velocityX = 0f
        velocityY = 0f
        alpha = 1f
        scale = 1f
        rotation = 0f
        text = ""
        lifespan = 0L
        startTime = System.currentTimeMillis()
        return this
    }

    fun cleanup() {
        text = ""
    }

    fun isAlive(): Boolean {
        val elapsed = System.currentTimeMillis() - startTime
        return elapsed < lifespan && alpha > 0f
    }

    fun updateProgress() {
        val elapsed = System.currentTimeMillis() - startTime
        val progress = (elapsed.toFloat() / lifespan.toFloat()).coerceIn(0f, 1f)
        alpha = (1f - progress).coerceIn(0f, 1f)
    }
}
