package com.mtlc.studyplan.image

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.io.File

/**
 * Coil-based Image Loading Optimization
 * Provides memory-efficient image loading with optimized caching strategies
 */
object CoilImageOptimizer {
    
    private const val TAG = "CoilImageOptimizer"
    
    // Image loader with optimized settings
    private var imageLoader: ImageLoader? = null
    
    /**
     * Create an optimized ImageLoader with memory and disk cache settings
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory for cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(calculateOptimalDiskCacheSize(context))
                    .build()
            }
            .respectCacheHeaders(false) // Use our own cache strategy
            .allowHardware(true) // Allow hardware bitmaps for better performance
            .crossfade(true)
            .build()
    }
    
    /**
     * Calculate optimal disk cache size based on available storage
     */
    private fun calculateOptimalDiskCacheSize(context: Context): Long {
        val cacheDir = context.cacheDir
        val usableSpace = cacheDir.usableSpace
        
        // Use 10% of available cache space, capped at 200MB
        return (usableSpace * 0.1).toLong().coerceAtMost(200 * 1024 * 1024L)
    }
    
    /**
     * Get the optimized image loader, creating it if necessary
     */
    private fun getOptimizedImageLoader(context: Context): ImageLoader {
        if (imageLoader == null) {
            imageLoader = createOptimizedImageLoader(context)
        }
        return imageLoader!!
    }
    
    /**
     * Load image with optimized settings
     */
    fun loadImage(
        context: Context,
        imageUrl: String,
        target: coil.target.Target? = null
    ) {
        try {
            val imageLoader = getOptimizedImageLoader(context)
            
            val requestBuilder = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
            
            // Apply target if provided
            if (target != null) {
                requestBuilder.target(target)
            }
            
            val request = requestBuilder.build()
            imageLoader.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: $imageUrl", e)
        }
    }
    
    /**
     * Preload images for better performance
     */
    fun preloadImages(
        context: Context,
        imageUrls: List<String>,
        maxSize: Int = 128 // Smaller size for preloading
    ) {
        imageUrls.take(10).forEach { url -> // Limit to 10 images to avoid memory issues
            try {
                val imageLoader = getOptimizedImageLoader(context)
                
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(maxSize, maxSize)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                
                imageLoader.enqueue(request)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to preload image: $url", e)
            }
        }
    }
    
    /**
     * Clear image cache to free up memory
     */
    fun clearImageCache(context: Context) {
        try {
            val imageLoader = getOptimizedImageLoader(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing image cache", e)
        }
    }
}
