package com.github.ljarka.immich.android.local

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ImageData(
    val id: Long,
    val uri: Uri,
    val path: String,
    val fileName: String,
    val dateTaken: Long,
    val width: Int?,
    val height: Int?,
)

class LocalImagesProvider {

    suspend fun getImagesForMonth(context: Context, month: Int, year: Int): List<ImageData> {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(year, month, calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
            val startOfMonth = calendar.timeInMillis
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endOfMonth = calendar.timeInMillis

            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,  // Path (deprecated in API 29+)
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
            )
            val selection =
                "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} <= ?"
            val selectionArgs = arrayOf(startOfMonth.toString(), endOfMonth.toString())
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val images = mutableListOf<ImageData>()
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val dataColumn =
                        cursor.getColumnIndex(MediaStore.Images.Media.DATA) // Deprecated but works for older APIs
                    val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                    val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val contentUri = Uri.withAppendedPath(uri, id.toString())
                        val filePath = if (dataColumn != -1) cursor.getString(dataColumn) else ""
                        val fileName =
                            if (nameColumn != -1) cursor.getString(nameColumn) else "Unknown"
                        val dateTaken = if (dateColumn != -1) cursor.getLong(dateColumn) else 0
                        val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                        val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0

                        images.add(
                            ImageData(
                                id,
                                contentUri,
                                filePath,
                                fileName,
                                dateTaken,
                                width,
                                height
                            )
                        )
                    }
                }
            images
        }
    }

    suspend fun getImagesForCurrentMonth(context: Context): List<ImageData> {
        val calendar = Calendar.getInstance()
        return getImagesForMonth(context, calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }

    suspend fun getImageCountForCurrentMonth(context: Context): Int {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val endOfMonth = calendar.timeInMillis - 1

            val selection =
                "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} <= ?"
            val selectionArgs = arrayOf(startOfMonth.toString(), endOfMonth.toString())

            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(uri, null, selection, selectionArgs, null)
                ?.use { cursor ->
                    return@withContext cursor.count
                } ?: 0
        }
    }
}