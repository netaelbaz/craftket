package com.example.craftket.Models

data class TimeSlot(
    val date: String = "",
    val startTime: Int = 0,
    val endTime: Int = 24,
    val capacity: Int = 0,
    val registeredUsers: List<String> = emptyList()
)