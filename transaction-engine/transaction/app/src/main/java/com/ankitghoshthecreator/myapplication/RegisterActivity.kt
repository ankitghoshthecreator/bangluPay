package com.ankitghoshthecreator.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnSubmitRegister = findViewById<Button>(R.id.btnSubmitRegister)

        btnSubmitRegister.setOnClickListener {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
            finish() // go back to login
        }
    }
}
