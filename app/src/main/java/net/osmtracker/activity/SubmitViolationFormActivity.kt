package net.osmtracker.activity

import android.Manifest
import android.R.attr.typeface
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import ir.hamsaa.persiandatepicker.PersianDatePickerDialog
import ir.hamsaa.persiandatepicker.api.PersianPickerDate
import ir.hamsaa.persiandatepicker.api.PersianPickerListener
import ir.hamsaa.persiandatepicker.util.PersianCalendarUtils
import net.osmtracker.R
import net.osmtracker.layout.URLValidatorTask.TAG
import saman.zamani.persiandate.PersianDate


class SubmitViolationFormActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var btnPickImage: Button
    private var cameraImageUri: Uri? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
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
        val employerOptions = resources.getStringArray(R.array.employer)

        val contractNumber1Options = resources.getStringArray(R.array.contract_number1)
        val contractNumber2Options = resources.getStringArray(R.array.contract_number2)
        val contractNumber3Options = resources.getStringArray(R.array.contract_number3)
        val contractNumber4Options = resources.getStringArray(R.array.contract_number4)
        val contractNumber5Options = resources.getStringArray(R.array.contract_number5)
        val violationGroupOptions = resources.getStringArray(R.array.violation_group)
        val defect1Options = resources.getStringArray(R.array.defect1)
        val defect2Options = resources.getStringArray(R.array.defect2)
        val defect3Options = resources.getStringArray(R.array.defect3)
        val defect4Options = resources.getStringArray(R.array.defect4)
        val defect5Options = resources.getStringArray(R.array.defect5)
        val seasonOfPrice1Options = resources.getStringArray(R.array.season_of_price1)
        val seasonOfPrice2Options = resources.getStringArray(R.array.season_of_price2)
        val seasonOfPrice3Options = resources.getStringArray(R.array.season_of_price3)
        val seasonOfPrice4Options = resources.getStringArray(R.array.season_of_price4)
        val seasonOfPrice5Options = resources.getStringArray(R.array.season_of_price5)
        val price_list1Options = resources.getStringArray(R.array.price_list1)
        val price_list3Options = resources.getStringArray(R.array.price_list3)

        val employerAdapter = ArrayAdapter(this, R.layout.dropdown_item, employerOptions)
        val employerTextView = findViewById<AutoCompleteTextView>(R.id.employer)
        employerTextView.setText(employerOptions[0], false)
        employerTextView.setAdapter(employerAdapter)

        var contractNumberAdapter =
            ArrayAdapter(this, R.layout.dropdown_item, contractNumber1Options)
        var contractNumberTextView = findViewById<AutoCompleteTextView>(R.id.contract_number)
        contractNumberTextView.setText(contractNumber1Options[0], false)
        contractNumberTextView.setAdapter(contractNumberAdapter)

        var violationGroupAdapter =
            ArrayAdapter(this, R.layout.dropdown_item, violationGroupOptions)
        var violationGroupTextView = findViewById<AutoCompleteTextView>(R.id.violation_group)
        violationGroupTextView.setText(violationGroupOptions[0], false)
        violationGroupTextView.setAdapter(violationGroupAdapter)

        var defectAdapter =
            ArrayAdapter(this, R.layout.dropdown_item, defect1Options)
        var defectTextView = findViewById<AutoCompleteTextView>(R.id.defect)
        defectTextView.setText(defect1Options[0], false)
        defectTextView.setAdapter(defectAdapter)

        var seasonOfPriceAdapter =
            ArrayAdapter(this, R.layout.dropdown_item, seasonOfPrice1Options)
        var seasonOfPriceTextView = findViewById<AutoCompleteTextView>(R.id.season_of_price)
        seasonOfPriceTextView.setText(seasonOfPrice1Options[0], false)
        seasonOfPriceTextView.setAdapter(seasonOfPriceAdapter)

        var price_listAdapter =
            ArrayAdapter(this, R.layout.dropdown_item, price_list1Options)
        var price_listTextView = findViewById<AutoCompleteTextView>(R.id.price_list)
        var price_listTextInputLayout = findViewById<TextInputLayout>(R.id.price_list_lay)
        price_listTextView.setText(price_list1Options[0], false)
        price_listTextView.setAdapter(price_listAdapter)

        val countEditText = findViewById<EditText>(R.id.count)
        val resultEditText = findViewById<EditText>(R.id.result)
        val dateEditText = findViewById<TextView>(R.id.Date_of_performance_registration)
        imageView = findViewById(R.id.violation_image)
        btnPickImage = findViewById<MaterialButton>(R.id.image_picker)
        btnPickImage.setOnClickListener {
            checkAndRequestPermissions()
        }
        val saveButton = findViewById<MaterialButton>(R.id.save)
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
                        Log.d(TAG, "onDateSelected: " + persianPickerDate.timestamp) //675930448000
                        Log.d(
                            TAG,
                            "onDateSelected: " + persianPickerDate.gregorianDate
                        ) //Mon Jun 03 10:57:28 GMT+04:30 1991
                        Log.d(
                            TAG,
                            "onDateSelected: " + persianPickerDate.persianLongDate
                        ) // دوشنبه  13  خرداد  1370
                        Log.d(TAG, "onDateSelected: " + persianPickerDate.persianMonthName) //خرداد
                        Log.d(
                            TAG,
                            "onDateSelected: " + PersianCalendarUtils.isPersianLeapYear(
                                persianPickerDate.persianYear
                            )
                        ) //true
                        dateEditText.setText(persianPickerDate.persianYear.toString() + "/" + persianPickerDate.persianMonth + "/" + persianPickerDate.persianDay)

                    }

                    override fun onDismissed() {
                    }
                }).show()
        }
        saveButton.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("ذخیره")
                .setMessage("میخواهید اطلاعات تخلف را ذخیره کنید؟").setPositiveButton(
                    "تایید",
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        startActivity(Intent(this, TrackManager::class.java))
                    }).setCancelable(true).show()

        }
        countEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                val input = p0.toString().toIntOrNull()
                val result: Int = (input?.times(1500))?.div(0.7)?.toInt() ?: 0
                resultEditText.setText(result.toString())
            }
        })

        employerTextView.setOnItemClickListener { adapterView, view, i, l ->
            when (i) {
                0 -> {
                    contractNumberAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, contractNumber1Options)
                    contractNumberTextView.setText(contractNumber1Options[0], false)
                    contractNumberTextView.setAdapter(contractNumberAdapter)


                }

                1 -> {
                    contractNumberAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, contractNumber2Options)
                    contractNumberTextView.setText(contractNumber2Options[0], false)
                    contractNumberTextView.setAdapter(contractNumberAdapter)


                }

                2 -> {
                    contractNumberAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, contractNumber3Options)
                    contractNumberTextView.setText(contractNumber3Options[0], false)
                    contractNumberTextView.setAdapter(contractNumberAdapter)


                }

                3 -> {
                    contractNumberAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, contractNumber4Options)
                    contractNumberTextView.setText(contractNumber4Options[0], false)
                    contractNumberTextView.setAdapter(contractNumberAdapter)


                }

                4 -> {
                    contractNumberAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, contractNumber5Options)
                    contractNumberTextView.setText(contractNumber5Options[0], false)
                    contractNumberTextView.setAdapter(contractNumberAdapter)


                }
            }
        }
        violationGroupTextView.setOnItemClickListener { adapterView, view, i, l ->
            when (i) {
                0 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect1Options)
                    defectTextView.setText(defect1Options[0], false)
                    defectTextView.setAdapter(defectAdapter)

                    seasonOfPriceAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, seasonOfPrice1Options)
                    seasonOfPriceTextView.setText(seasonOfPrice1Options[0], false)
                    seasonOfPriceTextView.setAdapter(seasonOfPriceAdapter)

                    price_listAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, price_list1Options)
                    price_listTextView.setText(price_list1Options[0], false)
                    price_listTextView.setAdapter(price_listAdapter)
                    price_listTextView.isEnabled = true

                }

                1 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect2Options)
                    defectTextView.setText(defect2Options[0], false)
                    defectTextView.setAdapter(defectAdapter)

                    seasonOfPriceAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, seasonOfPrice2Options)
                    seasonOfPriceTextView.setText(seasonOfPrice2Options[0], false)
                    seasonOfPriceTextView.setAdapter(seasonOfPriceAdapter)

                    price_listTextView.isEnabled = false
                    price_listTextView.setText("")
                }

                2 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect3Options)
                    defectTextView.setText(defect3Options[0], false)
                    defectTextView.setAdapter(defectAdapter)

                    seasonOfPriceAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, seasonOfPrice3Options)
                    seasonOfPriceTextView.setText(seasonOfPrice3Options[0], false)
                    seasonOfPriceTextView.setAdapter(seasonOfPriceAdapter)
                    price_listTextView.isEnabled = true

                    price_listAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, price_list3Options)
                    price_listTextView.setText(price_list3Options[0], false)
                    price_listTextView.setAdapter(price_listAdapter)
                }

                3 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect4Options)
                    defectTextView.setText(defect4Options[0], false)
                    defectTextView.setAdapter(defectAdapter)

                    seasonOfPriceAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, seasonOfPrice4Options)
                    seasonOfPriceTextView.setText(seasonOfPrice4Options[0], false)
                    seasonOfPriceTextView.setAdapter(seasonOfPriceAdapter)

                    price_listTextView.isEnabled = false
                    price_listTextView.setText("")
                }

                4 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect5Options)
                    defectTextView.setText(defect5Options[0], false)
                    defectTextView.setAdapter(defectAdapter)

                    seasonOfPriceAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, seasonOfPrice5Options)
                    seasonOfPriceTextView.setText(seasonOfPrice5Options[0], false)
                    seasonOfPriceTextView.setAdapter(seasonOfPriceAdapter)

                    price_listTextView.isEnabled = false
                    price_listTextView.setText("")
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val readGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES]
                ?: permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

            if (cameraGranted && readGranted) {
                showImagePickerDialog()
            } else {
                Toast.makeText(this, "دسترسی‌ها رد شدند", Toast.LENGTH_SHORT).show()
            }
        }
//        autoCompleteTextView.setOnFocusChangeListener { view, b -> if (b) autoCompleteTextView.showDropDown() }
//        autoCompleteTextView.setOnItemClickListener { adapterView, view, i, l ->
//            val selected = adapterView.getItemIdAtPosition(i).toString()
//            autoCompleteTextView.setText(selected)
//        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("انتخاب از گالری", "گرفتن عکس با دوربین")
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("انتخاب تصویر")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> pickImageFromGallery.launch("image/*")
                1 -> openCamera()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, "New Picture")
                put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            }
            cameraImageUri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            cameraImageUri?.let { takePictureLauncher.launch(it) }
        }
    }
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            showImagePickerDialog()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "اجازه استفاده از دوربین داده نشد", Toast.LENGTH_SHORT).show()
        }
    }
}
