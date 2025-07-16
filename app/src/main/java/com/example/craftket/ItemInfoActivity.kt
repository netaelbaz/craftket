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
import android.view.View
import com.example.craftket.Models.Activity
import com.example.craftket.utilites.Constants
import com.google.gson.Gson
import java.util.Locale
import androidx.core.net.toUri

class ItemInfoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityItemInfoBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var addressCoordinates: Pair<Double, Double>? = null
    val gson = Gson()


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
        initViews()
    }

    private fun initViews () {
        mapFragment = supportFragmentManager.findFragmentById(R.id.info_FRAGMENT_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val json = intent.getStringExtra(Constants.BundleKeys.ACTIVITY)
        val currentActivity = gson.fromJson(json, Activity::class.java)
        binding.infoTXTActivityName.text = currentActivity.name
        binding.infoTXTPrice.text = buildString {
            append(currentActivity.price.toString())
            append("₪")
        }
        binding.infoTXTAddress.text = currentActivity.location.getFullAddress()
        addressCoordinates = getLatLongFromAddress(this, currentActivity.location.getFullAddress())
        binding.infoBTNRegister.setOnClickListener {
            registerForClass()
        }
        binding.infoTXTCancelTime.text = buildString {
            append(currentActivity.cancelTime.toString())
            append(" hours before class")
        }
        val facebookUrl = currentActivity.facebookUrl
        if (facebookUrl != null) {
            binding.infoBTNFacebook.visibility = View.VISIBLE
            binding.infoBTNFacebook.setOnClickListener { openLink(this, facebookUrl) }
        }

        val instagramUrl = currentActivity.instagramUrl
        if (instagramUrl != null) {
            binding.infoBTNInstagram.visibility = View.VISIBLE
            binding.infoBTNInstagram.setOnClickListener { openLink(this, instagramUrl) }
        }

        binding.infoTOOLNavigation.setNavigationOnClickListener { view: View -> finish() }
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

    private fun registerForClass() {
        println("register clicked")
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