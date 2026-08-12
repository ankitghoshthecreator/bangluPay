package com.ankitghoshthecreator.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val btnScanToPay = findViewById<Button>(R.id.btnScanToPay)

        btnScanToPay.setOnClickListener {
            Toast.makeText(this, "Opening QR Scanner...", Toast.LENGTH_SHORT).show()
            // Placeholder for real scanner
        }
    }
}
