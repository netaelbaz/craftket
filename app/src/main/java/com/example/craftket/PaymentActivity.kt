package com.example.craftket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.craftket.Models.Activity
import com.example.craftket.databinding.ActivityPaymentBinding
import com.example.craftket.utilites.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
    }

    fun floatToHour24(hour: Float): String {
        val h = hour.toInt()
        val m = ((hour - h) * 60).toInt()
        return String.format("%02d:%02d", h, m)
    }


    private fun initViews() {
        val activityIndex = intent.getStringExtra(Constants.BundleKeys.ACTIVITY_INDEX)
        val timeSlotIndex = intent.getIntExtra(Constants.BundleKeys.SELECTED_TIME_SLOT, -1)
        if (activityIndex != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("activities").child(activityIndex.toString())

            dbRef.get().addOnSuccessListener { snapshot ->
                val activity = snapshot.getValue(Activity::class.java)
                if (activity != null && timeSlotIndex != -1) {
                    val timeSlot = activity.schedule[timeSlotIndex]
                    binding.payTXTTitle.text = activity.name
                    binding.payTXTHour.text = buildString {
                        append(floatToHour24(timeSlot.startTime))
                        append(" - ")
                        append(floatToHour24(timeSlot.endTime))
                    }
                    binding.payTXTDate.text = timeSlot.date
                    binding.payTXTPrice.text = buildString {
                        append("Total: ")
                        append(activity.price.toString())}
                }
            }.addOnFailureListener {
                Log.e("Payment activity", "failed to read from database")
            }
        }

        binding.payBTNSubmit.setOnClickListener {
            registerToClass(timeSlotIndex)
        }

        binding.payTOOLNavigation.setNavigationOnClickListener { view: View -> finish() }
    }

    private fun registerToClass(selectedTimeSlot: Int) {
        if (selectedTimeSlot == -1) {
            Log.e("Register Failed", "No selected time slot")
        }
        val usersRef = dbRef.child("schedule").child(selectedTimeSlot.toString()).child("registeredUsers")
        val user = FirebaseAuth.getInstance().currentUser
        if (user!= null) {
            usersRef.get().addOnSuccessListener { snapshot ->
                val currentUsers = snapshot.getValue(object : GenericTypeIndicator<MutableList<String>>() {}) ?: mutableListOf()

                if (!currentUsers.contains(user.uid)) {
                    currentUsers.add(user.uid)
                    usersRef.setValue(currentUsers)
                    showPaymentDialog()
                } else {
                    Log.d("Register Failed", "User already registered.")
                }
            }.addOnFailureListener {
                Log.e("Register Failed", "Failed to access database", it)
            }
        }
    }

    private fun showPaymentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_successful, null)
        val scoresButton: MaterialButton = dialogView.findViewById(R.id.dialog_BTN_back_home)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        scoresButton.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, SearchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}