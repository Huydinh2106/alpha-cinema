package com.example.alphacinema.payment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import vn.momo.momo_partner.AppMoMoLib
import vn.momo.momo_partner.MoMoParameterNamePayment
import java.util.UUID

data class MomoSdkPaymentRequest(
    val amount: Long,
    val orderInfo: String,
    val plan: String
)

data class MomoSdkTokenResult(
    val orderId: String,
    val token: String,
    val phoneNumber: String,
    val env: String,
    val message: String
)

object MomoSdkCoordinator {
    private const val MERCHANT_NAME = "Alpha Cinema"
    private const val MERCHANT_CODE = "MOMOIQA420180417"
    private const val MERCHANT_NAME_LABEL = "Dịch vụ"
    private const val ORDER_LABEL = "Mã đơn hàng"
    private const val TEST_MOMO_PACKAGE = "vn.momo.platform.test"

    private var callback: ((Result<MomoSdkTokenResult>) -> Unit)? = null
    private var pendingOrderId: String? = null

    fun requestPayment(
        activity: Activity,
        request: MomoSdkPaymentRequest,
        onResult: (Result<MomoSdkTokenResult>) -> Unit
    ) {
        if (!isPackageInstalled(activity, TEST_MOMO_PACKAGE)) {
            onResult(Result.failure(IllegalStateException("Chưa cài MoMo test app trên thiết bị")))
            return
        }

        callback = onResult

        val orderId = "$MERCHANT_CODE-${System.currentTimeMillis()}-${UUID.randomUUID()}"
        pendingOrderId = orderId
        val extraData = JSONObject().apply {
            put("plan", request.plan)
            put("source", "alpha_cinema_android")
        }

        val eventValue = hashMapOf<String, Any>(
            MoMoParameterNamePayment.MERCHANT_NAME to MERCHANT_NAME,
            MoMoParameterNamePayment.MERCHANT_CODE to MERCHANT_CODE,
            MoMoParameterNamePayment.AMOUNT to request.amount.toString(),
            MoMoParameterNamePayment.DESCRIPTION to request.orderInfo,
            MoMoParameterNamePayment.FEE to "0",
            MoMoParameterNamePayment.MERCHANT_NAME_LABEL to MERCHANT_NAME_LABEL,
            MoMoParameterNamePayment.REQUEST_ID to orderId,
            MoMoParameterNamePayment.PARTNER_CODE to MERCHANT_CODE,
            MoMoParameterNamePayment.EXTRA_DATA to extraData.toString(),
            MoMoParameterNamePayment.REQUEST_TYPE to "payment",
            MoMoParameterNamePayment.LANGUAGE to "vi",
            MoMoParameterNamePayment.EXTRA to "",
            "orderId" to orderId,
            "orderLabel" to ORDER_LABEL
        )

        AppMoMoLib.getInstance().setEnvironment(AppMoMoLib.ENVIRONMENT.DEVELOPMENT)
        AppMoMoLib.getInstance().setAction(AppMoMoLib.ACTION.PAYMENT)
        AppMoMoLib.getInstance().setActionType(AppMoMoLib.ACTION_TYPE.GET_TOKEN)
        AppMoMoLib.getInstance().requestMoMoCallBack(activity, eventValue)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != AppMoMoLib.getInstance().REQUEST_CODE_MOMO) {
            return false
        }

        val pendingCallback = callback
        val orderId = pendingOrderId
        callback = null
        pendingOrderId = null
        if (pendingCallback == null) {
            return true
        }

        if (resultCode != Activity.RESULT_OK || data == null || orderId.isNullOrBlank()) {
            pendingCallback(Result.failure(IllegalStateException("Không nhận được phản hồi từ MoMo")))
            return true
        }

        val status = data.getIntExtra("status", -1)
        val message = data.getStringExtra("message").orEmpty().ifBlank { "Thanh toán MoMo không thành công" }
        if (status == 0) {
            val token = data.getStringExtra("data").orEmpty()
            if (token.isBlank()) {
                pendingCallback(Result.failure(IllegalStateException("MoMo không trả token thanh toán")))
                return true
            }

            pendingCallback(
                Result.success(
                    MomoSdkTokenResult(
                        orderId = orderId,
                        token = token,
                        phoneNumber = data.getStringExtra("phonenumber").orEmpty(),
                        env = data.getStringExtra("env") ?: "app",
                        message = message
                    )
                )
            )
        } else {
            val normalizedMessage = when (status) {
                5 -> "Giao dịch MoMo đã hết thời gian xác nhận"
                6 -> "Bạn đã hủy giao dịch MoMo"
                else -> message
            }
            pendingCallback(Result.failure(IllegalStateException(normalizedMessage)))
        }

        return true
    }

    @Suppress("DEPRECATION")
    private fun isPackageInstalled(activity: Activity, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
                )
            } else {
                activity.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
