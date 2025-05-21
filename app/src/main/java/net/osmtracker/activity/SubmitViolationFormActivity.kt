package net.osmtracker.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.osmtracker.R

class SubmitViolationFormActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_submit_violation_form)
        val dropdownOptions = resources.getStringArray(R.array.test)
        val dapter = ArrayAdapter(this, R.layout.dropdown_item, dropdownOptions)
        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.autoComplete)
        autoCompleteTextView.setAdapter(dapter)
//        autoCompleteTextView.setOnFocusChangeListener { view, b -> if (b) autoCompleteTextView.showDropDown() }
//        autoCompleteTextView.setOnItemClickListener { adapterView, view, i, l ->
//            val selected = adapterView.getItemIdAtPosition(i).toString()
//            autoCompleteTextView.setText(selected)
//        }
    }
}
