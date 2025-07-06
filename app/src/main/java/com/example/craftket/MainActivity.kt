package com.example.craftket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.craftket.Models.Activity
import com.example.craftket.adapters.ActivityAdapter
import com.example.craftket.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ActivityAdapter
    var activityList = mutableListOf<Activity>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
//        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        adapter = ActivityAdapter(mutableListOf())
        binding.mainRVList.layoutManager = LinearLayoutManager(this)
        binding.mainRVList.adapter = adapter
//        val activityAdapter = ActivityAdapter(activityList)
    }

    private fun getDatabaseReference(path: String): DatabaseReference {
        val database = Firebase.database
        return database.getReference(path)
    }

    private fun getMessageFromDB() {
        val ref = getDatabaseReference("activities")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activityList = mutableListOf<Activity>()
                for (data in dataSnapshot.children) {
                    val activity = data.getValue(Activity::class.java)
                    activity?.let { activityList.add(it) }
                }
                adapter.updateData(activityList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("Data Error", "Failed to read value.", error.toException())
            }
        })
    }



    private fun initViews() {
        binding.mainBTNSignOut.setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    switchActivity()
                }
        }

        getMessageFromDB()

//        binding.mainBTNSave.setOnClickListener {
//            val myRef = getDatabaseReference(Constants.DB.MESSAGE_REF)
//            myRef.setValue(binding.mainETMessage.text.toString())
//        }
//        getMessageFromDB(binding.mainLBLMessage)

    }

    private fun switchActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}