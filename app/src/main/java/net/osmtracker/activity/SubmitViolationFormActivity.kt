package net.osmtracker.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import net.osmtracker.R

class SubmitViolationFormActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_submit_violation_form)
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
        val seasonOfPriceOptions = resources.getStringArray(R.array.season_of_price)

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
                }

                1 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect2Options)
                    defectTextView.setText(defect2Options[0], false)
                    defectTextView.setAdapter(defectAdapter)
                }

                2 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect3Options)
                    defectTextView.setText(defect3Options[0], false)
                    defectTextView.setAdapter(defectAdapter)
                }

                3 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect4Options)
                    defectTextView.setText(defect4Options[0], false)
                    defectTextView.setAdapter(defectAdapter)
                }

                4 -> {
                    defectAdapter =
                        ArrayAdapter(this, R.layout.dropdown_item, defect5Options)
                    defectTextView.setText(defect5Options[0], false)
                    defectTextView.setAdapter(defectAdapter)
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
