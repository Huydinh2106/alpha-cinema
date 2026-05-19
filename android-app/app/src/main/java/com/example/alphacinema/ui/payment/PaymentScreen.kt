package com.example.alphacinema.ui.payment

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.alphacinema.data.model.MomoPaymentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ScreenBackground = Color(0xFF070B16)
private val SurfaceDark = Color(0xFF10192E)
private val SurfaceMuted = Color(0xFF151E34)
private val AccentGold = Color(0xFFF6E29A)
private val AccentTeal = Color(0xFF63E6D8)
private val ErrorRed = Color(0xFFFF7A7A)

private enum class PaymentPackageId {
    BASIC,
    COUPLE,
    PREMIUM
}

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
    onContinuePayment: (packageName: String, paymentMethod: String) -> Unit = { _, _ -> },
    viewModel: PaymentViewModel = viewModel()
) {
    val packages = rememberPaymentPackages()
    val paymentMethods = rememberPaymentMethods()
    var selectedPackage by remember { mutableStateOf<PaymentPackageUi?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethodUi?>(null) }
    var currentPackage by remember { mutableStateOf<PaymentPackageUi?>(null) }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.IDLE) }
    val isSelectionComplete = selectedPackage != null && selectedPaymentMethod != null

    val momoResponse by viewModel.paymentResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(momoResponse) {
        if (momoResponse != null) {
            paymentStatus = PaymentStatus.QR_DISPLAYED
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            paymentStatus = PaymentStatus.FAILED
        }
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
                    PaymentHero(currentPackage = currentPackage)

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
                        shape = RoundedCornerShape(18.dp),
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

                PaymentStatus.CONFIRMING -> {
                    val packageUi = selectedPackage
                    val method = selectedPaymentMethod
                    if (packageUi != null && method != null) {
                        OrderSummary(
                            selectedPackage = packageUi,
                            selectedPaymentMethod = method,
                            onConfirmPayment = {
                                if (method.id == PaymentMethodId.WALLET) {
                                    paymentStatus = PaymentStatus.PROCESSING
                                    viewModel.createMomoPayment(
                                        amount = packageUi.amount,
                                        orderInfo = "Thanh toán gói ${packageUi.name} Alpha Cinema"
                                    )
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
                    momoResponse?.let { response ->
                        MomoQrPayment(
                            response = response,
                            onCancel = {
                                paymentStatus = PaymentStatus.IDLE
                                viewModel.clearPaymentResponse()
                            },
                            onSuccess = {
                                currentPackage = selectedPackage
                                paymentStatus = PaymentStatus.SUCCESS
                                selectedPackage?.let { packageUi ->
                                    selectedPaymentMethod?.let { method ->
                                        onContinuePayment(packageUi.name, method.title)
                                    }
                                }
                            }
                        )
                    }
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
                errorMessage = error ?: "Thanh toán thất bại. Vui lòng thử lại.",
                onRetry = { paymentStatus = PaymentStatus.CONFIRMING },
                onDismiss = { 
                    paymentStatus = PaymentStatus.IDLE 
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
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Quét mã MoMo",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = response.qrCodeUrl,
                contentDescription = "Momo QR Code",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Nút tải mã QR xuống
        OutlinedButton(
            onClick = {
                response.qrCodeUrl?.let { url ->
                    scope.launch {
                        saveImageToGallery(context, url)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
            modifier = Modifier.height(42.dp)
        ) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tải mã QR xuống", color = AccentGold, fontSize = 13.sp)
        }

        Text(
            text = "Số tiền: ${response.amount}đ",
            color = AccentGold,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Mở ứng dụng MoMo và quét mã QR để hoàn tất thanh toán.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Hủy bỏ", color = Color.White)
            }
            
            Button(
                onClick = onSuccess,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Đã thanh toán", color = Color(0xFF060914), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private suspend fun saveImageToGallery(context: Context, imageUrl: String) {
    withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap

            if (bitmap != null) {
                val filename = "MomoQR_${System.currentTimeMillis()}.jpg"
                var fos: java.io.OutputStream? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver?.also { resolver ->
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        }
                        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        fos = imageUri?.let { resolver.openOutputStream(it) }
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val image = java.io.File(imagesDir, filename)
                    fos = java.io.FileOutputStream(image)
                }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã lưu mã QR vào thư viện", Toast.LENGTH_SHORT).show()
                    }
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
                    "Xem phim không giới hạn",
                    "Lưu danh sách phim yêu thích",
                    "Chất lượng HD"
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
                    "Tất cả tính năng của Basic",
                    "Tạo phòng xem chung",
                    "Đồng bộ thời gian xem phim",
                    "Chat trong phòng xem"
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
                    "Tất cả tính năng của Couple",
                    "Tạo nhiều phòng xem chung",
                    "Mời bạn bè tham gia bằng link",
                    "Ưu tiên chất lượng Full HD / 4K",
                    "Không quảng cáo"
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
                id = PaymentMethodId.WALLET,
                title = "Ví điện tử MoMo",
                description = "Thanh toán nhanh qua ví điện tử Momo",
                icon = Icons.Outlined.AccountBalanceWallet
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF202846),
                        Color(0xFF111A2D),
                        ScreenBackground
                    )
                )
            )
    )
}

@Composable
private fun PaymentTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
private fun PaymentHero(currentPackage: PaymentPackageUi?) {
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

        CurrentPackageState(currentPackage = currentPackage)
    }
}

@Composable
private fun CurrentPackageState(currentPackage: PaymentPackageUi?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = null,
            tint = if (currentPackage == null) Color.White.copy(alpha = 0.55f) else AccentGold,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trạng thái người dùng",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = currentPackage?.let { "Đã nâng cấp gói ${it.name}" } ?: "Chưa có gói trả phí",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
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
    val backgroundBrush = if (isPremium) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF171F35),
                Color(0xFF221D34),
                Color(0xFF111A2D)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(SurfaceDark, Color(0xFF0E1628))
        )
    }
    val borderBrush = when {
        selected -> Brush.linearGradient(listOf(AccentGold, AccentTeal))
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
                    color = Color.White.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }

            if (isPremium) {
                PremiumIcon()
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            packageUi.badge?.let { BadgePill(text = it) }
            if (selected) {
                SelectedPill()
            }
        }

        Text(
            text = packageUi.price,
            color = AccentGold,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            packageUi.features.forEach { feature ->
                FeatureRow(text = feature, highlighted = selected || isPremium)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(
            onClick = onSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected || isPremium) AccentGold else SurfaceMuted,
                contentColor = if (selected || isPremium) Color(0xFF060914) else Color.White
            )
        ) {
            Text(
                text = packageUi.buttonText,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
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
            imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            color = AccentGold,
            trackColor = Color.White.copy(alpha = 0.12f),
            strokeWidth = 4.dp,
            modifier = Modifier.size(54.dp)
        )
        Text(
            text = "Đang kết nối MoMo...",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Vui lòng chờ trong giây lát hệ thống đang tạo mã thanh toán.",
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
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
            imageVector = Icons.Outlined.CheckCircle,
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
            imageVector = Icons.Outlined.ErrorOutline,
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
                .background(Color(0xFF10192E))
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
            imageVector = Icons.Outlined.WorkspacePremium,
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
        color = Color(0xFF070B16),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.horizontalGradient(listOf(AccentGold, Color(0xFFFFD1A1))))
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
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (highlighted) AccentTeal else AccentGold,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.82f),
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
            imageVector = Icons.Outlined.Info,
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
