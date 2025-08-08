package com.example.craftket

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.craftket.Models.Activity
import com.example.craftket.Models.ActivityLevel
import com.example.craftket.Models.TimeSlot
import com.example.craftket.databinding.ActivityAddItemBinding
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.craftket.Models.ActivityType
import com.example.craftket.Models.Location
import com.example.craftket.utilites.Constants
import com.example.craftket.utilites.SignalManager
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.lifecycle.lifecycleScope
import com.example.craftket.utilites.Utils.signOut
import kotlinx.coroutines.launch



class AddItemActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddItemBinding
    private val scheduleSlots = mutableListOf<TimeSlot>()
    private var pickedDate: Calendar? = null
    private var pickedStartTime: Calendar? = null
    private var pickedEndTime: Calendar? = null
    private lateinit var mainImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var additionalImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private var selectedMainImageUri: Uri? = null
    private val additionalImageUris = mutableListOf<Uri>()
    private lateinit var imageViews: List<ImageView>
    private var selectedImageIndex = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        mainImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let {
                binding.addIMAGEAddMain.setImageURI(it)
                binding.addIMAGEAddMain.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                selectedMainImageUri = it
            } ?: Log.d("PhotoPicker", "No media selected")
        }


        additionalImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let {
                imageViews[selectedImageIndex].setImageURI(it)
                imageViews[selectedImageIndex].setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                additionalImageUris.add(it)
            }
        }

        initView()
    }


    private fun initImageGallery() {
        imageViews = listOf(binding.addAdditionalImage1, binding.addAdditionalImage2,binding.addAdditionalImage3)

        imageViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedImageIndex = index
                additionalImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }


    private fun initView() {
        binding.addBTNSubmit.setOnClickListener { addItem() }
        setupTypeButtons(ActivityType.entries.map{ it.displayName }) // Replace with your actual types
        renderLevels()

        binding.addBTNSignOut.setOnClickListener {  signOut(this) }

        binding.scheduleREPEATCheckbox.setOnCheckedChangeListener { _, checked ->
            binding.scheduleREPEATCount.visibility = if (checked) View.VISIBLE else View.GONE
        }
        binding.scheduleTXTDate.setOnClickListener {
            showDatePicker()
        }

        binding.scheduleTXTStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        binding.scheduleTXTEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        binding.scheduleBTNAdd.setOnClickListener {
            addScheduleSlot()
        }

        binding.addIMAGEAddMain.setOnClickListener {
            pickImageFromGallery()
        }

        binding.scheduleBTNMinus.setOnClickListener {
            setCapacity(-1)
        }

        binding.scheduleBTNPlus.setOnClickListener {
            setCapacity(1)
        }

        binding.addToolbar.setNavigationOnClickListener { view: View -> finish() }

        initImageGallery()
    }


    private fun setCapacity(offset: Int) {
        val currentValue = binding.scheduleTXTCapacity.text.toString().toIntOrNull() ?: 0
        val newValue = (currentValue + offset).coerceAtLeast(0)
        binding.scheduleTXTCapacity.text = newValue.toString()
    }

    private fun parseLocationFromString(address: String?): Location? {
        if (address == null) return null

        val parts = address.split(",").map { it.trim() }
        if (parts.size < 3) return null  // Invalid format

        val street = parts[0]
        val number = parts[1].toIntOrNull() ?: return null
        val city = parts.subList(2, parts.size).joinToString(", ") // Re-join the rest as city

        return Location(city = city, street = street, number = number)
    }

    private fun pickImageFromGallery() {
        mainImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun addItem() {
        val activityName = binding.addEDITName.text.toString().takeIf { it.isNotEmpty() } ?: return SignalManager.getInstance().toast("Name is required")
        val activityPrice = binding.addEDITPrice.text.toString().toIntOrNull()
            ?.takeIf { it in 1..10000 }
            ?: return SignalManager.getInstance().toast("Please enter a valid price")

        val activityLocation = parseLocationFromString(binding.addEDITAddress.text.toString())
            ?: return SignalManager.getInstance().toast("Please enter a valid address")

        val activityFacebookUrl = binding.addEDITFacebook.text.toString()
        val activityInstagramUrl = binding.addEDITInstagram.text.toString()

        val activityCancelTime = binding.addEDITCancel.text.toString().toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: return SignalManager.getInstance().toast("Please enter a valid cancellation time")

        val selectedType = (0 until binding.addGROUPTypesContainer.childCount)
            .mapNotNull { binding.addGROUPTypesContainer.getChildAt(it) as? RadioButton }
            .firstOrNull { it.isChecked }
            ?.text.toString()
            .takeIf { it.isNotEmpty() }
            ?: return SignalManager.getInstance().toast("Type is required")


        val selectedLevels = (0 until binding.levelContainer.childCount)
            .mapNotNull { binding.levelContainer.getChildAt(it) as? CheckBox }
            .filter { it.isChecked }
            .mapNotNull {
                try {
                    ActivityLevel.valueOf(it.text.toString().uppercase())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        if (selectedLevels.isEmpty()) return SignalManager.getInstance().toast("Please select at least one level")
        if (scheduleSlots.isEmpty()) return SignalManager.getInstance().toast("Please add at least one time slot")
        if (additionalImageUris.isEmpty()) return SignalManager.getInstance().toast("At least one additional picture required")

        val mainImageUri = selectedMainImageUri ?: return SignalManager.getInstance().toast("Main picture is required")

        binding.addProgressbar.visibility = View.VISIBLE
        binding.addBTNSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                val imageUrl = uploadImageToStorage(mainImageUri)

                val additionalImageUrls = additionalImageUris.map { uploadImageToStorage(it, additionalFlag = true) }

                val newActivity = Activity(
                    name = activityName,
                    imageUrl = imageUrl,
                    field = ActivityType.valueOf(selectedType.uppercase()),
                    price = activityPrice,
                    facebookUrl = activityFacebookUrl,
                    instagramUrl = activityInstagramUrl,
                    location = activityLocation,
                    levels = selectedLevels,
                    cancelTime = activityCancelTime,
                    schedule = scheduleSlots,
                    additionalImages = additionalImageUrls
                )
                saveToDataBase(newActivity)
            } catch (e: Exception) {
                Log.e("Add Activity", "Upload or save failed: ${e.message}")
                SignalManager.getInstance().toast("Something went wrong: ${e.message}")
            }
            finally {
                binding.addProgressbar.visibility = View.GONE
                binding.addBTNSubmit.isEnabled = true
            }
        }
    }

    private fun saveToDataBase(newActivity: Activity) {
        val dbRef = FirebaseDatabase.getInstance().getReference("activities")

        dbRef.get().addOnSuccessListener { snapshot ->

            val nameExists = snapshot.children.any { child ->
                val name = child.child("name").getValue(String::class.java)
                name == newActivity.name
            }

            if (nameExists) {
                SignalManager.getInstance().toast("This activity name already exists")
                Log.e("Firebase", "Activity name already exists")
                return@addOnSuccessListener

            }
            val newKey = dbRef.push().key
            if (newKey == null) {
                Log.e("Firebase", "Failed to generate unique key")
                SignalManager.getInstance().toast("Failed to Save activity to database")
                return@addOnSuccessListener
            }
            dbRef.child(newKey).setValue(newActivity)
                .addOnSuccessListener {
                    Log.d("Firebase", "Activity added at key id $newKey")
                    SignalManager.getInstance().toast("Added Successfully")
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to add activity", e)
                }

        }.addOnFailureListener {
            Log.e("Firebase", "Failed to read from database", it)
            SignalManager.getInstance().toast("Database error")
        }
    }

    private fun setupTypeButtons(types: List<String>) {
        binding.addGROUPTypesContainer.removeAllViews()
        val addedButtons = mutableListOf<RadioButton>()

        types.forEach { type ->
            val btn = RadioButton(this).apply {
                text = type
                isAllCaps = false
                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = 8
                }

                // manually disable multi selection because using flexbox and not radio group
                setOnClickListener {
                    addedButtons.forEach { it.isChecked = false }
                    isChecked = true
                }
            }
            addedButtons.add(btn)
            binding.addGROUPTypesContainer.addView(btn)
        }

    }

    private fun renderLevels() {
        for (level in ActivityLevel.entries) {
            val checkBox = CheckBox(this).apply {
                text = level.name.lowercase().replaceFirstChar { it.uppercase() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            binding.levelContainer.addView(checkBox)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()


    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                pickedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val sdf = SimpleDateFormat(Constants.Format.DATE_FORMATTER, Locale.getDefault())
                binding.scheduleTXTDate.text = sdf.format(pickedDate!!.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val cal = Calendar.getInstance()
        val dialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val timeCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                if (isStartTime) {
                    pickedStartTime = timeCal
                    binding.scheduleTXTStartTime.text = sdf.format(timeCal.time)
                } else {
                    pickedEndTime = timeCal
                    binding.scheduleTXTEndTime.text = sdf.format(timeCal.time)
                }
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        )
        dialog.show()
    }


    fun generateRepeatedTimeSlots(
        startDate: LocalDate,
        startHour: Float,
        endHour: Float,
        repeatCount: Int,
        capacity: Int
    ): List<TimeSlot> {

        val formatter = DateTimeFormatter.ofPattern(Constants.Format.DATE_FORMATTER)
        val slots = mutableListOf<TimeSlot>()

        for (i in 0..repeatCount) {
            val currentDate = startDate.plusWeeks(i.toLong())
            slots.add(
                TimeSlot(
                    date = currentDate.format(formatter),
                    startTime = startHour,
                    endTime = endHour,
                    capacity = capacity,
                    registeredUsers = emptyList()
                )
            )
        }

        return slots
    }

    private fun addScheduleSlot() {
        if (pickedDate == null) {
            SignalManager.getInstance().toast("Please choose a date")
            return
        }
        // format picked date to correct format
        val formatter = DateTimeFormatter.ofPattern(Constants.Format.DATE_FORMATTER)
        val pickedDateTimestamp = pickedDate!!.time
        val localStartDate = pickedDateTimestamp.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val formattedDate = localStartDate.format(formatter)

        if (pickedStartTime == null || pickedEndTime == null) {
            SignalManager.getInstance().toast("Please choose start and end time")
            return
        }
        val diffMillis = pickedEndTime!!.timeInMillis - pickedStartTime!!.timeInMillis
        if (diffMillis < 60_000) { // 60000 ms - one minute
            SignalManager.getInstance().toast("Please choose correct end time")
            return
        }

        val repeatWeekly = binding.scheduleREPEATCheckbox.isChecked
        val repeatCount = if (repeatWeekly) {
            binding.scheduleREPEATCount.text.toString().toIntOrNull() ?: 0
        } else 0

        val startHour = pickedStartTime!!.get(Calendar.HOUR_OF_DAY)
        val startMinutes = pickedStartTime!!.get(Calendar.MINUTE)
        val startTime = (startHour + startMinutes / 60.0).toFloat()
        val endHour = pickedEndTime!!.get(Calendar.HOUR_OF_DAY)
        val endMinutes = pickedEndTime!!.get(Calendar.MINUTE)
        val endTime = (endHour + endMinutes / 60.0).toFloat()

        val capacity = binding.scheduleTXTCapacity.text.toString().toIntOrNull()
        if (capacity == null || capacity == 0) {
            SignalManager.getInstance().toast("Please choose capacity")
            return
        }
        if (repeatWeekly) {
            val slots = generateRepeatedTimeSlots(
                localStartDate,
                startTime,
                endTime,
                repeatCount,
                capacity
            )
            scheduleSlots.addAll(slots)
        } else {
            val slot = TimeSlot(
                date = formattedDate,
                startTime = startTime,
                endTime = endTime,
                capacity = capacity,
                registeredUsers = emptyList()
            )
            scheduleSlots.add(slot)
        }

        SignalManager.getInstance().toast("Time slot added successfully")
        resetUi()
    }

    private fun resetUi() {
        binding.scheduleTXTDate.text = getString(R.string.pick_start_date)
        binding.scheduleTXTStartTime.text = getString(R.string.start_time)
        binding.scheduleTXTEndTime.text = getString(R.string.end_time)
        pickedDate = null
        pickedStartTime = null
        pickedEndTime = null
        binding.scheduleREPEATCheckbox.isChecked = false
        binding.scheduleREPEATCount.setText("")
        binding.scheduleTXTCapacity.text = getString(R.string.capacity_default)
    }

    suspend fun uploadImageToStorage(uri: Uri, additionalFlag: Boolean = false): String {
        return suspendCoroutine { continuation ->
            var fileName = "images/${System.currentTimeMillis()}.jpg"
            if (additionalFlag) {
                fileName = "images/additional/${System.currentTimeMillis()}.jpg"
            }
            val ref = FirebaseStorage.getInstance().getReference(fileName)
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        continuation.resume(downloadUri.toString())
                    }.addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
}