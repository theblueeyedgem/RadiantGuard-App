package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object NoteStorageManager {

    private const val ATTACHMENTS_DIR = "radiant_notes_attachments"

    fun saveUriToPhoneStorage(context: Context, sourceUri: Uri): NoteAttachment? {
        return try {
            val contentResolver = context.contentResolver
            val (displayName, size) = queryFileInfo(context, sourceUri)
            val mimeType = contentResolver.getType(sourceUri) ?: getMimeTypeFromExtension(displayName)
            val isImage = mimeType.startsWith("image/")

            val dir = File(context.filesDir, ATTACHMENTS_DIR).apply {
                if (!exists()) mkdirs()
            }

            val uniqueFileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}_${displayName}"
            val destFile = File(dir, uniqueFileName)

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            NoteAttachment(
                id = UUID.randomUUID().toString(),
                fileName = displayName,
                mimeType = mimeType,
                localFilePath = destFile.absolutePath,
                fileSizeBytes = if (size > 0) size else destFile.length(),
                isImage = isImage
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportImageToGallery(context: Context, attachment: NoteAttachment): Boolean {
        val file = File(attachment.localFilePath)
        if (!file.exists()) return false

        return try {
            val contentResolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Radiant_${attachment.fileName}")
                put(MediaStore.Images.Media.MIME_TYPE, if (attachment.mimeType.isNotEmpty()) attachment.mimeType else "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RadiantGuard")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

            contentResolver.openOutputStream(uri)?.use { outStream ->
                file.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportFileToDownloads(context: Context, attachment: NoteAttachment): Boolean {
        val file = File(attachment.localFilePath)
        if (!file.exists()) return false

        return try {
            val contentResolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, attachment.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, attachment.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RadiantGuard")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            } else {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(publicDownloads, attachment.fileName)
                file.copyTo(targetFile, overwrite = true)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "attachment_${System.currentTimeMillis()}"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }
}
