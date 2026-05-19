package com.example.alphacinema.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.MomoPaymentRequest
import com.example.alphacinema.data.model.MomoPaymentResponse
import com.example.alphacinema.util.HashUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel : ViewModel() {
    private val _paymentResponse = MutableStateFlow<MomoPaymentResponse?>(null)
    val paymentResponse = _paymentResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val partnerCode = "MOMO3EZU20260519"
    private val accessKey = "5hEIlys5O7oLzOwb"
    private val secretKey = "3qanEvKIEZOJsfW3avjA8FV2vpdTJZ0I"
    private val storeId = "E7ly67eJcWJ3l7Kd"

    fun createMomoPayment(amount: Long, orderInfo: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val requestId = System.currentTimeMillis().toString()
                val orderId = System.currentTimeMillis().toString()
                
                // Cấu hình URL trỏ về Firebase Project của bạn
                val redirectUrl = "https://alpha-cinema-39dfb.web.app/payment-success"
                val ipnUrl = "https://asia-southeast1-alpha-cinema-39dfb.cloudfunctions.net/momoIpn"
                
                val requestType = "captureWallet"
                val extraData = "" 

                val rawSignature = "accessKey=$accessKey" +
                        "&amount=$amount" +
                        "&extraData=$extraData" +
                        "&ipnUrl=$ipnUrl" +
                        "&orderId=$orderId" +
                        "&orderInfo=$orderInfo" +
                        "&partnerCode=$partnerCode" +
                        "&redirectUrl=$redirectUrl" +
                        "&requestId=$requestId" +
                        "&requestType=$requestType"

                val signature = HashUtils.hmacSha256(rawSignature, secretKey)

                val request = MomoPaymentRequest(
                    partnerCode = partnerCode,
                    partnerName = "ALPHA CINEMA",
                    storeId = storeId,
                    requestId = requestId,
                    amount = amount,
                    orderId = orderId,
                    orderInfo = orderInfo,
                    redirectUrl = redirectUrl,
                    ipnUrl = ipnUrl,
                    extraData = extraData,
                    requestType = requestType,
                    signature = signature
                )

                val response = RetrofitClient.momoApi.createPayment(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.resultCode == 0) {
                        _paymentResponse.value = body
                    } else {
                        val errorMessage = body?.message ?: "Lỗi từ MoMo"
                        _error.value = "$errorMessage (Mã lỗi: ${body?.resultCode})"
                    }
                } else {
                    val errorDetail = response.errorBody()?.string() ?: "Unknown error"
                    _error.value = "Lỗi hệ thống MoMo: $errorDetail"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPaymentResponse() {
        _paymentResponse.value = null
        _error.value = null
    }
}
