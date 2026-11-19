package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.RadiantGuardApplication
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Emergency export of ALL app data (vault, subscriptions, notes) as a PDF or a
 * Word-compatible .doc file, saved to Downloads/RadiantGuard on the device.
 *
 * The file contains passwords in PLAIN TEXT by design (it is an emergency
 * backup), so the header warns the user to keep it safe. The master PIN is
 * never included or logged.
 */
class ExportManager(private val context: Context) {

    private val app: RadiantGuardApplication
        get() = context.applicationContext as RadiantGuardApplication

    data class ExportResult(val uri: Uri, val fileName: String, val displayPath: String)

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    private fun displayTimestamp(): String =
        SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()).format(Date())

    private val headerWarning =
        "RadiantGuard emergency export \u2014 keep this file safe, it contains your passwords in plain text"

    /** Loads and decrypts everything once, so PDF and DOC share the same content. */
    private suspend fun loadAll(): Triple<List<DecryptedVaultItem>, List<SubscriptionItem>, List<NoteItem>> {
        val vault = app.vaultRepository.allDecryptedItems.first()
        val subs = app.subscriptionRepository.allSubscriptions.first()
        val notes = app.noteRepository.allNotes.first()
        return Triple(vault, subs, notes)
    }

    // ------------------------------------------------------------------ PDF

    suspend fun exportPdf(): ExportResult {
        val (vault, subs, notes) = loadAll()
        val fileName = "RadiantGuard-Export-${timestamp()}.pdf"
        val lines = buildPlainLines(vault, subs, notes)

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 48f
        val lineHeight = 18f
        val titlePaint = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val smallPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 10f

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float = lineHeight) {
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f) {
            val maxWidth = pageWidth - 2 * margin - indent
            val words = text.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) <= maxWidth) {
                    line = StringBuilder(test)
                } else {
                    ensureSpace()
                    canvas.drawText(line.toString(), margin + indent, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace()
                canvas.drawText(line.toString(), margin + indent, y, paint)
                y += lineHeight
            }
        }

        canvas.drawText("RadiantGuard \u2014 Emergency Export", margin, y, titlePaint)
        y += 26f
        drawWrapped(headerWarning, smallPaint)
        y += 6f
        drawWrapped("Exported: ${displayTimestamp()}", smallPaint)
        y += 10f

        for ((text, style) in lines) {
            when (style) {
                LineStyle.HEADING -> {
                    y += 10f
                    ensureSpace(24f)
                    drawWrapped(text, headingPaint)
                    y += 2f
                }
                LineStyle.BODY -> drawWrapped(text, bodyPaint, indent = 8f)
                LineStyle.EMPTY -> y += 6f
            }
        }

        document.finishPage(page)
        val uri = saveBytes(fileName, "application/pdf") { out ->
            document.writeTo(out)
            document.close()
        }
        return ExportResult(uri, fileName, "Downloads/RadiantGuard/$fileName")
    }

    // ------------------------------------------------------------------ DOC

    /** Word-compatible .doc: HTML with tables, which Word/LibreOffice open natively. */
    suspend fun exportDoc(): ExportResult {
        val (vault, subs, notes) = loadAll()
        val fileName = "RadiantGuard-Export-${timestamp()}.doc"
        val html = buildString {
            append("<html><head><meta charset=\"utf-8\">")
            append("<title>RadiantGuard Emergency Export</title></head><body>")
            append("<h1>RadiantGuard \u2014 Emergency Export</h1>")
            append("<p><b>${headerWarning.escapeHtml()}</b></p>")
            append("<p>Exported: ${displayTimestamp().escapeHtml()}</p>")

            append("<h2>Vault (${vault.size} item${if (vault.size == 1) "" else "s"})</h2>")
            if (vault.isEmpty()) {
                append("<p>No items</p>")
            } else {
                append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">")
                append("<tr><th>Title</th><th>Username</th><th>Password</th><th>URL</th><th>Notes</th><th>2FA secret</th></tr>")
                vault.forEach { item ->
                    val locked = item.isLocked
                    append("<tr><td>${item.title.escapeHtml()}${if (item.isPinned) " \uD83D\uDCCC" else ""}</td>")
                    append("<td>${item.username.escapeHtml()}</td>")
                    append("<td>${if (locked) "\u2022\u2022\u2022\u2022\u2022\u2022 (locked item \u2014 password hidden)" else item.password.escapeHtml()}</td>")
                    append("<td>${item.url.escapeHtml()}</td>")
                    append("<td>${item.notes.escapeHtml()}</td>")
                    append("<td>${if (locked) "\u2022\u2022\u2022\u2022\u2022\u2022" else item.totpSecret.escapeHtml()}</td></tr>")
                }
                append("</table>")
            }

            append("<h2>Subscriptions (${subs.size})</h2>")
            if (subs.isEmpty()) {
                append("<p>No items</p>")
            } else {
                append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">")
                append("<tr><th>Name</th><th>Category</th><th>Price</th><th>Billing cycle</th><th>Next renewal</th><th>Notes</th></tr>")
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                subs.forEach { item ->
                    append("<tr><td>${item.name.escapeHtml()}${if (item.isPinned) " \uD83D\uDCCC" else ""}</td>")
                    append("<td>${item.category.escapeHtml()}</td>")
                    append("<td>${Currencies.format(item.currency, item.price).escapeHtml()}</td>")
                    append("<td>${item.billingCycle.escapeHtml()}</td>")
                    append("<td>${dateFormat.format(Date(item.nextRenewalTimestamp)).escapeHtml()}</td>")
                    append("<td>${item.notes.escapeHtml()}</td></tr>")
                }
                append("</table>")
            }

            append("<h2>Notes (${notes.size})</h2>")
            if (notes.isEmpty()) {
                append("<p>No items</p>")
            } else {
                notes.forEach { note ->
                    append("<h3>${note.title.escapeHtml()}${if (note.isPinned) " \uD83D\uDCCC" else ""}</h3>")
                    if (note.isLocked) {
                        append("<p><i>Locked note \u2014 content hidden</i></p>")
                    } else {
                        append("<p>${note.content.escapeHtml().replace("\n", "<br>")}</p>")
                    }
                }
            }
            append("</body></html>")
        }
        val bytes = html.toByteArray(Charsets.UTF_8)
        val uri = saveBytes(fileName, "application/msword") { out -> out.write(bytes) }
        return ExportResult(uri, fileName, "Downloads/RadiantGuard/$fileName")
    }

    // ---------------------------------------------------------------- helpers

    private enum class LineStyle { HEADING, BODY, EMPTY }

    private fun buildPlainLines(
        vault: List<DecryptedVaultItem>,
        subs: List<SubscriptionItem>,
        notes: List<NoteItem>
    ): List<Pair<String, LineStyle>> {
        val out = mutableListOf<Pair<String, LineStyle>>()
        fun h(text: String) = out.add(text to LineStyle.HEADING)
        fun b(text: String) = out.add(text to LineStyle.BODY)
        fun e() = out.add("" to LineStyle.EMPTY)

        h("VAULT (${vault.size} item${if (vault.size == 1) "" else "s"})")
        if (vault.isEmpty()) b("No items") else vault.forEach { item ->
            val pin = if (item.isPinned) " [pinned]" else ""
            b("${item.title}$pin")
            b("  Username: ${item.username}")
            b(if (item.isLocked) "  Password: \u2022\u2022\u2022\u2022\u2022\u2022 (locked item \u2014 password hidden)" else "  Password: ${item.password}")
            if (item.url.isNotEmpty()) b("  URL: ${item.url}")
            if (item.notes.isNotEmpty()) b("  Notes: ${item.notes}")
            if (item.totpSecret.isNotEmpty()) b(if (item.isLocked) "  2FA secret: \u2022\u2022\u2022\u2022\u2022\u2022" else "  2FA secret: ${item.totpSecret}")
            e()
        }

        h("SUBSCRIPTIONS (${subs.size})")
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        if (subs.isEmpty()) b("No items") else subs.forEach { item ->
            val pin = if (item.isPinned) " [pinned]" else ""
            b("${item.name}$pin \u2014 ${Currencies.format(item.currency, item.price)} / ${item.billingCycle}")
            b("  Category: ${item.category}  |  Next renewal: ${dateFormat.format(Date(item.nextRenewalTimestamp))}")
            if (item.notes.isNotEmpty()) b("  Notes: ${item.notes}")
            e()
        }

        h("NOTES (${notes.size})")
        if (notes.isEmpty()) b("No items") else notes.forEach { note ->
            val pin = if (note.isPinned) " [pinned]" else ""
            b("${note.title}$pin")
            if (note.isLocked) b("  (Locked note \u2014 content hidden)") else if (note.content.isNotBlank()) b("  ${note.content}")
            e()
        }
        return out
    }

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /**
     * Saves bytes to Downloads/RadiantGuard via MediaStore (API 29+), falling
     * back to the public Downloads folder on older devices. Returns the Uri
     * to share (no FileProvider needed for a MediaStore Uri).
     */
    private fun saveBytes(fileName: String, mimeType: String, write: (java.io.OutputStream) -> Unit): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RadiantGuard")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create export file")
            resolver.openOutputStream(uri)?.use { write(it) }
                ?: throw IllegalStateException("Could not write export file")
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RadiantGuard")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { write(it) }
            Uri.fromFile(file)
        }
    }
}
