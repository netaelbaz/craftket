package com.example.craftket.Models


data class Filters(
    val selectedCity: String? = null,
    val selectedTypes: List<String>? = null,
    val selectedLevels: List<ActivityLevel>? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null
) {
    class Builder {
        private var selectedCity: String? = null
        private var selectedTypes: List<String> = emptyList()
        private var selectedLevels: List<ActivityLevel> = emptyList()
        private var minPrice: Int = 0
        private var maxPrice: Int = 0
        private var startDate: String? = null
        private var endDate: String? = null

        fun setCities(city: String?) = apply { this.selectedCity = city }
        fun setTypes(types: List<String>) = apply { this.selectedTypes = types }
        fun setLevels(levels: List<ActivityLevel>) = apply { this.selectedLevels = levels }
        fun setPriceRange(min: Int, max: Int) = apply {
            this.minPrice = min
            this.maxPrice = max
        }

        fun setStartDate(date: String?) = apply { this.startDate = date }
        fun setEndDate(date: String?) = apply { this.endDate = date }

        fun build() = Filters(selectedCity, selectedTypes, selectedLevels ,minPrice, maxPrice, startDate, endDate)
    }
}