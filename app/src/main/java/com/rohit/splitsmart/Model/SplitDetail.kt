package com.rohit.splitsmart.Model

data class SplitDetail(
    val userId: String = "",
    val memberName: String = "",
    val amount: Double = 0.0,
    val status: String = "" // "Paid" or "Owed"
)