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
    var dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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

        setLocationOptions()
        setActivityOptions()
        displayDate()
        binding.filtersBTNBack.setOnClickListener {
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
                binding.filtersTXTMinPrice.setText(buildString{
                    append(values[0])
                    append("₪")
                })
                binding.filtersTXTMaxPrice.setText(buildString{
                    append(values[1])
                    append("₪")
                })
            }
        })

        binding.filtersTXTReset.setOnClickListener { resetFilters() }
        binding.filtersBTNApply.setOnClickListener { applyFilters() }
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
        val priceRange = binding.filtersSLIDERPrice.values
        val startDateStr = startDate?.let { dateFormatter.format(it) }
        val endDateStr = endDate?.let { dateFormatter.format(it) }
        val resultIntent = Intent()
        val filters = Filters.Builder()
            .setCities(selectedCity.takeIf { it.isNotBlank() })
            .setTypes(selectedTypes) // your custom method
            .setPriceRange(priceRange[0], priceRange[1])
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

    }
}