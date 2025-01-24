package com.rohit.splitsmart.Model

data class Payment(
    val id: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",  // userId of person making payment
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)