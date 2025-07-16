package com.example.craftket.Models


data class Filters(
    val selectedCity: String? = null,
    val selectedTypes: List<String>? = null,
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
    val startDate: String? = null,
    val endDate: String? = null
) {
    class Builder {
        private var selectedCity: String? = null
        private var selectedTypes: List<String> = emptyList()
        private var minPrice: Float = 0f
        private var maxPrice: Float = 0f
        private var startDate: String? = null
        private var endDate: String? = null

        fun setCities(cities: String?) = apply { this.selectedCity = cities }
        fun setTypes(types: List<String>) = apply { this.selectedTypes = types }
        fun setPriceRange(min: Float, max: Float) = apply {
            this.minPrice = min
            this.maxPrice = max
        }

        fun setStartDate(date: String?) = apply { this.startDate = date }
        fun setEndDate(date: String?) = apply { this.endDate = date }

        fun build() = Filters(selectedCity, selectedTypes, minPrice, maxPrice, startDate, endDate)
    }
}