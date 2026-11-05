package com.ecoalertum.android.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

private const val UPLOAD_MAX_SIDE = 1280
private const val JPEG_QUALITY = 80

/** Reads [uri] and shrinks it so its longest side is at most [maxSide] px. Call off the main thread. */
fun decodeScaledBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null

    val longest = maxOf(decoded.width, decoded.height)
    if (longest <= maxSide) return decoded
    val scale = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).toInt(),
        (decoded.height * scale).toInt(),
        true,
    )
}

/** Shrinks the photo, uploads it to Storage and returns its path: reports/<uid>/<random>.jpg */
suspend fun uploadReportPhoto(context: Context, uri: Uri, uid: String): Result<String> =
    withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaledBitmap(context, uri, UPLOAD_MAX_SIDE)
                ?: return@withContext Result.failure(IllegalStateException("Could not read the photo"))
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
            val path = "reports/$uid/${UUID.randomUUID()}.jpg"
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
            FirebaseStorage.getInstance().reference.child(path).putBytes(bytes, metadata).await()
            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }