package com.mtlc.studyplan.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageProcessor {

    /**
     * Process and crop image from URI to create circular avatar
     * @param context Application context
     * @param uri Image URI from gallery
     * @param targetSize Target size for the avatar (default 512px)
     * @return File path of the processed avatar image, null if processing failed
     */
    suspend fun processAvatarImage(
        context: Context,
        uri: Uri,
        targetSize: Int = 512
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Read the image from URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return@withContext null

            // Create square bitmap by cropping to center
            val squareBitmap = cropToSquare(originalBitmap)

            // Resize to target size
            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)

            // Create circular crop
            val circularBitmap = createCircularBitmap(resizedBitmap)

            // Save to app's internal storage
            val avatarFile = saveAvatarToStorage(context, circularBitmap)

            // Clean up bitmaps
            originalBitmap.recycle()
            if (squareBitmap != originalBitmap) squareBitmap.recycle()
            if (resizedBitmap != squareBitmap) resizedBitmap.recycle()
            circularBitmap.recycle()

            avatarFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crop bitmap to square by taking center portion
     */
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)

        val x = (width - size) / 2
        val y = (height - size) / 2

        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * Create circular bitmap from square bitmap
     */
    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)

        // Draw circle
        canvas.drawOval(rectF, paint)

        // Apply source image with circular mask
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    /**
     * Save avatar bitmap to app's internal storage
     */
    private suspend fun saveAvatarToStorage(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }

            val fileName = "custom_avatar_${System.currentTimeMillis()}.png"
            val file = File(avatarsDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get file size in bytes
     */
    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Format file size to human readable string
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }

    /**
     * Delete avatar file
     */
    fun deleteAvatar(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }
}
