package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Deterministic readable-text colours for note backgrounds.
 *
 * The old code painted every note card with its light background colour but drew
 * the text with the theme's onSurface colour — in dark mode that is near-white
 * (#EDE0DB) on a near-white card, i.e. invisible. These helpers compute the
 * background's relative luminance and pick a near-black or near-white text
 * colour, so the title/content/date are readable on EVERY offered background
 * in BOTH light and dark themes.
 */
object NoteColors {

    fun parseColor(hex: String, fallback: Color = Color.White): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            fallback
        }
    }

    /** Relative luminance (0 = black, 1 = white) per WCAG. */
    fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    /** Near-black on light backgrounds, near-white on dark backgrounds. */
    fun contentColorForBackground(hex: String): Color =
        if (luminance(parseColor(hex)) > 0.45) Color(0xFF201A17) else Color(0xFFFFFFFF)

    /** Same, but for a gradient style: judged on its first (top) colour. */
    fun contentColorForStyle(backgroundStyle: String, colorHex: String): Color {
        val firstHex = gradientHexes(backgroundStyle).firstOrNull() ?: colorHex
        return contentColorForBackground(firstHex)
    }

    /** "gradient:#HEX1,#HEX2" -> [hex1, hex2]; anything else -> empty list. */
    fun gradientHexes(backgroundStyle: String): List<String> {
        if (!backgroundStyle.startsWith("gradient:")) return emptyList()
        return backgroundStyle.removePrefix("gradient:").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isGradient(backgroundStyle: String): Boolean = backgroundStyle.startsWith("gradient:")

    /** Vertical gradient brush for a gradient style, or null for solid backgrounds. */
    fun gradientBrush(backgroundStyle: String): Brush? {
        val hexes = gradientHexes(backgroundStyle)
        if (hexes.size < 2) return null
        return Brush.verticalGradient(hexes.map { parseColor(it) })
    }

    /** Solid background colour for a note (used when the style is not a gradient). */
    fun solidColor(note: com.example.data.NoteItem): Color = parseColor(note.colorHex)

    // ---- Offered backgrounds (12 solid colours + 4 gradients) ----

    val SOLID_COLORS: List<String> = listOf(
        // Light pastels
        "#FFFFFF", "#FFF7ED", "#F0FDF4", "#F0F9FF", "#FFF1F2", "#F8FAFC",
        // Dark-friendly
        "#1E1B4B", "#312E81", "#3B0764", "#134E4A", "#451A03", "#111827"
    )

    val GRADIENT_STYLES: List<String> = listOf(
        "gradient:#FDE68A,#FCA5A5", // sunrise
        "gradient:#BFDBFE,#DDD6FE", // sky lavender
        "gradient:#312E81,#7C3AED", // deep violet
        "gradient:#134E4A,#0F766E"  // deep teal
    )

    fun gradientLabel(style: String): String = when (style) {
        "gradient:#FDE68A,#FCA5A5" -> "Sunrise"
        "gradient:#BFDBFE,#DDD6FE" -> "Sky Lavender"
        "gradient:#312E81,#7C3AED" -> "Deep Violet"
        "gradient:#134E4A,#0F766E" -> "Deep Teal"
        else -> "Gradient"
    }
}
