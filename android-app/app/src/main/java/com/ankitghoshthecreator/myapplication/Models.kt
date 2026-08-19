package com.ankitghoshthecreator.myapplication

import java.io.Serializable

data class Customer(
    val customerId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val status: String
) : Serializable

data class UserAuth(
    val authId: String,
    val customerId: String,
    val username: String,
    val role: String,
    val locked: Boolean
) : Serializable

data class Account(
    val accountId: String,
    val accountNumber: String,
    val holderName: String,
    val balance: Double,
    val currency: String,
    val status: String,
    val accountType: String,
    val bankId: String,
    val branchId: String
) : Serializable

data class UPIProfile(
    val upiId: String,
    val customerId: String,
    val vpa: String,
    val linkedAccountId: String,
    val status: String
) : Serializable

data class Payment(
    val paymentId: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Double,
    val currency: String,
    val paymentRail: String,
    val status: String,
    val description: String,
    val referenceNumber: String,
    val senderVpa: String?,
    val receiverVpa: String?,
    val transactionId: String?,
    val failureReason: String?,
    val createdAt: String?
) : Serializable

data class KYCRecord(
    val kycId: String,
    val customerId: String,
    val status: String // PENDING, VERIFIED, FAILED
) : Serializable
