package com.rohit.splitsmart.Model

data class Expense(
    var id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: Long = 0,
    val paidBy: String = "",
    val split: Map<String, Double> = emptyMap(),
    val description: String = "",
    val category: String = "",
    val receiptUrl: String = "",
    var settled: Boolean = false // Tracks if the expense is settled
)
