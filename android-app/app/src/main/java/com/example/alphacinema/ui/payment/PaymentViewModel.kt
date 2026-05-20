package com.example.alphacinema.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.MomoAppPaymentRequest
import com.example.alphacinema.data.model.MomoPaymentRequest
import com.example.alphacinema.data.model.MomoPaymentResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Payment ViewModel - calls Cloud Function to create MoMo payment.
 * No secret keys stored in the app.
 */
class PaymentViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var orderListener: ListenerRegistration? = null

    private val _paymentResponse = MutableStateFlow<MomoPaymentResponse?>(null)
    val paymentResponse = _paymentResponse.asStateFlow()

    private val _paymentConfirmed = MutableStateFlow(false)
    val paymentConfirmed = _paymentConfirmed.asStateFlow()

    private val _paymentStatusMessage = MutableStateFlow<String?>(null)
    val paymentStatusMessage = _paymentStatusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun createMomoPayment(amount: Long, orderInfo: String, plan: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _paymentResponse.value = null
            _paymentConfirmed.value = false
            _paymentStatusMessage.value = null
            orderListener?.remove()
            orderListener = null
            try {
                val user = auth.currentUser
                if (user == null) {
                    _error.value = "Vui lòng đăng nhập trước khi thanh toán"
                    return@launch
                }

                val idToken = user.getIdToken(false).await().token
                if (idToken.isNullOrBlank()) {
                    _error.value = "Không lấy được phiên đăng nhập"
                    return@launch
                }

                val request = MomoPaymentRequest(
                    amount = amount,
                    orderInfo = orderInfo,
                    plan = plan
                )

                val response = RetrofitClient.momoApi.createPayment("Bearer $idToken", request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.resultCode == 0) {
                        _paymentResponse.value = body
                        observePaymentOrder(body.orderId)
                    } else {
                        val errorMessage = body?.message ?: body?.error ?: "Lỗi từ MoMo"
                        _error.value = "$errorMessage (Mã lỗi: ${body?.resultCode})"
                    }
                } else {
                    val errorDetail = response.errorBody()?.string() ?: "Unknown error"
                    _error.value = "Lỗi hệ thống: $errorDetail"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processMomoAppPayment(
        orderId: String,
        amount: Long,
        orderInfo: String,
        plan: String,
        token: String,
        phoneNumber: String,
        env: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _paymentResponse.value = null
            _paymentConfirmed.value = false
            _paymentStatusMessage.value = "MoMo đã xác nhận, đang hoàn tất giao dịch"
            orderListener?.remove()
            orderListener = null
            try {
                val user = auth.currentUser
                if (user == null) {
                    _error.value = "Vui lòng đăng nhập trước khi thanh toán"
                    return@launch
                }

                val idToken = user.getIdToken(false).await().token
                if (idToken.isNullOrBlank()) {
                    _error.value = "Không lấy được phiên đăng nhập"
                    return@launch
                }

                val request = MomoAppPaymentRequest(
                    orderId = orderId,
                    amount = amount,
                    orderInfo = orderInfo,
                    plan = plan,
                    token = token,
                    phoneNumber = phoneNumber,
                    env = env
                )

                val response = RetrofitClient.momoApi.processAppPayment("Bearer $idToken", request)
                if (response.isSuccessful) {
                    val body = response.body()
                    val isSuccess = body?.status == 0 || body?.resultCode == 0
                    if (body != null && isSuccess) {
                        _paymentStatusMessage.value = "Đang đồng bộ trạng thái giao dịch"
                        observePaymentOrder(body.orderId)
                    } else {
                        val errorMessage = body?.message ?: body?.error ?: "MoMo xử lý giao dịch không thành công"
                        _error.value = body?.status?.let { "$errorMessage (Mã lỗi: $it)" } ?: errorMessage
                    }
                } else {
                    val errorDetail = response.errorBody()?.string() ?: "Unknown error"
                    _error.value = "Lỗi hệ thống: $errorDetail"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observePaymentOrder(orderId: String?) {
        if (orderId.isNullOrBlank()) {
            _paymentStatusMessage.value = "Đang chờ MoMo xác nhận giao dịch"
            return
        }

        _paymentStatusMessage.value = "Đang chờ MoMo xác nhận giao dịch"
        orderListener?.remove()
        orderListener = firestore.collection("momoOrders")
            .document(orderId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    _paymentStatusMessage.value = "Đang chờ hệ thống đồng bộ trạng thái"
                    return@addSnapshotListener
                }

                when (snapshot?.getString("status")) {
                    "paid" -> {
                        _paymentStatusMessage.value = "MoMo đã xác nhận thanh toán"
                        _paymentConfirmed.value = true
                        orderListener?.remove()
                        orderListener = null
                    }
                    "failed" -> {
                        _error.value = snapshot.getString("message") ?: "Thanh toán không thành công"
                        orderListener?.remove()
                        orderListener = null
                    }
                    else -> {
                        _paymentStatusMessage.value = "Đang chờ MoMo xác nhận giao dịch"
                    }
                }
            }
    }

    fun clearPaymentResponse() {
        orderListener?.remove()
        orderListener = null
        _paymentResponse.value = null
        _paymentConfirmed.value = false
        _paymentStatusMessage.value = null
        _error.value = null
    }

    override fun onCleared() {
        orderListener?.remove()
        super.onCleared()
    }
}
