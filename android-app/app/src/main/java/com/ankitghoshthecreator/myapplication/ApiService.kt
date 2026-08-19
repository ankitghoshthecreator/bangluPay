package com.ankitghoshthecreator.myapplication

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Customer Endpoints ---
    @POST("api/customers/register")
    suspend fun register(@Body body: Map<String, String>): Response<Customer>

    @POST("api/customers/login")
    suspend fun login(@Body body: Map<String, String>): Response<UserAuth>

    @GET("api/customers/{id}")
    suspend fun getCustomer(@Path("id") customerId: String): Response<Customer>

    @GET("api/customers/{id}/accounts")
    suspend fun getCustomerAccounts(@Path("id") customerId: String): Response<List<Account>>

    // --- KYC Endpoints ---
    @POST("api/kyc/{customerId}/initiate")
    suspend fun initiateKYC(@Path("customerId") customerId: String): Response<KYCRecord>

    @POST("api/kyc/{customerId}/aadhaar")
    suspend fun verifyAadhaar(
        @Path("customerId") customerId: String,
        @Body body: Map<String, String>
    ): Response<KYCRecord>

    @POST("api/kyc/{customerId}/pan")
    suspend fun verifyPAN(
        @Path("customerId") customerId: String,
        @Body body: Map<String, String>
    ): Response<KYCRecord>

    @GET("api/kyc/{customerId}")
    suspend fun getKYCStatus(@Path("customerId") customerId: String): Response<KYCRecord>

    // --- Account Endpoints ---
    @POST("api/accounts/full")
    suspend fun createAccount(@Body body: Map<String, String>): Response<Account>

    @POST("api/accounts/{id}/deposit")
    suspend fun deposit(
        @Path("id") accountId: String,
        @Query("amount") amount: Double
    ): Response<Account>

    @GET("api/accounts/{id}")
    suspend fun getAccount(@Path("id") accountId: String): Response<Account>

    // --- UPI Endpoints ---
    @POST("api/upi/register")
    suspend fun registerVPA(@Body body: Map<String, String>): Response<UPIProfile>

    @GET("api/upi/lookup/{vpa}")
    suspend fun lookupVPA(@Path("vpa") vpa: String): Response<UPIProfile>

    @GET("api/upi/profile/{customerId}")
    suspend fun getUPIProfiles(@Path("customerId") customerId: String): Response<List<UPIProfile>>

    // --- Payment Endpoints ---
    @POST("api/payments/upi")
    suspend fun initiateUPIPayment(@Body body: Map<String, String>): Response<Payment>

    @GET("api/payments/account/{accountId}")
    suspend fun getPaymentHistory(@Path("accountId") accountId: String): Response<List<Payment>>
}
