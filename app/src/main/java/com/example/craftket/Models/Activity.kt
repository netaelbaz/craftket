package com.example.craftket.Models


data class Activity (
    var name : String = "",
    var location: Location = Location(),
    var price: Int = 0,
    var levels: List<ActivityLevel> = emptyList(),
    var cancelTime: Int = 24,
    var schedule: List<TimeSlot> = emptyList(),
    var imageUrl: String = "",
    var field: ActivityType = ActivityType.SPORT,
    var facebookUrl: String? = null,
    var instagramUrl: String? = null,
    var additionalImages: List<String> = emptyList()
)
