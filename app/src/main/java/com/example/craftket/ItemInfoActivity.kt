package com.example.craftket

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.craftket.databinding.ActivityItemInfoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.util.Log
import android.view.View
import com.example.craftket.Models.Activity
import com.example.craftket.utilites.Constants
import com.google.gson.Gson
import java.util.Locale
import androidx.core.net.toUri
import com.example.craftket.adapters.ImageSliderAdapter
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*


class ItemInfoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityItemInfoBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var addressCoordinates: Pair<Double, Double>? = null
    val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var activityIndex: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemInfoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        showLoading()

        coroutineScope.launch {
            val json = intent.getStringExtra(Constants.BundleKeys.ACTIVITY)
            val currentActivity = gson.fromJson(json, Activity::class.java)
            activityIndex = intent.getIntExtra(Constants.BundleKeys.ACTIVITY_INDEX, -1)
            val address = currentActivity.location.getFullAddress()

            addressCoordinates = withContext(Dispatchers.IO) {
                getLatLongFromAddress(this@ItemInfoActivity, address)
            }

            initViews(currentActivity)
            Log.d("finished", "finish")
            hideLoading()
        }
    }

    private fun setSlidingPictures() {
        // set sliding images
        val pageMarginPx = (8 * resources.displayMetrics.density).toInt()  // 8dp in pixels
        val pageOffsetPx = (20 * resources.displayMetrics.density).toInt() // 20dp in pixels

        binding.infoViewpagerSlidingImages.setPageTransformer { page, position ->
            val offset = position * -(2 * pageOffsetPx + pageMarginPx)
            page.translationX = offset

            // Optional: scale effect on side pages
            val scale = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
            page.scaleY = scale
            page.scaleX = scale
        }
        val dbRef = FirebaseDatabase.getInstance().getReference("activities").child(activityIndex.toString()).child("additionalImages")

        dbRef.get().addOnSuccessListener { result ->
            val urls = mutableListOf<String>()
            for (child in result.children) {
                val url = child.getValue(String::class.java)
                if (!url.isNullOrEmpty()) {
                    urls.add(url)
                }
            }
            binding.infoViewpagerSlidingImages.adapter = ImageSliderAdapter(urls)
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Failed to load image URLs", e)
        }
    }

    private fun initViews (currentActivity: Activity) {
        mapFragment = supportFragmentManager.findFragmentById(R.id.info_FRAGMENT_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.infoTXTActivityName.text = currentActivity.name
        binding.infoTXTPrice.text = currentActivity.price.toString()
        // fix levels appearance
        binding.infoTXTLevels.text = currentActivity.levels.joinToString(" | ") {
            it.name.lowercase().replaceFirstChar { it.uppercase() }
        }
        binding.infoTXTAddress.text = currentActivity.location.getFullAddress()
        addressCoordinates = getLatLongFromAddress(this, currentActivity.location.getFullAddress())
        binding.infoBTNRegister.setOnClickListener {
            registerForClass(currentActivity)
        }
        binding.infoTXTCancelTime.text = buildString {
            append(currentActivity.cancelTime.toString())
            append(" hours before class")
        }
        val facebookUrl = currentActivity.facebookUrl
        if (!facebookUrl.isNullOrBlank()) {
            binding.infoBTNFacebook.visibility = View.VISIBLE
            binding.infoBTNFacebook.setOnClickListener { openLink(this, facebookUrl) }
        }

        val instagramUrl = currentActivity.instagramUrl
        if (!instagramUrl.isNullOrBlank()) {
            binding.infoBTNInstagram.visibility = View.VISIBLE
            binding.infoBTNInstagram.setOnClickListener { openLink(this, instagramUrl) }
        }

        binding.infoTOOLNavigation.setNavigationOnClickListener { view: View -> finish() }

        setSlidingPictures()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (::googleMap.isInitialized) {
            addressCoordinates?.let { (lat, long) ->
                zoomInMap(lat, long)
            }
        }
    }

    private fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    private fun showLoading() {
        binding.infoProgressbar.visibility = View.VISIBLE
        binding.infoMainContent.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.infoProgressbar.visibility = View.GONE
        binding.infoMainContent.visibility = View.VISIBLE
    }


    private fun registerForClass(currentActivity: Activity) {
        val intent = Intent(this, ClassRegisterActivity::class.java)
        intent.putExtra(Constants.BundleKeys.TIMESLOTS, gson.toJson(currentActivity.schedule) )
        intent.putExtra(Constants.BundleKeys.ACTIVITY_INDEX, activityIndex )
        startActivity(intent)
    }

    private fun zoomInMap(lat: Double, lon: Double) {
        val location = LatLng(lat, lon)
        googleMap.addMarker(MarkerOptions().position(location))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
    }

    private fun getLatLongFromAddress(context: Context, address: String): Pair<Double, Double>? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses!!.isNotEmpty()) {
                val location = addresses[0]
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}