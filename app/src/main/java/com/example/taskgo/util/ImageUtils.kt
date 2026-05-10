package com.example.taskgo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Converts a Uri to a compressed Base64 string that fits in Firestore (under 1MB).
     */
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 400, maxHeight: Int = 400): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Calculate scaling
            val ratio = Math.min(maxWidth.toFloat() / originalBitmap.width, maxHeight.toFloat() / originalBitmap.height)
            val finalWidth = (originalBitmap.width * ratio).toInt().coerceAtLeast(1)
            val finalHeight = (originalBitmap.height * ratio).toInt().coerceAtLeast(1)

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true)
            
            val outputStream = ByteArrayOutputStream()
            // Using 60% quality to keep string length manageable for text fields
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()
            
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a Base64 string (with or without data:image prefix) to a ByteArray.
     * Coil handles ByteArrays much better than extremely long strings.
     */
    fun decodeBase64ToByteArray(base64String: String?): ByteArray? {
        if (base64String == null) return null
        return try {
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            Base64.decode(pureBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}
