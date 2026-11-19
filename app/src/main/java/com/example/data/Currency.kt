package com.example.data

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** One supported currency: ISO code, display symbol and country label. */
data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val country: String,
    /** BCP-47 locale used for NumberFormat; falls back to symbol formatting when unknown. */
    val localeTag: String = "en-US"
)

object Currencies {
    val ALL: List<CurrencyInfo> = listOf(
        CurrencyInfo("PKR", "Rs", "Pakistan", "en-PK"),
        CurrencyInfo("INR", "\u20B9", "India", "en-IN"),
        CurrencyInfo("USD", "$", "USA", "en-US"),
        CurrencyInfo("GBP", "\u00A3", "England / UK", "en-GB"),
        CurrencyInfo("RUB", "\u20BD", "Russia", "ru-RU"),
        CurrencyInfo("EUR", "\u20AC", "Eurozone", "de-DE"),
        CurrencyInfo("AED", "\u062F.\u0625", "UAE", "ar-AE"),
        CurrencyInfo("SAR", "\uFDFC", "Saudi Arabia", "ar-SA"),
        CurrencyInfo("CNY", "\u00A5", "China", "zh-CN"),
        CurrencyInfo("JPY", "\u00A5", "Japan", "ja-JP"),
        CurrencyInfo("TRY", "\u20BA", "Turkey", "tr-TR"),
        CurrencyInfo("AUD", "$", "Australia", "en-AU"),
        CurrencyInfo("CAD", "$", "Canada", "en-CA"),
        CurrencyInfo("BDT", "\u09F3", "Bangladesh", "bn-BD"),
        CurrencyInfo("NPR", "Rs", "Nepal", "ne-NP"),
        CurrencyInfo("LKR", "Rs", "Sri Lanka", "si-LK"),
        CurrencyInfo("KRW", "\u20A9", "South Korea", "ko-KR"),
        CurrencyInfo("THB", "\u0E3F", "Thailand", "th-TH"),
        CurrencyInfo("MYR", "RM", "Malaysia", "ms-MY"),
        CurrencyInfo("CHF", "CHF", "Switzerland", "de-CH")
    )

    val DEFAULT: CurrencyInfo = ALL.first { it.code == "PKR" }

    fun forCode(code: String?): CurrencyInfo {
        if (code.isNullOrBlank()) return DEFAULT
        return ALL.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: DEFAULT
    }

    /**
     * Normalises a stored currency value to an ISO code. Old rows stored the raw
     * symbol ("$", "\u20B9", ...) or an empty string. "$" was the old default and
     * almost always meant a Pakistan-priced subscription, so it maps to PKR;
     * other recognised symbols map to their code.
     */
    fun normalizeCurrency(stored: String?): String {
        if (stored.isNullOrBlank()) return "PKR"
        val value = stored.trim()
        if (value.length == 3 && value.all { it.isLetter() }) {
            return ALL.firstOrNull { it.code.equals(value, ignoreCase = true) }?.code ?: "PKR"
        }
        return when (value) {
            "$" -> "PKR" // old default; user is in Pakistan
            "\u20B9" -> "INR"
            "\u00A3" -> "GBP"
            "\u20BD" -> "RUB"
            "\u20AC" -> "EUR"
            "\u062F.\u0625", "AED" -> "AED"
            "\uFDFC" -> "SAR"
            "\u00A5" -> "JPY"
            "\u20BA" -> "TRY"
            "\u09F3" -> "BDT"
            "\u20A9" -> "KRW"
            "\u0E3F" -> "THB"
            "RM" -> "MYR"
            "CHF" -> "CHF"
            "Rs" -> "PKR"
            else -> "PKR"
        }
    }

    /** Formats [amount] in [code], e.g. "Rs 1,250.00" / "$9.99". Falls back to "CODE 1,234.56". */
    fun format(code: String, amount: Double): String {
        val normalized = normalizeCurrency(code)
        return try {
            val locale = Locale.forLanguageTag(forCode(normalized).localeTag)
            val fmt = NumberFormat.getCurrencyInstance(locale)
            fmt.currency = Currency.getInstance(normalized)
            fmt.format(amount)
        } catch (e: Exception) {
            val grouped = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(amount)
            "$normalized $grouped"
        }
    }
}
