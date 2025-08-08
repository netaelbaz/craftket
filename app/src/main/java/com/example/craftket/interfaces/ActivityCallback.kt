package com.example.craftket.interfaces

import com.example.craftket.Models.Activity

interface ActivityCallback {
    fun moreInfoClicked(activity: Activity, key: String)
}