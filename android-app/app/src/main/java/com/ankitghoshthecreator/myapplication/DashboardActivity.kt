package com.ankitghoshthecreator.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvHolderName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvAccountNumber: TextView
    private lateinit var tvVpa: TextView
    private lateinit var btnScanToPay: Button

    private var customerId: String? = null
    private var accountId: String? = null
    private var accountNumber: String? = null
    private var holderName: String? = null
    private var userVpa: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvHolderName = findViewById(R.id.tvHolderName)
        tvBalance = findViewById(R.id.tvBalance)
        tvAccountNumber = findViewById(R.id.tvAccountNumber)
        tvVpa = findViewById(R.id.tvVpa)
        btnScanToPay = findViewById(R.id.btnScanToPay)

        // Read saved session
        val prefs = getSharedPreferences("BangluPayPrefs", Context.MODE_PRIVATE)
        customerId = prefs.getString("customerId", null)
        accountId = prefs.getString("accountId", null)
        accountNumber = prefs.getString("accountNumber", "")
        holderName = prefs.getString("holderName", "")

        if (customerId == null || accountId == null) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Set initial local storage data
        tvHolderName.text = holderName
        tvAccountNumber.text = "A/C: $accountNumber"

        // Load dynamic data from backend
        fetchDashboardData()

        btnScanToPay.setOnClickListener {
            showPaymentDialog()
        }
    }

    private fun fetchDashboardData() {
        lifecycleScope.launch {
            try {
                val api = NetworkClient.apiService
                val accId = accountId ?: return@launch

                // 1. Fetch account to get latest balance
                val accResponse = withContext(Dispatchers.IO) { api.getAccount(accId) }
                if (accResponse.isSuccessful && accResponse.body() != null) {
                    val account = accResponse.body()!!
                    tvBalance.text = String.format("₹ %.2f", account.balance)
                }

                // 2. Fetch UPI profile to get VPA
                val cId = customerId ?: return@launch
                val upiResponse = withContext(Dispatchers.IO) { api.getUPIProfiles(cId) }
                if (upiResponse.isSuccessful && !upiResponse.body().isNullOrEmpty()) {
                    val firstProfile = upiResponse.body()!!.first()
                    userVpa = firstProfile.vpa
                    tvVpa.text = userVpa
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPaymentDialog() {
        val context = this
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Send Money via UPI")

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val etReceiverVpa = EditText(context).apply {
            hint = "Receiver VPA (e.g. recipient@bngl)"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        val etAmount = EditText(context).apply {
            hint = "Amount (₹)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        val etPin = EditText(context).apply {
            hint = "UPI PIN (4-6 Digits)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        // Add a helper button to fill demo receiver VPA
        val btnDemoPayee = Button(context).apply {
            text = "Use Demo Receiver (bob@bngl)"
            setOnClickListener {
                etReceiverVpa.setText("bob@bngl")
                etAmount.setText("250.00")
                etPin.setText("5678") // default bob pin
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        layout.addView(etReceiverVpa)
        layout.addView(etAmount)
        layout.addView(etPin)
        layout.addView(btnDemoPayee)

        builder.setView(layout)

        builder.setPositiveButton("Pay Now") { dialog, _ ->
            val receiverVpa = etReceiverVpa.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val pin = etPin.text.toString().trim()

            if (receiverVpa.isEmpty() || amountStr.isEmpty() || pin.isEmpty()) {
                Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Call payment API
            lifecycleScope.launch {
                try {
                    Toast.makeText(context, "Initiating payment...", Toast.LENGTH_SHORT).show()

                    val payBody = mapOf(
                        "senderVpa" to userVpa,
                        "receiverVpa" to receiverVpa,
                        "amount" to amountStr,
                        "pin" to pin,
                        "idempotencyKey" to UUID.randomUUID().toString(),
                        "description" to "Transfer from Android client"
                    )

                    val api = NetworkClient.apiService
                    val response = withContext(Dispatchers.IO) { api.initiateUPIPayment(payBody) }

                    if (response.isSuccessful && response.body() != null) {
                        val payment = response.body()!!
                        if (payment.status == "SUCCESS") {
                            Toast.makeText(
                                context,
                                "Payment Successful! Ref: ${payment.referenceNumber}",
                                Toast.LENGTH_LONG
                            ).show()
                            fetchDashboardData() // refresh balance
                        } else {
                            Toast.makeText(
                                context,
                                "Payment Failed: ${payment.failureReason ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val error = response.errorBody()?.string() ?: "Connection issue"
                        throw Exception(error)
                    }

                } catch (e: Exception) {
                    Toast.makeText(context, "Payment failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}
