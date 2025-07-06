package com.example.craftket.Models

data class Location(
    var city: String = "",
    var street: String = "",
    var number: Int = 0
) {
    fun getFullAddress(): String {
        return "$street, $number, $city"
    }
}