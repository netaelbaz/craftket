package com.example.craftket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.craftket.databinding.ActivityMainBinding
import com.example.craftket.utilites.Utils.signOut
import com.firebase.ui.auth.AuthUI


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
    }

    private fun initViews() {
        binding.mainBTNSearch.setOnClickListener {
            // save in reference and load main activity
            Log.d("Activity Switch", "moving to main activity")
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.mainBTNAdd.setOnClickListener {
            Log.d("Activity Switch", "moving to add item activity")
            startActivity(Intent(this, AddItemActivity::class.java))
        }

        binding.mainBTNSignOut.setOnClickListener {
            signOut(this)
        }
    }


}