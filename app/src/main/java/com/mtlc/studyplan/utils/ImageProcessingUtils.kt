package com.mtlc.studyplan.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

data class ImageValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0,
    val mimeType: String = ""
)

data class ProcessedImage(
    val file: File,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val mimeType: String
)

object ImageProcessingUtils {

    private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB
    private const val MAX_DIMENSION = 512
    private const val TARGET_SIZE = 256
    private const val JPEG_QUALITY = 85

    private val SUPPORTED_FORMATS = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    /**
     * Validates an image from URI
     */
    suspend fun validateImage(context: Context, uri: Uri): ImageValidationResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext ImageValidationResult(false, "Cannot access image file")

            // Get file size
            val fileSize = inputStream.available().toLong()
            if (fileSize > MAX_FILE_SIZE) {
                inputStream.close()
                return@withContext ImageValidationResult(false, "Image file must be smaller than 2MB")
            }

            // Get MIME type
            val mimeType = contentResolver.getType(uri) ?: ""
            if (!SUPPORTED_FORMATS.contains(mimeType.lowercase())) {
                inputStream.close()
                return@withContext ImageValidationResult(false, "Unsupported image format. Please use JPG, PNG, or WebP")
            }

            // Get image dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val width = options.outWidth
            val height = options.outHeight

            if (width <= 0 || height <= 0) {
                return@withContext ImageValidationResult(false, "Invalid image dimensions")
            }

            ImageValidationResult(
                isValid = true,
                width = width,
                height = height,
                fileSize = fileSize,
                mimeType = mimeType
            )

        } catch (e: Exception) {
            ImageValidationResult(false, "Error reading image: ${e.message}")
        }
    }

    /**
     * Checks if image needs cropping (non-square aspect ratio)
     */
    fun needsCropping(width: Int, height: Int): Boolean {
        val aspectRatio = width.toFloat() / height.toFloat()
        return kotlin.math.abs(aspectRatio - 1.0f) > 0.1f // Allow 10% tolerance
    }

    /**
     * Checks if image needs resizing
     */
    fun needsResizing(width: Int, height: Int): Boolean {
        return width > MAX_DIMENSION || height > MAX_DIMENSION
    }

    /**
     * Processes image: validates, crops to square, resizes, compresses, and saves to internal storage
     */
    suspend fun processAndSaveImage(
        context: Context,
        sourceUri: Uri,
        userId: String
    ): Result<ProcessedImage> = withContext(Dispatchers.IO) {
        try {
            // Validate image first
            val validation = validateImage(context, sourceUri)
            if (!validation.isValid) {
                return@withContext Result.failure(Exception(validation.errorMessage ?: "Invalid image"))
            }

            // Load bitmap
            val bitmap = loadBitmapFromUri(context, sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to load image"))

            // Process bitmap
            val processedBitmap = processBitmap(bitmap)

            // Save to internal storage
            val savedFile = saveBitmapToInternalStorage(context, processedBitmap, userId)

            // Clean up
            if (bitmap != processedBitmap) {
                bitmap.recycle()
            }
            processedBitmap.recycle()

            Result.success(
                ProcessedImage(
                    file = savedFile,
                    width = TARGET_SIZE,
                    height = TARGET_SIZE,
                    fileSize = savedFile.length(),
                    mimeType = "image/jpeg"
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads bitmap from URI with proper orientation handling
     */
    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null

            // Calculate sample size to avoid OutOfMemoryError
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION)
            options.inJustDecodeBounds = false

            val inputStream2 = contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2.close()

            bitmap?.let { correctBitmapOrientation(context, it, uri) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Corrects bitmap orientation based on EXIF data
     */
    private suspend fun correctBitmapOrientation(context: Context, bitmap: Bitmap, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                inputStream.close()

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                }

                if (!matrix.isIdentity) {
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    return@withContext rotatedBitmap
                }
            }
        } catch (e: Exception) {
            // If orientation correction fails, return original bitmap
        }
        bitmap
    }

    /**
     * Processes bitmap: crops to square and resizes to target size
     */
    private fun processBitmap(bitmap: Bitmap): Bitmap {
        // Crop to square
        val squareBitmap = cropToSquare(bitmap)

        // Resize to target size
        val resizedBitmap = if (squareBitmap.width != TARGET_SIZE || squareBitmap.height != TARGET_SIZE) {
            Bitmap.createScaledBitmap(squareBitmap, TARGET_SIZE, TARGET_SIZE, true)
        } else {
            squareBitmap
        }

        // Clean up intermediate bitmap if different
        if (squareBitmap != bitmap && squareBitmap != resizedBitmap) {
            squareBitmap.recycle()
        }

        return resizedBitmap
    }

    /**
     * Crops bitmap to square (center crop)
     */
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = kotlin.math.min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2

        return if (x == 0 && y == 0 && bitmap.width == size && bitmap.height == size) {
            bitmap // Already square
        } else {
            Bitmap.createBitmap(bitmap, x, y, size, size)
        }
    }

    /**
     * Saves bitmap to internal storage
     */
    private suspend fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        userId: String
    ): File = withContext(Dispatchers.IO) {
        val avatarsDir = File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }

        val fileName = "${userId}_${UUID.randomUUID()}.jpg"
        val file = File(avatarsDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        file
    }

    /**
     * Calculates sample size for efficient bitmap loading
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Deletes avatar file from internal storage
     */
    suspend fun deleteAvatarFile(context: Context, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars")
            val file = File(avatarsDir, fileName)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleans up old avatar files for a user
     */
    suspend fun cleanupOldAvatars(context: Context, userId: String, keepCount: Int = 3): Boolean = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) return@withContext true

            val userAvatars = avatarsDir.listFiles { file ->
                file.name.startsWith("${userId}_") && file.name.endsWith(".jpg")
            }?.sortedByDescending { it.lastModified() }

            if (userAvatars != null && userAvatars.size > keepCount) {
                userAvatars.drop(keepCount).forEach { file ->
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}