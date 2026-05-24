package com.example.alphacinema.ui.payment

import android.app.Activity
import android.content.ContentValues
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alphacinema.data.model.MomoPaymentResponse
import com.example.alphacinema.data.model.availableUpgradePlans
import com.example.alphacinema.payment.MomoSdkCoordinator
import com.example.alphacinema.payment.MomoSdkPaymentRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.EnumMap
import java.util.Locale

private val ScreenBackground = Color.Black
private val SurfaceDark = Color(0xFF141414)
private val SurfaceMuted = Color(0xFF1F1F1F)
private val AccentGold = Color(0xFFF6E29A)
private val AccentTeal = Color(0xFF63E6D8)
private val ErrorRed = Color(0xFFFF7A7A)
private val MomoPrimary = Color(0xFFA50064)
private val MomoSoft = Color(0xFFFCEAF4)

private enum class PaymentPackageId {
    BASIC,
    COUPLE,
    PREMIUM
}

private fun PaymentPackageId.selectionColor(): Color {
    return when (this) {
        PaymentPackageId.BASIC -> Color(0xFFD8DEE9)
        PaymentPackageId.COUPLE -> Color(0xFFFF8AB8)
        PaymentPackageId.PREMIUM -> Color(0xFFF6E29A)
    }
}

private fun PaymentPackageId.planId(): String = name.lowercase(Locale.ROOT)

private data class PaymentPackageUi(
    val id: PaymentPackageId,
    val name: String,
    val price: String,
    val amount: Long,
    val duration: String,
    val description: String,
    val features: List<String>,
    val buttonText: String,
    val badge: String? = null
)

private enum class PaymentMethodId {
    WALLET,
    BANK_CARD,
    QR_CODE
}

private data class PaymentMethodUi(
    val id: PaymentMethodId,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private enum class PaymentStatus {
    IDLE,
    CONFIRMING,
    PROCESSING,
    QR_DISPLAYED,
    SUCCESS,
    FAILED
}

@Composable
fun PaymentScreen(
    onBack: () -> Unit = {},
    onPaymentConfirmed: () -> Unit = {},
    currentPlan: String? = null,
    viewModel: PaymentViewModel = viewModel()
) {
    val context = LocalContext.current
    val allPackages = rememberPaymentPackages()
    val upgradePlanIds = remember(currentPlan) {
        availableUpgradePlans(currentPlan).map { it.id }.toSet()
    }
    val packages = remember(allPackages, upgradePlanIds) {
        allPackages.filter { it.id.planId() in upgradePlanIds }
    }
    val paymentMethods = rememberPaymentMethods()
    var selectedPackage by remember { mutableStateOf<PaymentPackageUi?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethodUi?>(null) }
    var currentPackage by remember { mutableStateOf<PaymentPackageUi?>(null) }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.IDLE) }
    var sdkError by remember { mutableStateOf<String?>(null) }
    val isSelectionComplete = selectedPackage != null && selectedPaymentMethod != null

    val momoResponse by viewModel.paymentResponse.collectAsState()
    val paymentConfirmed by viewModel.paymentConfirmed.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(momoResponse) {
        if (momoResponse != null) {
            paymentStatus = PaymentStatus.QR_DISPLAYED
        }
    }

    LaunchedEffect(packages) {
        if (selectedPackage != null && packages.none { it.id == selectedPackage?.id }) {
            selectedPackage = null
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            sdkError = null
            paymentStatus = PaymentStatus.FAILED
        }
    }

    LaunchedEffect(paymentConfirmed) {
        if (paymentConfirmed) {
            currentPackage = selectedPackage
            paymentStatus = PaymentStatus.SUCCESS
            onPaymentConfirmed()
            viewModel.clearPaymentResponse()
        }
    }

    if (paymentStatus == PaymentStatus.QR_DISPLAYED && momoResponse != null) {
        MomoQrPayment(
            response = momoResponse!!,
            onCancel = {
                paymentStatus = PaymentStatus.IDLE
                viewModel.clearPaymentResponse()
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        PaymentBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PaymentTopBar(
                onBack = {
                    if (paymentStatus == PaymentStatus.CONFIRMING || paymentStatus == PaymentStatus.QR_DISPLAYED) {
                        paymentStatus = PaymentStatus.IDLE
                        viewModel.clearPaymentResponse()
                    } else {
                        onBack()
                    }
                }
            )

            when (paymentStatus) {
                PaymentStatus.IDLE,
                PaymentStatus.SUCCESS,
                PaymentStatus.FAILED -> {
                    PaymentHero()

                    if (packages.isEmpty()) {
                        NoUpgradePlansNotice()
                    } else {
                        PricingSection(
                            packages = packages,
                            selectedPackageId = selectedPackage?.id,
                            onSelectPackage = {
                                selectedPackage = it
                                if (paymentStatus != PaymentStatus.IDLE) {
                                    paymentStatus = PaymentStatus.IDLE
                                }
                            }
                        )

                        PaymentMethodSection(
                            methods = paymentMethods,
                            selectedMethodId = selectedPaymentMethod?.id,
                            onSelectMethod = {
                                selectedPaymentMethod = it
                                if (paymentStatus != PaymentStatus.IDLE) {
                                    paymentStatus = PaymentStatus.IDLE
                                }
                            }
                        )

                        PaymentPolicyNotice()

                        Button(
                            onClick = { paymentStatus = PaymentStatus.CONFIRMING },
                            enabled = isSelectionComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color(0xFF060914),
                                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                disabledContentColor = Color.White.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = "Tiếp tục thanh toán",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                PaymentStatus.CONFIRMING -> {
                    val packageUi = selectedPackage
                    val method = selectedPaymentMethod
                    if (packageUi != null && method != null) {
                        OrderSummary(
                            selectedPackage = packageUi,
                            selectedPaymentMethod = method,
                            onConfirmPayment = {
                                if (method.id == PaymentMethodId.WALLET) {
                                    val orderInfo = "Thanh toán gói ${packageUi.name} Alpha Cinema"
                                    val plan = packageUi.name.lowercase()
                                    val activity = context.findActivity()
                                    paymentStatus = PaymentStatus.PROCESSING
                                    sdkError = null
                                    if (activity == null) {
                                        sdkError = "Không mở được màn hình thanh toán MoMo"
                                        paymentStatus = PaymentStatus.FAILED
                                    } else {
                                        MomoSdkCoordinator.requestPayment(
                                            activity = activity,
                                            request = MomoSdkPaymentRequest(
                                                amount = packageUi.amount,
                                                orderInfo = orderInfo,
                                                plan = plan
                                            )
                                        ) { result ->
                                            result.fold(
                                                onSuccess = { tokenResult ->
                                                    viewModel.processMomoAppPayment(
                                                        orderId = tokenResult.orderId,
                                                        amount = packageUi.amount,
                                                        orderInfo = orderInfo,
                                                        plan = plan,
                                                        token = tokenResult.token,
                                                        phoneNumber = tokenResult.phoneNumber,
                                                        env = tokenResult.env
                                                    )
                                                },
                                                onFailure = { throwable ->
                                                    sdkError = throwable.message ?: "Thanh toán MoMo không thành công"
                                                    paymentStatus = PaymentStatus.FAILED
                                                }
                                            )
                                        }
                                    }
                                } else if (method.id == PaymentMethodId.QR_CODE) {
                                    val orderInfo = "Thanh toán gói ${packageUi.name} Alpha Cinema"
                                    val plan = packageUi.name.lowercase()
                                    paymentStatus = PaymentStatus.PROCESSING
                                    sdkError = null
                                    viewModel.createMomoPayment(packageUi.amount, orderInfo, plan)
                                } else {
                                    paymentStatus = PaymentStatus.PROCESSING
                                }
                            },
                            onChangeSelection = { paymentStatus = PaymentStatus.IDLE }
                        )
                    }
                }

                PaymentStatus.PROCESSING -> {
                    ProcessingPaymentState()
                }

                PaymentStatus.QR_DISPLAYED -> {
                    ProcessingPaymentState()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (paymentStatus == PaymentStatus.SUCCESS && currentPackage != null) {
            PaymentSuccessModal(
                packageName = currentPackage?.name.orEmpty(),
                onDismiss = { paymentStatus = PaymentStatus.IDLE }
            )
        }

        if (paymentStatus == PaymentStatus.FAILED) {
            PaymentFailedModal(
                errorMessage = sdkError ?: error ?: "Thanh toán thất bại. Vui lòng thử lại.",
                onRetry = { paymentStatus = PaymentStatus.CONFIRMING },
                onDismiss = { 
                    paymentStatus = PaymentStatus.IDLE 
                    sdkError = null
                    viewModel.clearPaymentResponse()
                }
            )
        }
    }
}

@Composable
private fun MomoQrPayment(
    response: MomoPaymentResponse,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qrPayload = remember(response.qrCodeUrl, response.payUrl, response.deeplink) {
        response.qrCodeUrl?.takeIf { it.isNotBlank() }
            ?: response.payUrl?.takeIf { it.isNotBlank() }
            ?: response.deeplink?.takeIf { it.isNotBlank() }
    }
    val qrBitmap = remember(qrPayload) {
        qrPayload?.let { generateQrBitmap(it) }
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MomoPrimary)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MomoLogoMark()
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thanh toán MoMo",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Quét QR hoặc mở ví MoMo để hoàn tất giao dịch",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.13f))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Số tiền",
                        color = Color.White.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = formatVnd(response.amount),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Dùng app MoMo để quét mã",
                    color = Color(0xFF19111A),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(286.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE9DCE5), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Mã QR thanh toán MoMo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = MomoPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "Không có dữ liệu QR",
                                color = Color(0xFF3D2A37),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                response.orderId?.takeIf { it.isNotBlank() }?.let { orderId ->
                    Text(
                        text = "Mã giao dịch: $orderId",
                        color = Color(0xFF5A4A55),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MomoSoft)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MomoInstructionRow("1", "Mở ứng dụng MoMo trên điện thoại")
                    MomoInstructionRow("2", "Chọn Quét mã và đưa camera vào mã QR")
                    MomoInstructionRow("3", "Kiểm tra số tiền rồi xác nhận thanh toán")
                }

                Button(
                    onClick = { openMomoPayment(context, response) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MomoPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Mở ứng dụng MoMo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F1F5))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MomoPrimary,
                        trackColor = MomoPrimary.copy(alpha = 0.12f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            qrBitmap?.let { bitmap ->
                                scope.launch {
                                    saveImageToGallery(context, bitmap)
                                }
                            }
                        },
                        enabled = qrBitmap != null,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MomoPrimary.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FileDownload,
                            contentDescription = null,
                            tint = MomoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Lưu QR",
                            color = MomoPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5D7E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Hủy",
                            color = Color(0xFF45313D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MomoLogoMark() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MoMo",
            color = MomoPrimary,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MomoInstructionRow(index: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MomoPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = text,
            color = Color(0xFF3D2A37),
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun generateQrBitmap(content: String, size: Int = 1024): Bitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        put(EncodeHintType.MARGIN, 1)
    }
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    val black = Color.Black.toArgb()
    val white = Color.White.toArgb()

    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) black else white
        }
    }

    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

private fun formatVnd(amount: Long?): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    return "${formatter.format(amount ?: 0L)}đ"
}

private fun openMomoPayment(context: Context, response: MomoPaymentResponse) {
    val primaryUri = response.deeplink?.takeIf { it.isNotBlank() }
        ?: response.payUrl?.takeIf { it.isNotBlank() }
        ?: response.qrCodeUrl?.takeIf { it.isNotBlank() }

    if (primaryUri == null) {
        Toast.makeText(context, "Không có liên kết thanh toán MoMo", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(primaryUri)))
    } catch (e: ActivityNotFoundException) {
        val fallbackUrl = response.payUrl?.takeIf { it.isNotBlank() && it != primaryUri }
        if (fallbackUrl != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
        } else {
            Toast.makeText(context, "Không mở được ứng dụng MoMo", Toast.LENGTH_SHORT).show()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private suspend fun saveImageToGallery(context: Context, bitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            val filename = "MomoQR_${System.currentTimeMillis()}.png"
            var fos: java.io.OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                imagesDir.mkdirs()
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Đã lưu mã QR vào thư viện", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun NoUpgradePlansNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WorkspacePremium,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Bạn đang dùng gói cao nhất",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Gói Premium đã bao gồm toàn bộ quyền lợi hiện có.",
            color = Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun rememberPaymentPackages(): List<PaymentPackageUi> {
    return remember {
        listOf(
            PaymentPackageUi(
                id = PaymentPackageId.BASIC,
                name = "Basic",
                price = "29.000đ / tháng",
                amount = 29000,
                duration = "1 tháng",
                description = "Dành cho người dùng cá nhân",
                features = listOf(
                    "Không quảng cáo",
                    "Chế độ trẻ em",
                    "Lưu video vào playlist"
                ),
                buttonText = "Chọn gói Basic"
            ),
            PaymentPackageUi(
                id = PaymentPackageId.COUPLE,
                name = "Couple",
                price = "59.000đ / tháng",
                amount = 59000,
                duration = "1 tháng",
                description = "Dành cho 2 người xem chung",
                features = listOf(
                    "Không quảng cáo",
                    "Chế độ trẻ em",
                    "Lưu video vào playlist",
                    "Tạo phòng xem chung tối đa 2 người"
                ),
                buttonText = "Chọn gói Couple"
            ),
            PaymentPackageUi(
                id = PaymentPackageId.PREMIUM,
                name = "Premium",
                price = "99.000đ / tháng",
                amount = 99000,
                duration = "1 tháng",
                description = "Dành cho nhóm bạn / gia đình",
                features = listOf(
                    "Không quảng cáo",
                    "Chế độ trẻ em",
                    "Lưu video vào playlist",
                    "Tạo phòng xem chung tối đa 10 người",
                    "Mời bạn bè tham gia bằng link"
                ),
                buttonText = "Chọn gói Premium",
                badge = "Phổ biến nhất"
            )
        )
    }
}

@Composable
private fun rememberPaymentMethods(): List<PaymentMethodUi> {
    return remember {
        listOf(
            PaymentMethodUi(
                id = PaymentMethodId.QR_CODE,
                title = "Mã QR MoMo",
                description = "Hiện mã QR để quét bằng app MoMo",
                icon = Icons.Rounded.QrCode2
            ),
            PaymentMethodUi(
                id = PaymentMethodId.WALLET,
                title = "Ví điện tử MoMo",
                description = "Thanh toán nhanh qua ví điện tử Momo",
                icon = Icons.Rounded.AccountBalanceWallet
            )
        )
    }
}

@Composable
private fun PaymentBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(ScreenBackground)
    )
}

@Composable
private fun PaymentTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Thanh toán",
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentHero() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Nâng cấp trải nghiệm xem phim",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp
        )
        Text(
            text = "Xem chung cùng bạn bè, tạo phòng riêng và tận hưởng nhiều tính năng cao cấp hơn.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun PricingSection(
    packages: List<PaymentPackageUi>,
    selectedPackageId: PaymentPackageId?,
    onSelectPackage: (PaymentPackageUi) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 720.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                packages.forEach { packageUi ->
                    PricingCard(
                        packageUi = packageUi,
                        selected = selectedPackageId == packageUi.id,
                        onSelect = { onSelectPackage(packageUi) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                packages.forEach { packageUi ->
                    PricingCard(
                        packageUi = packageUi,
                        selected = selectedPackageId == packageUi.id,
                        onSelect = { onSelectPackage(packageUi) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingCard(
    packageUi: PaymentPackageUi,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val isPremium = packageUi.id == PaymentPackageId.PREMIUM
    val selectionColor = packageUi.id.selectionColor()
    val backgroundBrush = when {
        selected -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                selectionColor.copy(alpha = 0.16f),
                Color(0xFF101010)
            )
        )
        isPremium -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF1F1F1F),
                Color(0xFF242424),
                Color(0xFF171717)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(SurfaceDark, Color(0xFF101010))
        )
    }
    val borderBrush = when {
        selected -> Brush.linearGradient(listOf(selectionColor, selectionColor.copy(alpha = 0.45f), Color.White.copy(alpha = 0.12f)))
        isPremium -> Brush.linearGradient(listOf(AccentGold.copy(alpha = 0.95f), Color(0xFFFF7D8A).copy(alpha = 0.65f)))
        else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.07f)))
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(brush = backgroundBrush)
            .border(if (selected) 2.dp else 1.dp, borderBrush, shape)
            .clickable(onClick = onSelect)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gói ${packageUi.name}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = packageUi.description,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }

            PlanSelectionIndicator(
                selected = selected,
                color = selectionColor,
                onClick = onSelect
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            packageUi.badge?.let { BadgePill(text = it) }
        }

        Text(
            text = packageUi.price,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            packageUi.features.forEach { feature ->
                FeatureRow(text = feature, highlighted = selected || isPremium)
            }
        }

    }
}

@Composable
private fun PlanSelectionIndicator(
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (selected) color else Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = if (selected) color else Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Gói đang được chọn",
                tint = Color(0xFF060914),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PaymentMethodSection(
    methods: List<PaymentMethodUi>,
    selectedMethodId: PaymentMethodId?,
    onSelectMethod: (PaymentMethodUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Phương thức thanh toán",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 560.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    methods.forEach { method ->
                        PaymentMethodCard(
                            method = method,
                            selected = selectedMethodId == method.id,
                            onClick = { onSelectMethod(method) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    methods.forEach { method ->
                        PaymentMethodCard(
                            method = method,
                            selected = selectedMethodId == method.id,
                            onClick = { onSelectMethod(method) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethodUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(if (selected) AccentGold.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AccentGold else Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = method.icon,
            contentDescription = null,
            tint = if (selected) AccentGold else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = method.description,
                color = Color.White.copy(alpha = 0.56f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) AccentGold else Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OrderSummary(
    selectedPackage: PaymentPackageUi,
    selectedPaymentMethod: PaymentMethodUi,
    onConfirmPayment: () -> Unit,
    onChangeSelection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Xác nhận đơn hàng",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Kiểm tra thông tin gói trước khi xác nhận thanh toán.",
            color = Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.bodyMedium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryRow(label = "Tên gói", value = "Gói ${selectedPackage.name}")
            SummaryRow(label = "Giá", value = selectedPackage.price)
            SummaryRow(label = "Thời hạn", value = selectedPackage.duration)
            SummaryRow(label = "Phương thức", value = selectedPaymentMethod.title)
        }

        PaymentPolicyNotice()

        Button(
            onClick = onConfirmPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = Color(0xFF060914)
            )
        ) {
            Text(
                text = "Xác nhận thanh toán",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }

        TextButton(
            onClick = onChangeSelection,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Đổi gói hoặc phương thức", color = Color.White.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

@Composable
private fun ProcessingPaymentState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = AccentGold,
            trackColor = Color.White.copy(alpha = 0.12f),
            strokeWidth = 4.dp,
            modifier = Modifier.size(54.dp)
        )
    }
}

@Composable
private fun PaymentSuccessModal(
    packageName: String,
    onDismiss: () -> Unit
) {
    ResultModalShell {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = AccentTeal,
            modifier = Modifier.size(58.dp)
        )
        Text(
            text = "Thanh toán thành công",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Bạn đã nâng cấp lên gói $packageName",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = Color(0xFF060914)
            )
        ) {
            Text("Hoàn tất", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PaymentFailedModal(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ResultModalShell {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(58.dp)
        )
        Text(
            text = "Thanh toán thất bại",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = errorMessage,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = Color(0xFF060914)
            )
        ) {
            Text("Thử lại", fontWeight = FontWeight.ExtraBold)
        }
        TextButton(onClick = onDismiss) {
            Text("Đóng", color = Color.White.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun ResultModalShell(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(28.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun PremiumIcon() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(AccentGold.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.WorkspacePremium,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun BadgePill(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, AccentGold.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun SelectedPill() {
    Text(
        text = "Đang chọn",
        color = AccentTeal,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentTeal.copy(alpha = 0.12f))
            .border(1.dp, AccentTeal.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun FeatureRow(
    text: String,
    highlighted: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (highlighted) AccentTeal else AccentGold,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun PaymentPolicyNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.62f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Giao dịch được bảo mật. Bạn có thể hủy gia hạn bất cứ lúc nào.",
            color = Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
