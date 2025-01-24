package com.rohit.splitsmart.Model

import java.util.Date

data class ExtractedReceiptData(
    val totalAmount: Double?,
    val merchantName: String?,
    val date: Date?,
    val rawText: String
)