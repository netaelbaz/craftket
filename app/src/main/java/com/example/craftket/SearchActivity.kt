package com.example.craftket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.craftket.Models.Activity
import com.example.craftket.Models.Filters
import com.example.craftket.adapters.ActivityAdapter
import com.example.craftket.databinding.ActivitySearchBinding
import com.example.craftket.interfaces.ActivityCallback
import com.example.craftket.utilites.Constants
import com.example.craftket.utilites.Utils.signOut
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale


class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: ActivityAdapter
    private var activityPairs = mutableListOf<Pair<String, Activity>>()
    private lateinit var citiesList: List<String>
    private lateinit var activityTypesList: List<String>
    private lateinit var filtersLauncher: ActivityResultLauncher<Intent>
    private var lastUsedFilters: Filters? = null
    val gson = Gson()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        adapter = ActivityAdapter(mutableListOf())
        binding.searchRVList.layoutManager = LinearLayoutManager(this)
        binding.searchRVList.adapter = adapter
        adapter.activityCallback = object : ActivityCallback {
            override fun moreInfoClicked(activity: Activity, key: String) {
                // Handle "more info" button click
                openMoreInfo(activity, key)
            }
        }
    }

    private fun getDatabaseReference(path: String): DatabaseReference {
        val database = Firebase.database
        return database.getReference(path)
    }

    private fun handleNoResults(result: List<Pair<String, Activity>>) {
        if (result.isEmpty()) {
            binding.searchTXTNoActivities.visibility = View.VISIBLE
        } else {
            binding.searchTXTNoActivities.visibility = View.GONE
        }
    }


    private fun getActivitiesFromDB() {
        val ref = getDatabaseReference("activities")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activityPairs = dataSnapshot.children.mapNotNull { child ->
                    val activity = child.getValue(Activity::class.java)
                    val key = child.key
                    if (activity != null && key != null) key to activity else null
                }.toMutableList()

                handleNoResults(activityPairs)
                adapter.updateData(activityPairs)

                citiesList = activityPairs
                    .map { it.second.location.city }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                activityTypesList = activityPairs
                    .map { it.second.field.displayName }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            }


            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("Data Error", "Failed to read value.", error.toException())
            }
        })
    }


    private fun initViews() {
        binding.searchBTNSignOut.setOnClickListener {
            signOut(this)
        }

        getActivitiesFromDB()

        binding.searchBTNFilters.setOnClickListener {
            openFiltersActivity()
        }

        registerForFilters()
        binding.searchToolbar.setNavigationOnClickListener { view: View -> finish() }
    }


    private fun registerForFilters() {
        filtersLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val filtersAsJson = data?.getStringExtra("FILTERS")
                    if (filtersAsJson != null) {
                        val filters = gson.fromJson(filtersAsJson, Filters::class.java)
                        Log.d("Filters", filters.toString())
                        applyFilters(filters)
                    }
                }
            }
    }

    private fun openMoreInfo(activity: Activity, key: String) {
        val intent = Intent(this, ItemInfoActivity::class.java)
        val activityAsJson = gson.toJson(activity)
        intent.putExtra(Constants.BundleKeys.ACTIVITY, activityAsJson)
        intent.putExtra(Constants.BundleKeys.ACTIVITY_INDEX, key)
        startActivity(intent)
    }

    private fun openFiltersActivity() {
        val intent = Intent(this, FiltersActivity::class.java)
        var bundle = Bundle()
        bundle.putStringArrayList(Constants.BundleKeys.CITIES_LIST, ArrayList(citiesList))
        bundle.putStringArrayList(Constants.BundleKeys.TYPES_LIST, ArrayList(activityTypesList))
        lastUsedFilters?.let {
            val filtersJson = gson.toJson(it)
            bundle.putString(Constants.BundleKeys.FILTERS, filtersJson)
        }

        intent.putExtras(bundle)
        filtersLauncher.launch(intent)
    }

    private fun applyFilters(filters: Filters) {
        lastUsedFilters = filters
        val filteredPairs = activityPairs.filter { (_, activity) ->

            val locationMatches = filters.selectedCity.isNullOrBlank() || activity.location.city == filters.selectedCity
            val typesMatches = filters.selectedTypes.isNullOrEmpty() || filters.selectedTypes.contains(activity.field.displayName)
            val levelMatches = filters.selectedLevels.isNullOrEmpty() || activity.levels.any { it in filters.selectedLevels }
            val minPriceMatches = filters.minPrice == null || activity.price >= filters.minPrice
            val maxPriceMatches = filters.maxPrice == null || activity.price <= filters.maxPrice

            val dateFormatter = SimpleDateFormat(Constants.Format.DATE_FORMATTER, Locale.getDefault())
            val startDate =filters.startDate?.let { dateFormatter.parse(it) }
            val endDate =filters.endDate?.let { dateFormatter.parse(it) }
            val dateMatches = if (startDate != null && endDate != null) {
                activity.schedule.any { timeSlot ->
                    try {
                        val slotDate = dateFormatter.parse(timeSlot.date)
                        slotDate != null && slotDate >= startDate && slotDate <= endDate
                    } catch (_: Exception) {
                        false
                    }
                }
            } else {
                true // If no date filter applied
            }

            locationMatches && typesMatches && minPriceMatches && maxPriceMatches && dateMatches && levelMatches
        }

        handleNoResults(filteredPairs)
        adapter.updateData(filteredPairs)

    }
}