package com.ankitghoshthecreator.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etRegName = findViewById<EditText>(R.id.etRegName)
        val etRegMobile = findViewById<EditText>(R.id.etRegMobile)
        val btnSubmitRegister = findViewById<Button>(R.id.btnSubmitRegister)

        btnSubmitRegister.setOnClickListener {
            val name = etRegName.text.toString().trim()
            val mobile = etRegMobile.text.toString().trim()

            if (name.isEmpty() || mobile.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform backend registration and full onboarding sequence
            lifecycleScope.launch {
                try {
                    btnSubmitRegister.isEnabled = false
                    btnSubmitRegister.text = "Creating profile..."
                    Toast.makeText(this@RegisterActivity, "Starting registration...", Toast.LENGTH_SHORT).show()

                    val api = NetworkClient.apiService

                    // 1. Register customer on backend
                    val regBody = mapOf(
                        "fullName" to name,
                        "email" to "$mobile@bngl.com",
                        "phone" to mobile,
                        "address" to "Simulated Offline Home",
                        "password" to "Password@123" // dummy password for simplified login
                    )

                    val regResponse = withContext(Dispatchers.IO) { api.register(regBody) }
                    if (!regResponse.isSuccessful || regResponse.body() == null) {
                        val error = regResponse.errorBody()?.string() ?: "Registration failed"
                        throw Exception(error)
                    }

                    val customer = regResponse.body()!!
                    val customerId = customer.customerId

                    // 2. Initiate KYC
                    btnSubmitRegister.text = "Initiating KYC..."
                    val initKycResponse = withContext(Dispatchers.IO) { api.initiateKYC(customerId) }
                    if (!initKycResponse.isSuccessful) {
                        throw Exception("Failed to initiate KYC")
                    }

                    // 3. Verify Aadhaar (Simulated)
                    btnSubmitRegister.text = "Verifying Aadhaar..."
                    val aadhaarBody = mapOf("aadhaar" to "123412341234")
                    val aadhaarResponse = withContext(Dispatchers.IO) { api.verifyAadhaar(customerId, aadhaarBody) }
                    if (!aadhaarResponse.isSuccessful) {
                        throw Exception("Failed to verify Aadhaar")
                    }

                    // 4. Verify PAN (Simulated)
                    btnSubmitRegister.text = "Verifying PAN..."
                    val panBody = mapOf("pan" to "ABCDE1234F")
                    val panResponse = withContext(Dispatchers.IO) { api.verifyPAN(customerId, panBody) }
                    if (!panResponse.isSuccessful) {
                        throw Exception("Failed to verify PAN")
                    }

                    // 5. Create Savings Account
                    btnSubmitRegister.text = "Opening account..."
                    val accBody = mapOf(
                        "customerId" to customerId,
                        "holderName" to name,
                        "type" to "SAVINGS",
                        "bankId" to "00000002-0000-0000-0000-000000000001", // BNGL ID
                        "branchId" to "00000003-0000-0000-0000-000000000001" // BNGL Main Branch ID
                    )
                    val accResponse = withContext(Dispatchers.IO) { api.createAccount(accBody) }
                    if (!accResponse.isSuccessful || accResponse.body() == null) {
                        throw Exception("Failed to open bank account")
                    }
                    val account = accResponse.body()!!

                    // 6. Seed initial deposit (₹10,000 faucet cash)
                    btnSubmitRegister.text = "Seeding balance..."
                    val depResponse = withContext(Dispatchers.IO) { api.deposit(account.accountId, 10000.00) }
                    if (!depResponse.isSuccessful) {
                        throw Exception("Failed to seed initial balance")
                    }

                    // 7. Register UPI ID (vpa: mobile@bngl, pin: 1234)
                    btnSubmitRegister.text = "Registering UPI..."
                    val upiBody = mapOf(
                        "customerId" to customerId,
                        "accountId" to account.accountId,
                        "vpa" to "$mobile@bngl",
                        "pin" to "1234"
                    )
                    val upiResponse = withContext(Dispatchers.IO) { api.registerVPA(upiBody) }
                    if (!upiResponse.isSuccessful) {
                        throw Exception("Failed to register UPI ID")
                    }

                    Toast.makeText(
                        this@RegisterActivity,
                        "Onboarding complete! Mobile VPA: $mobile@bngl (PIN: 1234)",
                        Toast.LENGTH_LONG
                    ).show()

                    finish() // go back to login screen

                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSubmitRegister.isEnabled = true
                    btnSubmitRegister.text = "Register"
                }
            }
        }
    }
}
