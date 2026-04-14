package com.example.kksales.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.*

object FileUtils {
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "product_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, "product_images").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(file, fileName)
            
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            
            Uri.fromFile(destinationFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImageFromInternalStorage(imageUri: String?) {
        if (imageUri == null) return
        try {
            val uri = Uri.parse(imageUri)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllProductImageUris(context: Context): List<String> {
        val folder = File(context.filesDir, "product_images")
        if (!folder.exists()) return emptyList()
        return folder.listFiles()?.map { Uri.fromFile(it).toString() } ?: emptyList()
    }
}
