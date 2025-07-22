package com.example.craftket.Models

data class TimeSlot(
    val date: String = "",
    val startTime: Float = 0f,
    val endTime: Float = 24f,
    val capacity: Int = 0,
    val registeredUsers: List<String> = emptyList()
)