package com.example.craftket

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.craftket.Models.ActivityLevel
import com.example.craftket.Models.Filters
import com.example.craftket.databinding.ActivityFiltersBinding
import com.example.craftket.utilites.Constants
import com.google.android.material.slider.RangeSlider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.gson.Gson

class FiltersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFiltersBinding
    private lateinit var textInput: AutoCompleteTextView
    private lateinit var bundle: Bundle
    private var startDate: Date? = null
    private var endDate: Date? = null
    var dateFormatter = SimpleDateFormat(Constants.Format.DATE_FORMATTER, Locale.getDefault())
    private var receivedFilters: Filters? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiltersBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        textInput = findViewById(R.id.filters_TEXT_locationSearch)
        initViews()
    }

    private fun initViews() {
        bundle = intent.extras ?: Bundle()

        val filtersJson = bundle.getString(Constants.BundleKeys.FILTERS)
        filtersJson?.let {
            receivedFilters = Gson().fromJson(it, Filters::class.java)
        }

        setLocationOptions()
        setActivityOptions()
        setLevelsFilter()
        displayDate()

        applyReceivedFilters()

        binding.filtersToolbar.setNavigationOnClickListener {
            finish()
        }
        binding.filtersTXTMinPrice.doAfterTextChanged { text ->
            val min = text.toString().toFloatOrNull()
            val max = binding.filtersTXTMaxPrice.text.toString().toFloatOrNull()
            if (min != null && max != null && min <= max) {
                binding.filtersSLIDERPrice.values = listOf(min, max)
            }
        }
        binding.filtersTXTMaxPrice.doAfterTextChanged { text ->
            val max = text.toString().toFloatOrNull()
            val min = binding.filtersTXTMinPrice.text.toString().toFloatOrNull()
            if (min != null && max != null && min <= max) {
                binding.filtersSLIDERPrice.values = listOf(min, max)
            }
        }
        binding.filtersSLIDERPrice.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {
                // do nothing
            }
            override fun onStopTrackingTouch(slider: RangeSlider) {
                val values = slider.values
                binding.filtersTXTMinPrice.setText(values[0].toInt().toString())
                binding.filtersTXTMaxPrice.setText(values[1].toInt().toString())
            }
        })

        binding.filtersBTNReset.setOnClickListener { resetFilters() }
        binding.filtersBTNApply.setOnClickListener { applyFilters() }
    }


    private fun setLevelsFilter() {
        val levels = ActivityLevel.entries.toTypedArray()

        val checkBoxes = listOf(
            binding.filtersCHECKBOXBeginner,
            binding.filtersCHECKBOXAdvanced,
            binding.filtersCHECKBOXExpert
        )

        levels.forEachIndexed { index, level ->
            checkBoxes[index].text = level.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun setLocationOptions() {
        val locationItems = bundle.getStringArrayList(Constants.BundleKeys.CITIES_LIST)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locationItems ?: arrayListOf())
                binding.filtersTEXTLocationSearch.threshold = 1
        binding.filtersTEXTLocationSearch.setAdapter(adapter)
    }

    private fun setActivityOptions() {
        val options = bundle.getStringArrayList(Constants.BundleKeys.TYPES_LIST)
        options?.forEachIndexed { index, text ->
            val radioButton = CheckBox(this).apply {
                id = View.generateViewId()
                this.text = text
                this.textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            binding.filtersCHECKBOXGroup.addView(radioButton)
        }
    }


    private fun showDatePicker(initialDate: Calendar, onDateSelected: (Date) -> Unit) {
        val year = initialDate.get(Calendar.YEAR)
        val month = initialDate.get(Calendar.MONTH)
        val day = initialDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonth, selectedDay)
                onDateSelected(cal.time)
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private fun displayDate() {
        binding.filtersTXTStartDate.setOnClickListener {
            showDatePicker(Calendar.getInstance()) { date ->
                startDate = date
                binding.filtersTXTStartDate.text = dateFormatter.format(date)
                validateDates()
            }
        }

        binding.filtersTXTEndDate.setOnClickListener {
            showDatePicker(Calendar.getInstance()) { date ->
                endDate = date
                binding.filtersTXTEndDate.text = dateFormatter.format(date)
                validateDates()
            }
        }
    }

    private fun validateDates() {
        if (startDate != null && endDate != null) {
            if (endDate!!.before(startDate)) {
                // End date is before start date — show error on end date TextView
                binding.filtersTXTEndDate.setTextColor(Color.RED)
                // Optionally add a hint or toast
                binding.filtersTXTEndDate.error = "End date can't be before start date"
            } else {
                // Dates valid — reset error and color
                binding.filtersTXTEndDate.setTextColor(Color.BLACK)
                binding.filtersTXTEndDate.error = null
            }
        } else {
            // Dates not set — reset error and color
            binding.filtersTXTEndDate.setTextColor(Color.BLACK)
            binding.filtersTXTEndDate.error = null
        }
    }


    private fun applyFilters() {
        val selectedCity = binding.filtersTEXTLocationSearch.text.toString()
        val selectedTypes = arrayListOf<String>()
        for (i in 0 until binding.filtersCHECKBOXGroup.childCount) {
            val view = binding.filtersCHECKBOXGroup.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                selectedTypes.add(view.text.toString())
            }
        }
        val levelMap = mapOf(
            binding.filtersCHECKBOXBeginner to ActivityLevel.BEGINNER,
            binding.filtersCHECKBOXAdvanced to ActivityLevel.ADVANCED,
            binding.filtersCHECKBOXExpert to ActivityLevel.EXPERT
        )

        val selectedLevels = levelMap.filter { it.key.isChecked }.values.toList()
        val priceRange = binding.filtersSLIDERPrice.values
        val startDateStr = startDate?.let { dateFormatter.format(it) }
        val endDateStr = endDate?.let { dateFormatter.format(it) }
        val resultIntent = Intent()
        val filters = Filters.Builder()
            .setCities(selectedCity.takeIf { it.isNotBlank() })
            .setTypes(selectedTypes)
            .setLevels(selectedLevels)
            .setPriceRange(priceRange[0].toInt(), priceRange[1].toInt())
            .setStartDate(startDateStr)
            .setEndDate(endDateStr)
            .build()
        val gson = Gson()
        val filtersAsJson = gson.toJson(filters)
        resultIntent.putExtra(Constants.BundleKeys.FILTERS, filtersAsJson)
        setResult(RESULT_OK, resultIntent)
        finish()
    }


    private fun resetFilters() {
        binding.filtersTEXTLocationSearch.setText("")

        for (i in 0 until binding.filtersCHECKBOXGroup.childCount) {
            val view = binding.filtersCHECKBOXGroup.getChildAt(i)
            if (view is CheckBox) {
                view.isChecked = false
            }
        }

        binding.filtersCHECKBOXBeginner.isChecked = false
        binding.filtersCHECKBOXAdvanced.isChecked = false
        binding.filtersCHECKBOXExpert.isChecked = false

        binding.filtersSLIDERPrice.values = listOf(0f, 10000f)

        binding.filtersTXTMinPrice.setText(getString(R.string.default_minimum_price))
        binding.filtersTXTMaxPrice.setText(getString(R.string.default_maximum_price))

        startDate = null
        endDate = null
        binding.filtersTXTStartDate.text = getString(R.string.start_date_filter)
        binding.filtersTXTEndDate.text = getString(R.string.end_date_filter)
        binding.filtersTXTEndDate.setTextColor(Color.BLACK)
        binding.filtersTXTEndDate.error = null
    }


    private fun applyReceivedFilters() {
        val filters = receivedFilters ?: return

        filters.selectedCity?.let {
            binding.filtersTEXTLocationSearch.setText(it, false)
        }

        filters.selectedTypes?.let { selected ->
            for (i in 0 until binding.filtersCHECKBOXGroup.childCount) {
                val view = binding.filtersCHECKBOXGroup.getChildAt(i)
                if (view is CheckBox && view.text in selected) {
                    view.isChecked = true
                }
            }
        }

        filters.minPrice?.let {
            binding.filtersTXTMinPrice.setText(it.toString())
        }
        filters.maxPrice?.let {
            binding.filtersTXTMaxPrice.setText(it.toString())
        }
        if (filters.minPrice != null && filters.maxPrice != null) {
            binding.filtersSLIDERPrice.values = listOf(filters.minPrice.toFloat(), filters.maxPrice.toFloat())
        }
        filters.startDate?.let {
            startDate = dateFormatter.parse(it)
            binding.filtersTXTStartDate.text = it
        }
        filters.endDate?.let {
            endDate = dateFormatter.parse(it)
            binding.filtersTXTEndDate.text = it
        }
        filters.selectedLevels?.let { levels ->
            if (ActivityLevel.BEGINNER in levels) binding.filtersCHECKBOXBeginner.isChecked = true
            if (ActivityLevel.ADVANCED in levels) binding.filtersCHECKBOXAdvanced.isChecked = true
            if (ActivityLevel.EXPERT in levels) binding.filtersCHECKBOXExpert.isChecked = true
        }
    }

}