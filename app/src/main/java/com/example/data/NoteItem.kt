package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val colorHex: String = "#FFFFFF",
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    /** SHA-256 hex of the per-note lock password; empty = no lock password set. */
    val lockPasswordHash: String = "",
    val backgroundStyle: String = "solid", // "solid" (uses colorHex) or "gradient:#HEX1,#HEX2"
    val attachmentsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getAttachments(): List<NoteAttachment> {
        val list = mutableListOf<NoteAttachment>()
        try {
            val jsonArray = JSONArray(attachmentsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NoteAttachment(
                        id = obj.optString("id"),
                        fileName = obj.optString("fileName"),
                        mimeType = obj.optString("mimeType"),
                        localFilePath = obj.optString("localFilePath"),
                        fileSizeBytes = obj.optLong("fileSizeBytes"),
                        isImage = obj.optBoolean("isImage")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        fun serializeAttachments(attachments: List<NoteAttachment>): String {
            val array = JSONArray()
            attachments.forEach { att ->
                val obj = JSONObject().apply {
                    put("id", att.id)
                    put("fileName", att.fileName)
                    put("mimeType", att.mimeType)
                    put("localFilePath", att.localFilePath)
                    put("fileSizeBytes", att.fileSizeBytes)
                    put("isImage", att.isImage)
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}

data class NoteAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val localFilePath: String, // Absolute path in app's internal phone storage directory
    val fileSizeBytes: Long = 0L,
    val isImage: Boolean = false
)
