package com.ankitghoshthecreator.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)
        val etMobileNumber = findViewById<EditText>(R.id.etMobileNumber)

        btnLogin.setOnClickListener {
            val mobile = etMobileNumber.text.toString().trim()
            if (mobile.isEmpty()) {
                Toast.makeText(this, "Please enter mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    btnLogin.isEnabled = false
                    btnLogin.text = "Logging in..."

                    val api = NetworkClient.apiService
                    val loginBody = mapOf(
                        "username" to mobile,
                        "password" to "Password@123" // matching default password from registration
                    )

                    val response = withContext(Dispatchers.IO) { api.login(loginBody) }
                    if (!response.isSuccessful || response.body() == null) {
                        val error = response.errorBody()?.string() ?: "Invalid mobile number"
                        throw Exception(error)
                    }

                    val auth = response.body()!!
                    val customerId = auth.customerId

                    // Save customer info
                    val prefs = getSharedPreferences("BangluPayPrefs", Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString("customerId", customerId)
                    editor.putString("mobile", mobile)

                    // Fetch customer account
                    val accountsResponse = withContext(Dispatchers.IO) { api.getCustomerAccounts(customerId) }
                    if (accountsResponse.isSuccessful && !accountsResponse.body().isNullOrEmpty()) {
                        val firstAccount = accountsResponse.body()!!.first()
                        editor.putString("accountId", firstAccount.accountId)
                        editor.putString("accountNumber", firstAccount.accountNumber)
                        editor.putString("holderName", firstAccount.holderName)
                    } else {
                        throw Exception("No active bank account found. Please register again.")
                    }

                    editor.apply()

                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                }
            }
        }

        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
