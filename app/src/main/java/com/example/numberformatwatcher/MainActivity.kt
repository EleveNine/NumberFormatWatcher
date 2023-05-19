package com.example.numberformatwatcher

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.numberformatwatcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    private val numberFormatWatcher = NumberFormatWatcher(
        textEnding = "$",
        isDecimal = true,
        minAmount = 12.0,
        maxAmount = 15000.0,
        isDecimalGroupingEnabled = true,
        fieldCanBeEmpty = false
    ) { numericValue ->
        binding.tvEnteredValue.text = """Entered numeric value: $numericValue"""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etInput.addTextChangedListener(numberFormatWatcher)
        binding.etInput.setRawInputType(Configuration.KEYBOARD_12KEY)
    }
}