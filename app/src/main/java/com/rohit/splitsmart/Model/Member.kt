package com.rohit.splitsmart.Model


data class Member(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val balance: Double = 0.0,
    val settled: Boolean = false, // Add the settled property
    var splitAmount: Double = 0.0
    )

