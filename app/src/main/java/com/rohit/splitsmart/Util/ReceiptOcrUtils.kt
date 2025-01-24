package com.rohit.splitsmart.Util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rohit.splitsmart.Model.ExtractedReceiptData
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptOcrUtils {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val dateFormats = listOf(
        "MM/dd/yyyy",
        "MM-dd-yyyy",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "MM/dd/yy"
    ).map { SimpleDateFormat(it, Locale.US) }

    // Common words that indicate total amount
    private val totalIndicators = listOf(
        "total",
        "balance",
        "amount due",
        "amount to pay",
        "grand total",
        "final total",
        "payment total"
    )

    // Words to exclude when looking for totals
    private val excludedTotalWords = listOf(
        "subtotal",
        "sub-total",
        "sub total",
        "tax",
        "tip",
        "discount"
    )

    suspend fun extractReceiptData(context: Context, imageUri: Uri): ExtractedReceiptData {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val image = InputImage.fromBitmap(bitmap, 0)

        val visionText = recognizer.process(image).await()
        val rawText = visionText.text

        // Process text into clean lines for better parsing
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        return ExtractedReceiptData(
            totalAmount = findTotalAmount(lines),
            merchantName = findMerchantName(visionText.textBlocks),
            date = findDate(rawText),
            rawText = formatDescription(lines)
        )
    }

    private fun findTotalAmount(lines: List<String>): Double? {
        // First try to find amounts with total indicators
        val totalLine = lines.findLast { line ->
            val lowerLine = line.lowercase()
            totalIndicators.any { indicator ->
                lowerLine.contains(indicator) &&
                        excludedTotalWords.none { excluded -> lowerLine.contains(excluded) }
            }
        }

        totalLine?.let { line ->
            extractAmount(line)?.let { return it }
        }

        // If no clear total found, look for the last currency amount in the receipt
        val currencyPattern = "\\$?\\d+\\.\\d{2}".toRegex()
        return lines.asReversed()
            .mapNotNull { line ->
                if (excludedTotalWords.none { excluded -> line.lowercase().contains(excluded) }) {
                    extractAmount(line)
                } else null
            }
            .firstOrNull()
    }

    private fun extractAmount(text: String): Double? {
        val pattern = "\\$?(\\d+,?\\d*\\.\\d{2})".toRegex()
        return pattern.find(text)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    private fun findMerchantName(textBlocks: List<com.google.mlkit.vision.text.Text.TextBlock>): String? {
        // Look for merchant name in first few lines
        for (i in 0..minOf(2, textBlocks.size - 1)) {
            val lines = textBlocks[i].text.lines()
            for (line in lines) {
                val cleanLine = line.trim()
                if (isLikelyMerchantName(cleanLine)) {
                    return cleanLine
                }
            }
        }
        return null
    }

    private fun isLikelyMerchantName(line: String): Boolean {
        // Exclude lines that look like dates, amounts, addresses, or are too short
        return line.length in 3..50 &&
                !line.matches(".*\\d{2}[/-]\\d{2}[/-]\\d{2,4}.*".toRegex()) &&
                !line.matches(".*\\$?\\d+\\.\\d{2}.*".toRegex()) &&
                !line.matches(".*\\d{5,}.*".toRegex()) && // Exclude phone numbers, postal codes
                !line.lowercase().contains("tel:") &&
                !line.lowercase().contains("phone") &&
                !line.matches(".*@.*".toRegex()) // Exclude email addresses
    }

    private fun findDate(text: String): Date? {
        dateFormats.forEach { format ->
            val pattern = "\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b".toRegex()
            pattern.findAll(text).forEach { match ->
                try {
                    return format.parse(match.value)
                } catch (e: ParseException) {
                    // Continue to next format
                }
            }
        }
        return null
    }

    private fun formatDescription(lines: List<String>): String {
        // Filter out irrelevant lines and format the description
        return lines.filter { line ->
            // Keep only meaningful lines
            line.length > 3 &&
                    !line.matches("^[-=_*]{3,}$".toRegex()) && // Remove separator lines
                    !line.matches("^\\s*$".toRegex()) && // Remove empty lines
                    !line.lowercase().contains("wifi") &&
                    !line.lowercase().contains("password") &&
                    !line.matches(".*\\d{3,}[-.]\\d{3,}[-.]\\d{4}.*".toRegex()) && // Remove phone numbers
                    !line.matches(".*@.*\\..*".toRegex()) // Remove email addresses
        }.joinToString("\n") { it.trim() }
            .take(500) // Limit description length
    }
}