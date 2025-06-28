package net.osmtracker.activity

import android.Manifest
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import ir.hamsaa.persiandatepicker.PersianDatePickerDialog
import ir.hamsaa.persiandatepicker.api.PersianPickerDate
import ir.hamsaa.persiandatepicker.api.PersianPickerListener
import ir.hamsaa.persiandatepicker.util.PersianCalendarUtils
import net.osmtracker.R
import net.osmtracker.data.model.DropdownItem
import net.osmtracker.layout.URLValidatorTask.TAG
import net.osmtracker.presentation.viewmodel.FormDataViewModel
import saman.zamani.persiandate.PersianDate
import javax.inject.Inject

@AndroidEntryPoint
class SubmitViolationFormActivity : AppCompatActivity() {
    
    private val formDataViewModel: FormDataViewModel by viewModels()
    
    private lateinit var imageView: ImageView
    private lateinit var btnPickImage: Button
    private var cameraImageUri: Uri? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    // UI Components
    private lateinit var organsTextView: AutoCompleteTextView
    private lateinit var contractNumberTextView: AutoCompleteTextView
    private lateinit var violationGroupTextView: AutoCompleteTextView
    private lateinit var defectTextView: AutoCompleteTextView
    private lateinit var seasonOfPriceTextView: AutoCompleteTextView
    private lateinit var price_listTextView: AutoCompleteTextView
    
    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(it)
            }
        }
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(cameraImageUri)
            } else {
                Toast.makeText(this, "خطا در گرفتن تصویر", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_submit_violation_form)

        val myToolbar = findViewById<Toolbar>(R.id.my_toolbar)
        title = "اطلاعات تخلف"
        setSupportActionBar(myToolbar)
        
        initializeViews()
        setupObservers()
        fetchFormData()
        setupImagePicker()
        setupDatePicker()
        setupSaveButton()
        setupCalculationListener()
    }
    
    private fun initializeViews() {
        organsTextView = findViewById(R.id.organs)
        contractNumberTextView = findViewById(R.id.contracts)
        violationGroupTextView = findViewById(R.id.billCleaningViolationGroups)
        defectTextView = findViewById(R.id.billCleaningViolations)
        seasonOfPriceTextView = findViewById(R.id.billCleaningItemGroups)
        price_listTextView = findViewById(R.id.billOriginCleaningItems)
        imageView = findViewById(R.id.violation_image)
        btnPickImage = findViewById(R.id.image_picker)
    }
    
    private fun setupObservers() {
        formDataViewModel.formData.observe(this) { formData ->
            updateDropdownsWithPreservedSelections(formData)
        }
        
        formDataViewModel.isLoading.observe(this) { isLoading ->
            // You can show/hide a loading indicator here
            if (isLoading) {
                Toast.makeText(this, "در حال بارگذاری اطلاعات...", Toast.LENGTH_SHORT).show()
            }
        }
        
        formDataViewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error fetching form data: $error")
            }
        }
    }
    
    private fun updateDropdownsWithPreservedSelections(formData: net.osmtracker.data.model.FormDataResponse) {
        // Store current selections
        val currentOrganText = organsTextView.text.toString()
        val currentContractText = contractNumberTextView.text.toString()
        val currentViolationGroupText = violationGroupTextView.text.toString()
        val currentViolationText = defectTextView.text.toString()
        val currentItemGroupText = seasonOfPriceTextView.text.toString()
        val currentOriginItemText = price_listTextView.text.toString()
        
        // Update organs dropdown
        val organs = formData.response.organs
        val organsAdapter = ArrayAdapter(this, R.layout.dropdown_item, organs.map { it.text })
        organsTextView.setAdapter(organsAdapter)
        if (organs.isNotEmpty()) {
            // Try to preserve current selection, otherwise use first item
            val organToSelect = if (currentOrganText.isNotEmpty() && organs.any { it.text == currentOrganText }) {
                currentOrganText
            } else {
                organs[0].text
            }
            organsTextView.setText(organToSelect, false)
        }
        
        // Update contracts dropdown
        val contracts = formData.response.contracts
        val contractsAdapter = ArrayAdapter(this, R.layout.dropdown_item, contracts.map { it.text })
        contractNumberTextView.setAdapter(contractsAdapter)
        if (contracts.isNotEmpty()) {
            val contractToSelect = if (currentContractText.isNotEmpty() && contracts.any { it.text == currentContractText }) {
                currentContractText
            } else {
                contracts[0].text
            }
            contractNumberTextView.setText(contractToSelect, false)
        }
        
        // Update violation groups dropdown
        val violationGroups = formData.response.billCleaningViolationGroups
        val violationGroupsAdapter = ArrayAdapter(this, R.layout.dropdown_item, violationGroups.map { it.text })
        violationGroupTextView.setAdapter(violationGroupsAdapter)
        if (violationGroups.isNotEmpty()) {
            val violationGroupToSelect = if (currentViolationGroupText.isNotEmpty() && violationGroups.any { it.text == currentViolationGroupText }) {
                currentViolationGroupText
            } else {
                violationGroups[0].text
            }
            violationGroupTextView.setText(violationGroupToSelect, false)
        }
        
        // Update violations dropdown
        val violations = formData.response.billCleaningViolations
        val violationsAdapter = ArrayAdapter(this, R.layout.dropdown_item, violations.map { it.text })
        defectTextView.setAdapter(violationsAdapter)
        if (violations.isNotEmpty()) {
            val violationToSelect = if (currentViolationText.isNotEmpty() && violations.any { it.text == currentViolationText }) {
                currentViolationText
            } else {
                violations[0].text
            }
            defectTextView.setText(violationToSelect, false)
        }
        
        // Update season of price dropdown
        val itemGroups = formData.response.billCleaningItemGroups
        val itemGroupsAdapter = ArrayAdapter(this, R.layout.dropdown_item, itemGroups.map { it.text })
        seasonOfPriceTextView.setAdapter(itemGroupsAdapter)
        if (itemGroups.isNotEmpty()) {
            val itemGroupToSelect = if (currentItemGroupText.isNotEmpty() && itemGroups.any { it.text == currentItemGroupText }) {
                currentItemGroupText
            } else {
                itemGroups[0].text
            }
            seasonOfPriceTextView.setText(itemGroupToSelect, false)
        }
        
        // Update price list dropdown
        val originItems = formData.response.billOriginCleaningItems
        val originItemsAdapter = ArrayAdapter(this, R.layout.dropdown_item, originItems.map { it.text })
        price_listTextView.setAdapter(originItemsAdapter)
        if (originItems.isNotEmpty()) {
            val originItemToSelect = if (currentOriginItemText.isNotEmpty() && originItems.any { it.text == currentOriginItemText }) {
                currentOriginItemText
            } else {
                originItems[0].text
            }
            price_listTextView.setText(originItemToSelect, false)
        }
        
        // Set up dropdown listeners for dynamic updates
        setupDropdownListeners(formData)
    }
    
    private fun setupDropdownListeners(formData: net.osmtracker.data.model.FormDataResponse) {
        organsTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedOrgan = formData.response.organs[position]
            Log.d(TAG, "Selected organ: ${selectedOrgan.text} (${selectedOrgan.value})")
            // Update form data with selected organ
            formDataViewModel.updateOrganSelection(selectedOrgan.value)
        }
        
        contractNumberTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedContract = formData.response.contracts[position]
            Log.d(TAG, "Selected contract: ${selectedContract.text} (${selectedContract.value})")
            // Update form data with selected contract
            formDataViewModel.updateContractSelection(selectedContract.value)
        }
        
        violationGroupTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedViolationGroup = formData.response.billCleaningViolationGroups[position]
            Log.d(TAG, "Selected violation group: ${selectedViolationGroup.text} (${selectedViolationGroup.value})")
            // Update form data with selected violation group
            formDataViewModel.updateViolationGroupSelection(selectedViolationGroup.value)
        }
        
        defectTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedViolation = formData.response.billCleaningViolations[position]
            Log.d(TAG, "Selected violation: ${selectedViolation.text} (${selectedViolation.value})")
            // Update form data with selected violation
            formDataViewModel.updateViolationSelection(selectedViolation.value)
        }
        
        seasonOfPriceTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedItemGroup = formData.response.billCleaningItemGroups[position]
            Log.d(TAG, "Selected item group: ${selectedItemGroup.text} (${selectedItemGroup.value})")
            // Update form data with selected item group
            formDataViewModel.updateItemGroupSelection(selectedItemGroup.value)
        }
    }
    
    private fun fetchFormData() {
        formDataViewModel.fetchFormData()
    }
    
    private fun populateDropdowns(formData: net.osmtracker.data.model.FormDataResponse) {
        // Initial population of dropdowns (called only once)
        updateDropdownsWithPreservedSelections(formData)
    }
    
    private fun setupImagePicker() {
        btnPickImage.setOnClickListener {
            checkAndRequestPermissions()
        }
        
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val readGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES]
                ?: permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

            // Check if we have at least camera permission (for camera) or read permission (for gallery)
            if (cameraGranted || readGranted) {
                // Show dialog immediately after permission is granted
                showImagePickerDialog()
            } else {
                Toast.makeText(this, "دسترسی‌ها رد شدند", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupDatePicker() {
        val dateEditText = findViewById<TextView>(R.id.Date_of_performance_registration)
        dateEditText.text =
            PersianDate().shYear.toString() + "/" + PersianDate().shMonth.toString() + "/" + PersianDate().shDay.toString()
        dateEditText.setOnClickListener {
            PersianDatePickerDialog(this)
                .setPositiveButtonString("باشه")
                .setNegativeButton("بیخیال")
                .setTodayButton("امروز")
                .setTodayButtonVisible(true)
                .setMinYear(1300)
                .setMaxYear(PersianDatePickerDialog.THIS_YEAR)
                .setMaxMonth(PersianDatePickerDialog.THIS_MONTH)
                .setMaxDay(PersianDatePickerDialog.THIS_DAY)
                .setInitDate(1370, 3, 13)
                .setActionTextColor(Color.GRAY)
                .setTitleType(PersianDatePickerDialog.WEEKDAY_DAY_MONTH_YEAR)
                .setShowInBottomSheet(true)
                .setListener(object : PersianPickerListener {
                    override fun onDateSelected(persianPickerDate: PersianPickerDate) {
                        Log.d(TAG, "onDateSelected: " + persianPickerDate.timestamp)
                        Log.d(TAG, "onDateSelected: " + persianPickerDate.gregorianDate)
                        Log.d(TAG, "onDateSelected: " + persianPickerDate.persianLongDate)
                        Log.d(TAG, "onDateSelected: " + persianPickerDate.persianMonthName)
                        Log.d(TAG, "onDateSelected: " + PersianCalendarUtils.isPersianLeapYear(persianPickerDate.persianYear))
                        dateEditText.setText(persianPickerDate.persianYear.toString() + "/" + persianPickerDate.persianMonth + "/" + persianPickerDate.persianDay)
                    }

                    override fun onDismissed() {
                    }
                }).show()
        }
    }
    
    private fun setupSaveButton() {
        val saveButton = findViewById<MaterialButton>(R.id.save)
        saveButton.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("ذخیره")
                .setMessage("میخواهید اطلاعات تخلف را ذخیره کنید؟").setPositiveButton(
                    "تایید",
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        startActivity(Intent(this, TrackManager::class.java))
                    }).setCancelable(true).show()
        }
    }
    
    private fun setupCalculationListener() {
        val countEditText = findViewById<EditText>(R.id.count)
        val resultEditText = findViewById<EditText>(R.id.result)
        
        countEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val input = p0.toString().toIntOrNull()
                val result: Int = (input?.times(1500))?.div(0.7)?.toInt() ?: 0
                resultEditText.setText(result.toString())
            }
        })
    }

    private fun showImagePickerDialog() {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()
        
        // Check if we have read permission for gallery
        val hasReadPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        // Check if we have camera permission
        val hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        
        if (hasReadPermission) {
            options.add("انتخاب از گالری")
            actions.add { pickImageFromGallery.launch("image/*") }
        }
        
        if (hasCameraPermission) {
            options.add("گرفتن عکس با دوربین")
            actions.add { openCamera() }
        }
        
        if (options.isEmpty()) {
            Toast.makeText(this, "هیچ دسترسی‌ای برای انتخاب تصویر موجود نیست", Toast.LENGTH_SHORT).show()
            return
        }
        
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("انتخاب تصویر")
        builder.setItems(options.toTypedArray()) { _, which ->
            actions[which]()
        }
        builder.show()
    }

    private fun openCamera() {
        // Camera permission is already checked in checkAndRequestPermissions
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        cameraImageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        cameraImageUri?.let { takePictureLauncher.launch(it) }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // Always request camera permission for camera functionality
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // Request read permission for gallery functionality
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Only request write permission for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            // Permissions already granted, show dialog immediately
            showImagePickerDialog()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // This method is deprecated and not needed since we're using ActivityResultLauncher
        // The permission handling is now done in the permissionLauncher callback
    }
}
