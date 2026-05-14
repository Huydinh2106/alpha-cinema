package com.example.alphacinema.data.api

import android.util.Log

/**
 * Helper quản lý việc gửi mã OTP qua EmailJS và xác thực mã.
 *
 * Cách dùng:
 *   1. Gọi sendOtp(email) → gửi mail + lưu mã tạm
 *   2. Gọi verifyOtp(code) → so sánh mã người dùng nhập với mã đã lưu
 */
object EmailVerificationHelper {

    private const val TAG = "EmailVerification"

    // ── Thông số EmailJS (thay bằng của bạn) ────────────────────────────────
    private const val SERVICE_ID  = "service_mswbks4"
    private const val TEMPLATE_ID = "template_qp6s9oc"
    // TODO: Thay bằng Public Key của bạn từ EmailJS Dashboard → Account → API Keys
    private const val PUBLIC_KEY  = "Vs2XG4xrBAmiqFNpo"

    // ── Lưu mã OTP và thời gian hết hạn trong bộ nhớ ────────────────────────
    private var currentOtp: String? = null
    private var otpExpireTime: Long = 0L
    private const val OTP_VALIDITY_MS = 10 * 60 * 1000L  // 10 phút

    private val api: EmailJsService by lazy {
        RetrofitClient.emailJsApi
    }

    /**
     * Tạo mã OTP ngẫu nhiên 6 chữ số.
     */
    fun generateOtp(): String {
        return (100000..999999).random().toString()
    }

    /**
     * Gửi mã OTP đến email người dùng qua EmailJS.
     *
     * @param toEmail Email người nhận
     * @return true nếu gửi thành công, false nếu thất bại
     */
    suspend fun sendOtp(toEmail: String, serviceName: String = "Xác thực tài khoản"): Boolean {
        val otp = generateOtp()
        currentOtp = otp
        otpExpireTime = System.currentTimeMillis() + OTP_VALIDITY_MS

        val request = EmailJsRequest(
            service_id = SERVICE_ID,
            template_id = TEMPLATE_ID,
            user_id = PUBLIC_KEY,
            template_params = mapOf(
                "to_email" to toEmail,
                "otp_code" to otp,
                "service_name" to serviceName,
                "from_name" to "Alpha Cinema"
            )
        )

        return try {
            Log.d(TAG, "Đang gửi OTP đến $toEmail...")
            val response = api.sendEmail(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Gửi OTP thành công đến $toEmail")
                true
            } else {
                Log.e(TAG, "Gửi OTP thất bại: ${response.code()} - ${response.errorBody()?.string()}")
                // Reset OTP nếu gửi thất bại
                currentOtp = null
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi OTP: ${e.message}", e)
            currentOtp = null
            false
        }
    }

    /**
     * Xác thực mã OTP mà người dùng nhập vào.
     *
     * @param inputCode Mã 6 số người dùng nhập
     * @return kết quả xác thực
     */
    fun verifyOtp(inputCode: String): OtpVerifyResult {
        if (currentOtp == null) {
            return OtpVerifyResult.NO_OTP_SENT
        }
        if (System.currentTimeMillis() > otpExpireTime) {
            currentOtp = null
            return OtpVerifyResult.EXPIRED
        }
        return if (inputCode == currentOtp) {
            currentOtp = null  // Dùng xong thì xóa
            OtpVerifyResult.SUCCESS
        } else {
            OtpVerifyResult.WRONG_CODE
        }
    }

    /**
     * Xóa mã OTP hiện tại (khi người dùng hủy flow).
     */
    fun clearOtp() {
        currentOtp = null
        otpExpireTime = 0L
    }
}

/**
 * Kết quả xác thực OTP.
 */
enum class OtpVerifyResult {
    SUCCESS,       // Mã đúng
    WRONG_CODE,    // Mã sai
    EXPIRED,       // Mã hết hạn
    NO_OTP_SENT    // Chưa gửi mã nào
}
