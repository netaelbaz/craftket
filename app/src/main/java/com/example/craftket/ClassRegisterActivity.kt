package com.example.craftket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.craftket.Models.Activity
import com.example.craftket.Models.TimeSlot
import com.example.craftket.databinding.ActivityClassRegisterBinding
import com.example.craftket.utilites.Constants
import com.example.craftket.utilites.SignalManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt


class ClassRegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassRegisterBinding
    private var activityIndex: String = ""
    private var selectedSlot = -1
    private var selectedButton: MaterialButton? = null
    private var user: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassRegisterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        user = FirebaseAuth.getInstance().currentUser?.uid
        initViews()
    }

    private fun formatHourToString(value: Float): String {
        val hours = value.toInt()
        val minutes = ((value - hours) * 60).roundToInt()
        return String.format("%02d:%02d", hours, minutes)
    }

    private fun addTimeButton(slot: TimeSlot, index: Int) {
        val button = MaterialButton(this).apply {
            text = buildString {
                append(formatHourToString(slot.startTime))
                append(" - ")
                append(formatHourToString(slot.endTime))
            }
            setTextColor(ContextCompat.getColor(context, R.color.black))
            strokeColor = ContextCompat.getColorStateList(context, R.color.transparent)
            cornerRadius = 32
            strokeWidth = 3
            setOnClickListener {
                slotSelected(this, slot, index)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 16, 16, 16)
            layoutParams = params
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
        }
        binding.registerLAYOUTTimeDisplay.addView(button)
    }



    private fun slotSelected(button: MaterialButton, slot: TimeSlot, index: Int) {
        if (slot.registeredUsers.contains(user)) {
            SignalManager
                .getInstance()
                .toast("You are already subscribed to that time slot")
            binding.registerBTNPick.isEnabled = false
        }
        else {
            binding.registerBTNPick.isEnabled = true
            val placesLeft = slot.capacity - slot.registeredUsers.size
            binding.registerTXTPlacesLeft.text = buildString {
                append(placesLeft)
                append(" places left")
            }
            binding.registerTXTNoTimeSelected.visibility = View.INVISIBLE
            binding.registerTXTPlacesLeft.visibility = View.VISIBLE
            selectedSlot = index
            selectedButton?.let { it.strokeColor = ContextCompat.getColorStateList(this, R.color.transparent) } // reset stroke

            button.strokeColor = ContextCompat.getColorStateList(this, R.color.black)
            selectedButton = button
        }
    }

    private fun registerClicked() {
        if (selectedSlot == -1) {
            binding.registerTXTNoTimeSelected.visibility = View.VISIBLE
        }
        else {
            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra(Constants.BundleKeys.SELECTED_TIME_SLOT, selectedSlot )
            intent.putExtra(Constants.BundleKeys.ACTIVITY_INDEX, activityIndex )
            startActivity(intent)
            }
    }

    private fun displayTimesForSelectedDate(
        matchingSlots: List<TimeSlot>,
        fullSchedule: List<TimeSlot>
    ) {
        if (matchingSlots.isEmpty()) {
            binding.registerTXTNoTimes.visibility = View.VISIBLE
        } else {
            binding.registerTXTNoTimes.visibility = View.INVISIBLE
        }

        matchingSlots.forEach { slot ->
            val realIndex = fullSchedule.indexOfFirst {
                it.date == slot.date &&
                        it.startTime == slot.startTime &&
                        it.endTime == slot.endTime
            }
            addTimeButton(slot, realIndex)
        }
    }

    private fun initViews() {
        activityIndex = intent.getStringExtra(Constants.BundleKeys.ACTIVITY_INDEX) ?: return
        val formatter = DateTimeFormatter.ofPattern(Constants.Format.DATE_FORMATTER, Locale.getDefault())
        binding.registerCALENDARDates.date = System.currentTimeMillis()

        val dbRef = FirebaseDatabase.getInstance().getReference("activities").child(activityIndex.toString())

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activity = snapshot.getValue(Activity::class.java)
                if (activity != null) {
                    val timeSlots = activity.schedule
                    val today = LocalDate.now()
                    // get matching slots for current selected date
                    val todayMatchingSlots = timeSlots.filter {
                        LocalDate.parse(it.date, formatter) == today &&
                                it.capacity > it.registeredUsers.size
                    }
                    binding.registerLAYOUTTimeDisplay.removeAllViews()
                    displayTimesForSelectedDate(todayMatchingSlots, timeSlots)

                    binding.registerCALENDARDates.setOnDateChangeListener { _, year, month, dayOfMonth ->
                        // reset messages
                        binding.registerTXTNoTimeSelected.visibility = View.INVISIBLE
                        binding.registerTXTPlacesLeft.visibility = View.INVISIBLE

                        val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                        val matching = timeSlots.filter {
                            LocalDate.parse(it.date, formatter) == selectedDate &&
                                    it.capacity > it.registeredUsers.size
                        }
                        binding.registerLAYOUTTimeDisplay.removeAllViews()
                        displayTimesForSelectedDate(matching, timeSlots)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RegisterActivity", "Failed to load data", error.toException())
            }
        })

        binding.registerBTNPick.setOnClickListener{
            registerClicked()
        }

        binding.registerTOOLNavigation.setNavigationOnClickListener { view: View -> finish() }
    }
}