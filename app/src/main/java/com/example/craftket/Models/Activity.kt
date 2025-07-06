package com.example.craftket.Models


data class Activity (
    var name : String = "",
    var location: Location = Location(),
    var price: Float = 0f,
    var levels: List<ActivityLevel> = emptyList(),
    var cancelTime: Int = 24,
    var schedule: List<TimeSlot> = emptyList(),
    var imageUrl: String = "",
    var facebookUrl: String? = null,
    var instagramUrl: String? = null
)
