package com.example.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

enum class ImageUploadPreset(
  val maxDimensionPx: Int,
  val maxFileSizeBytes: Long,
  val initialQuality: Int,
) {
  PROFILE_PHOTO(
    maxDimensionPx = 800,
    maxFileSizeBytes = 512L * 1024L,
    initialQuality = 85,
  ),
  WORKFORCE_DOCUMENT(
    maxDimensionPx = 1600,
    maxFileSizeBytes = 2L * 1024L * 1024L,
    initialQuality = 88,
  ),
}

data class ProcessedImageAsset(
  val file: File,
  val mimeType: String,
  val width: Int,
  val height: Int,
  val sizeBytes: Long,
)

sealed interface ImageProcessingResult {
  data class Success(val asset: ProcessedImageAsset) : ImageProcessingResult
  data class ValidationError(val message: String) : ImageProcessingResult
}

object ImageProcessor {

  private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
  const val OUTPUT_MIME_TYPE = "image/jpeg"

  fun isSupportedMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    return mimeType.lowercase() in ALLOWED_MIME_TYPES
  }

  fun processCapturedFile(
    context: Context,
    sourceFile: File,
    preset: ImageUploadPreset,
  ): ImageProcessingResult {
    if (!sourceFile.exists() || sourceFile.length() == 0L) {
      return ImageProcessingResult.ValidationError("Captured image file is empty or missing.")
    }

    val orientation =
      try {
        ExifInterface(sourceFile.absolutePath).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL,
        )
      } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
      }

    val decoded =
      decodeSampledBitmap(sourceFile.absolutePath, preset.maxDimensionPx)
        ?: return ImageProcessingResult.ValidationError("Unable to decode captured image.")

    val rotated = rotateIfNeeded(decoded, orientation)
    if (rotated !== decoded) decoded.recycle()

    return compressAndSave(context, rotated, preset)
  }

  fun processPickedUri(
    context: Context,
    uri: Uri,
    preset: ImageUploadPreset,
  ): ImageProcessingResult {
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType != null && !isSupportedMimeType(mimeType)) {
      return ImageProcessingResult.ValidationError(
        "Unsupported file type ($mimeType). Only JPEG, PNG, or WebP images are permitted."
      )
    }

    val tempFile = File(context.cacheDir, "picked_${System.currentTimeMillis()}.tmp")
    return try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
      } ?: return ImageProcessingResult.ValidationError("Could not read selected image.")

      val result = processCapturedFile(context, tempFile, preset)
      tempFile.delete()
      result
    } catch (e: Exception) {
      tempFile.delete()
      ImageProcessingResult.ValidationError(
        e.localizedMessage ?: "Failed to process selected image."
      )
    }
  }

  private fun compressAndSave(
    context: Context,
    bitmap: Bitmap,
    preset: ImageUploadPreset,
  ): ImageProcessingResult {
    val scaled = scaleDownIfNeeded(bitmap, preset.maxDimensionPx)
    if (scaled !== bitmap) bitmap.recycle()

    val outputDir = File(context.cacheDir, "civictrack_uploads").apply { mkdirs() }
    val outFile = File(outputDir, "upload_${System.currentTimeMillis()}.jpg")

    var quality = preset.initialQuality
    var compressedBytes: ByteArray
    do {
      val stream = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
      compressedBytes = stream.toByteArray()
      quality -= 10
    } while (compressedBytes.size > preset.maxFileSizeBytes && quality >= 35)

    val finalWidth = scaled.width
    val finalHeight = scaled.height
    scaled.recycle()

    if (compressedBytes.size > preset.maxFileSizeBytes) {
      return ImageProcessingResult.ValidationError(
        "Image exceeds maximum upload limit of ${preset.maxFileSizeBytes / 1024} KB even after compression."
      )
    }

    FileOutputStream(outFile).use { it.write(compressedBytes) }
    return ImageProcessingResult.Success(
      ProcessedImageAsset(
        file = outFile,
        mimeType = OUTPUT_MIME_TYPE,
        width = finalWidth,
        height = finalHeight,
        sizeBytes = outFile.length(),
      )
    )
  }

  private fun decodeSampledBitmap(path: String, reqMaxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    val maxSide = max(bounds.outWidth, bounds.outHeight)
    while (maxSide / sampleSize > reqMaxDim * 2) {
      sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(path, decodeOpts)
  }

  private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val largest = max(bitmap.width, bitmap.height)
    if (largest <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / largest.toFloat()
    val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
    val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees =
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
      }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
}
