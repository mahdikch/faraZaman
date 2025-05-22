package net.osmtracker.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import net.osmtracker.R

class SubmitViolationFormActivity : AppCompatActivity() {
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

        var countEditText = findViewById<EditText>(R.id.count)
        var resultEditText = findViewById<EditText>(R.id.result)
        var saveButton = findViewById<MaterialButton>(R.id.save)

        saveButton.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("ذخیره")
                .setMessage("میخواهید اطلاعات تخلف را ذخیره کنید؟").setPositiveButton("تایید",
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
//        autoCompleteTextView.setOnFocusChangeListener { view, b -> if (b) autoCompleteTextView.showDropDown() }
//        autoCompleteTextView.setOnItemClickListener { adapterView, view, i, l ->
//            val selected = adapterView.getItemIdAtPosition(i).toString()
//            autoCompleteTextView.setText(selected)
//        }
    }
}
