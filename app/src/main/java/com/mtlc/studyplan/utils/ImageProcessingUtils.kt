package com.mtlc.studyplan.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.media.FaceDetector
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

data class AvatarPreview(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val estimatedFileSize: Long,
    val mimeType: String
)

object ImageProcessingUtils {

    private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB
    private const val MAX_DIMENSION = 512
    private const val TARGET_SIZE = 256
    private const val JPEG_QUALITY = 85
    private const val MAX_FACES = 5
    private const val MIN_FACE_DETECTION_SIZE = 64
    private const val FACE_FRAME_MULTIPLIER = 4f
    private const val FACE_FRAME_MIN_RATIO = 0.5f

    private val SUPPORTED_FORMATS = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/gif"
    )

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
                return@withContext ImageValidationResult(false, "Unsupported image format. Please use JPG, PNG, WebP, or GIF")
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
        return abs(aspectRatio - 1.0f) > 0.1f // Allow 10% tolerance
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
     * Generates a processed avatar preview without saving it to disk.
     */
    suspend fun generatePreviewImage(
        context: Context,
        sourceUri: Uri
    ): Result<AvatarPreview> = withContext(Dispatchers.IO) {
        try {
            val validation = validateImage(context, sourceUri)
            if (!validation.isValid) {
                return@withContext Result.failure(Exception(validation.errorMessage ?: "Invalid image"))
            }

            val bitmap = loadBitmapFromUri(context, sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to load image"))

            val processedBitmap = processBitmap(bitmap)
            if (bitmap != processedBitmap) {
                bitmap.recycle()
            }

            val byteStream = ByteArrayOutputStream()
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, byteStream)
            val estimatedSize = byteStream.size().toLong()
            byteStream.close()

            Result.success(
                AvatarPreview(
                    bitmap = processedBitmap,
                    width = processedBitmap.width,
                    height = processedBitmap.height,
                    estimatedFileSize = estimatedSize,
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
     * Processes bitmap: crops to square (face-aware) and resizes to target size
     */
    private fun processBitmap(bitmap: Bitmap): Bitmap {
        val cropped = cropToSquareWithFaceFocus(bitmap)
        val resized = if (cropped.width != TARGET_SIZE || cropped.height != TARGET_SIZE) {
            Bitmap.createScaledBitmap(cropped, TARGET_SIZE, TARGET_SIZE, true)
        } else {
            cropped
        }

        if (cropped !== bitmap && cropped !== resized) {
            cropped.recycle()
        }

        return resized
    }

    /**
     * Attempts face-aware square cropping, falling back to a centered crop when necessary.
     */
    private fun cropToSquareWithFaceFocus(bitmap: Bitmap): Bitmap {
        val detection = detectPrimaryFace(bitmap)
        if (detection == null) {
            return centerCropSquare(bitmap)
        }

        val safeDimension = min(bitmap.width, bitmap.height).toFloat()
        val desiredSize = max(
            detection.eyeDistance * FACE_FRAME_MULTIPLIER,
            safeDimension * FACE_FRAME_MIN_RATIO
        ).coerceAtMost(safeDimension)

        val halfSize = desiredSize / 2f
        var left = detection.centerX - halfSize
        var top = detection.centerY - halfSize
        var right = detection.centerX + halfSize
        var bottom = detection.centerY + halfSize

        if (left < 0f) {
            right -= left
            left = 0f
        }
        if (top < 0f) {
            bottom -= top
            top = 0f
        }
        if (right > bitmap.width) {
            val diff = right - bitmap.width
            left -= diff
            right = bitmap.width.toFloat()
        }
        if (bottom > bitmap.height) {
            val diff = bottom - bitmap.height
            top -= diff
            bottom = bitmap.height.toFloat()
        }

        left = left.coerceAtLeast(0f)
        top = top.coerceAtLeast(0f)
        right = right.coerceAtMost(bitmap.width.toFloat())
        bottom = bottom.coerceAtMost(bitmap.height.toFloat())

        var width = right - left
        var height = bottom - top
        val size = min(width, height)
        if (size <= 0f) {
            return centerCropSquare(bitmap)
        }

        val horizontalExcess = width - size
        val verticalExcess = height - size
        left += horizontalExcess / 2f
        right -= horizontalExcess / 2f
        top += verticalExcess / 2f
        bottom -= verticalExcess / 2f

        val intSize = min(size.roundToInt(), min(bitmap.width, bitmap.height))
        if (intSize <= 0) {
            return centerCropSquare(bitmap)
        }

        val intLeft = left.roundToInt().coerceIn(0, max(0, bitmap.width - intSize))
        val intTop = top.roundToInt().coerceIn(0, max(0, bitmap.height - intSize))

        return Bitmap.createBitmap(bitmap, intLeft, intTop, intSize, intSize)
    }

    /**
     * Simple centered square crop fallback.
     */
    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2

        return if (x == 0 && y == 0 && bitmap.width == size && bitmap.height == size) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, x, y, size, size)
        }
    }

        @Suppress("DEPRECATION")
    private fun detectPrimaryFace(bitmap: Bitmap): FaceDetectionResult? {
        if (bitmap.width < MIN_FACE_DETECTION_SIZE || bitmap.height < MIN_FACE_DETECTION_SIZE) {
            return null
        }

        var detectionBitmap = bitmap

        if (detectionBitmap.config != Bitmap.Config.RGB_565) {
            detectionBitmap = bitmap.copy(Bitmap.Config.RGB_565, false)
        }

        if (detectionBitmap.width % 2 != 0) {
            val trimmed = Bitmap.createBitmap(detectionBitmap, 0, 0, detectionBitmap.width - 1, detectionBitmap.height)
            if (detectionBitmap !== bitmap) {
                detectionBitmap.recycle()
            }
            detectionBitmap = trimmed
        }

        val detectionWidth = detectionBitmap.width
        val detectionHeight = detectionBitmap.height

        val detector = FaceDetector(detectionWidth, detectionHeight, MAX_FACES)
        val faces = arrayOfNulls<FaceDetector.Face>(MAX_FACES)
        val count = detector.findFaces(detectionBitmap, faces)

        if (detectionBitmap !== bitmap) {
            detectionBitmap.recycle()
        }

        if (count <= 0) {
            return null
        }

        val primary = faces.filterNotNull().maxByOrNull { it.eyesDistance() } ?: return null
        val midPoint = PointF()
        primary.getMidPoint(midPoint)

        val widthRatio = bitmap.width.toFloat() / detectionWidth.toFloat()
        val heightRatio = bitmap.height.toFloat() / detectionHeight.toFloat()
        val scale = (widthRatio + heightRatio) / 2f

        return FaceDetectionResult(
            centerX = midPoint.x * widthRatio,
            centerY = midPoint.y * heightRatio,
            eyeDistance = primary.eyesDistance() * scale
        )
    }
private data class FaceDetectionResult(
        val centerX: Float,
        val centerY: Float,
        val eyeDistance: Float
    )

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

