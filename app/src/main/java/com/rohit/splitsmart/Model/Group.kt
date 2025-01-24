package com.rohit.splitsmart.Model

// Data class for Group
data class Group(
    var id: String = "",
    val name: String = "",
    val members: Map<String, Member> = emptyMap(),
    val totalExpense: Double = 0.0,
    val createdAt: Long = 0,
    val createdBy: String = ""
)