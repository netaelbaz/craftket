package com.example.craftket

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
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
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.craftket.Models.ActivityType
import com.example.craftket.Models.Location
import com.example.craftket.utilites.Constants
import com.example.craftket.utilites.SignalManager
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class AddItemActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddItemBinding
    private val scheduleSlots = mutableListOf<TimeSlot>()
    private var pickedDate: Calendar? = null
    private var pickedStartTime: Calendar? = null
    private var pickedEndTime: Calendar? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var additionalPickImageLauncher: ActivityResultLauncher<String>
    private var mainImageUrl: String? = null
    private val storageRef = FirebaseStorage.getInstance().reference
    private lateinit var pickMultipleImagesLauncher: ActivityResultLauncher<String>
    private var additionalImageUrls: MutableList<String> = mutableListOf()

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

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                uploadSelectedImage(it)
            }
        }

        additionalPickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                imageViews[selectedImageIndex].setImageURI(it)
                imageViews[selectedImageIndex].setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                uploadImageToStorage(uri,
                    onSuccess = { url ->
                        additionalImageUrls.add(url)
                    },
                    onFailure = { e ->
                        Log.e("Add Activity", "Upload failed: ${e.message}")
                    }, additionalFlag = true)
            }
        }

        initView()
    }


    private fun initImageGallery() {
        imageViews = listOf(binding.addAdditionalImage1, binding.addAdditionalImage2,binding.addAdditionalImage3)

        imageViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedImageIndex = index
                additionalPickImageLauncher.launch("image/*")
            }
        }
    }


    private fun uploadSelectedImage(uri: Uri) {
        binding.addIMAGEAddMain.setImageURI(uri)
        binding.addIMAGEAddMain.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))

        uploadImageToStorage(uri,
            onSuccess = { url ->
                mainImageUrl = url
            },
            onFailure = { e ->
                Log.e("Add Activity", "Upload failed: ${e.message}")
            }
        )

    }

    private fun initView() {
        binding.addBTNSubmit.setOnClickListener { addItem() }
        setupTypeButtons(ActivityType.entries.map{ it.displayName }) // Replace with your actual types
        renderLevels()

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
        pickImageLauncher.launch("image/*")
    }

    private fun addItem() {
        val dbRef = FirebaseDatabase.getInstance().getReference("activities")
        val activityName = binding.addEDITName.text.toString().trim()
        if (activityName.isEmpty()) {
            SignalManager.getInstance().toast("Name is required")
            return
        }
        val activityPrice = binding.addEDITPrice.text.toString().toIntOrNull()
        if (activityPrice == null || activityPrice <= 0 || activityPrice > 10000) {
            SignalManager.getInstance().toast("Please enter a valid price")
            return

        }
        val activityLocation = parseLocationFromString(binding.addEDITAddress.text.toString())

        if (activityLocation == null) {
            SignalManager.getInstance().toast("Please enter a valid address")
            return
        }
        println("location ${activityLocation.getFullAddress()}")
        val activityFacebookUrl = binding.addEDITFacebook.text.toString()
        val activityInstagramUrl = binding.addEDITInstagram.text.toString()
        val activityCancelTime = binding.addEDITCancel.text.toString().toIntOrNull()
        if (activityCancelTime == null || activityCancelTime < 0) {
            SignalManager.getInstance().toast("Please enter a valid cancellation time")
            return
        }
        var selectedType: String? = null
        for (i in 0 until binding.addGROUPTypesContainer.childCount) {
            val view = binding.addGROUPTypesContainer.getChildAt(i)
            if (view is RadioButton && view.isChecked) {
                selectedType = view.text.toString()
            }
        }

        if (selectedType == null) {
            SignalManager.getInstance().toast("Type is required")
            return
        }

        val selectedLevels = arrayListOf<ActivityLevel>()
        for (i in 0 until binding.levelContainer.childCount) {
            val view = binding.levelContainer.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                val level = try {
                    ActivityLevel.valueOf(view.text.toString().uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
                level?.let { selectedLevels.add(it) }
            }
        }

        if (selectedLevels.isEmpty()) {
            SignalManager.getInstance().toast("Please select at least one level")
            return
        }

        if (mainImageUrl == null) {
            SignalManager.getInstance().toast("Main picture is required")
            return
        }

        if (scheduleSlots.isEmpty()) {
            SignalManager.getInstance().toast("Please add at least one time slot")
            return
        }

        if (additionalImageUrls.isEmpty()) {
            SignalManager.getInstance().toast("At least one additional picture required")
            return
        }

        val newActivity = Activity(
            name = activityName,
            imageUrl = mainImageUrl!!,
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
        dbRef.get().addOnSuccessListener { snapshot ->
            val nextIndex = snapshot.childrenCount.toInt()
            dbRef.child(nextIndex.toString()).setValue(newActivity)
                .addOnSuccessListener {
                    Log.d("Firebase", "Activity added at index $nextIndex")
                    SignalManager.getInstance().toast("Added Successfully")
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to add activity", e)
                }
        }
    }


    private fun fixNameFormat(name: String) {
        //
    }

    private fun setupTypeButtons(types: List<String>) {
        binding.addGROUPTypesContainer.removeAllViews()

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
            }
            binding.addGROUPTypesContainer.addView(btn)
        }

    }

    private fun renderLevels() {
        for (level in ActivityLevel.entries) {
            val checkBox = CheckBox(this).apply {
                text = level.name.lowercase().replaceFirstChar { it.uppercase() }
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
        startHour: Int,
        endHour: Int,
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
        if (pickedEndTime!!.before(pickedStartTime)) {
            SignalManager.getInstance().toast("Please choose correct end time")
            return
        }

        val repeatWeekly = binding.scheduleREPEATCheckbox.isChecked
        val repeatCount = if (repeatWeekly) {
            binding.scheduleREPEATCount.text.toString().toIntOrNull() ?: 0
        } else 0

        // Combine date with time to get exact timestamps (example)
        val startHour = pickedStartTime!!.get(Calendar.HOUR_OF_DAY)
        val endHour = pickedEndTime!!.get(Calendar.HOUR_OF_DAY)

        val capacity = binding.scheduleTXTCapacity.text.toString().toIntOrNull()
        if (capacity == null || capacity == 0) {
            SignalManager.getInstance().toast("Please choose capacity")
            return
        }
        if (repeatWeekly) {
            val slots = generateRepeatedTimeSlots(
                localStartDate,
                startHour,
                endHour,
                repeatCount,
                capacity
            )
            scheduleSlots.addAll(slots)
        } else {
            val slot = TimeSlot(
                date = formattedDate,
                startTime = startHour,
                endTime = endHour,
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


    private fun uploadImageToStorage(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit, additionalFlag: Boolean = false) {
        var fileName = "images/${System.currentTimeMillis()}.jpg"
        if (additionalFlag) {
            fileName = "images/additional/${System.currentTimeMillis()}.jpg"
        }

        val imageRef = storageRef.child(fileName)

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString()) // return the download URL
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }


}

