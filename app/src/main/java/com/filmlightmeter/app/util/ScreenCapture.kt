package com.filmlightmeter.app.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.view.Window
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenCapture {

    /**
     * Делает скриншот окна Activity через PixelCopy (работает и с SurfaceView/TextureView камеры)
     * и сохраняет его в общий каталог Pictures/FilmLightMeter через MediaStore.
     */
    fun capture(activity: Activity, callback: (Result) -> Unit) {
        val window = activity.window
        val rootView = window.decorView.rootView
        val width = rootView.width
        val height = rootView.height
        if (width <= 0 || height <= 0) {
            callback(Result.Error("Не удалось получить размеры окна"))
            return
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PixelCopy.request(
                    window,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            saveBitmap(activity, bitmap, callback)
                        } else {
                            callback(Result.Error("PixelCopy: код $result"))
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                callback(Result.Error("Ошибка скриншота: ${e.message}"))
            }
        } else {
            // Запасной путь для API < 26: рисуем иерархию View в bitmap (камера не попадёт, но интерфейс — да)
            try {
                val canvas = android.graphics.Canvas(bitmap)
                rootView.draw(canvas)
                saveBitmap(activity, bitmap, callback)
            } catch (e: Exception) {
                callback(Result.Error("Ошибка скриншота: ${e.message}"))
            }
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, callback: (Result) -> Unit) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "FilmLightMeter_$timestamp.png"

        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/FilmLightMeter"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
                val inserted = context.contentResolver.insert(collection, values)
                    ?: return callback(Result.Error("Не удалось создать файл в галерее"))

                context.contentResolver.openOutputStream(inserted)?.use { out: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: return callback(Result.Error("Не удалось открыть поток записи"))

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(inserted, values, null, null)
                inserted
            } else {
                // Для API 24..28 используем прямое сохранение в Pictures через MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                val inserted = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return callback(Result.Error("Не удалось создать файл в галерее"))

                context.contentResolver.openOutputStream(inserted)?.use { out: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: return callback(Result.Error("Не удалось открыть поток записи"))
                inserted
            }
            callback(Result.Success(uri, fileName))
        } catch (e: Exception) {
            callback(Result.Error("Ошибка сохранения: ${e.message}"))
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    sealed class Result {
        data class Success(val uri: Uri, val fileName: String) : Result()
        data class Error(val message: String) : Result()
    }
}
