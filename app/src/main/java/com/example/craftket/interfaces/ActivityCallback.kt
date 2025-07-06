package com.example.craftket.interfaces

import com.example.craftket.Models.Activity

interface ActivityCallback {
    fun activityClicked(activity: Activity, position: Int)
}