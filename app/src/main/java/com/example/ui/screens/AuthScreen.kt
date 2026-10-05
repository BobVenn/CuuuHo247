package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Đăng nhập, 1: Đăng ký
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Login Fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }
    var loginPassVisible by remember { mutableStateOf(false) }

    // Register Fields
    var regEmail by remember { mutableStateOf("") }
    var regPass by remember { mutableStateOf("") }
    var regConfirmPass by remember { mutableStateOf("") }
    var regDisplayName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regVehicleType by remember { mutableStateOf(AppConfig.VEHICLE_TYPES[0]) }
    var regVehicleName by remember { mutableStateOf("") }
    var regLicensePlate by remember { mutableStateOf("") }
    var regPassVisible by remember { mutableStateOf(false) }
    var regRole by remember { mutableStateOf(AppConfig.UserRole.USER) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val successMessage by authViewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Icon & Brand Title
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = RescuePrimary,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CỨU HỘ XE 24/7",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )

            Text(
                text = "Hệ thống cứu hộ giao thông 24/7",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Tab Selector (Grab / Gojek Style)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_tab_row")
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedTab = 0
                                authViewModel.clearMessages()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Đăng Nhập",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) RescuePrimary else OnSurfaceVariantLight,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedTab = 1
                                authViewModel.clearMessages()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Đăng Ký",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) RescuePrimary else OnSurfaceVariantLight,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                // LOGIN FORM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RescuePrimary) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login_email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = loginPass,
                            onValueChange = { loginPass = it },
                            label = { Text("Mật khẩu") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RescuePrimary) },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { loginPassVisible = !loginPassVisible }) {
                                    Icon(
                                        imageVector = if (loginPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login_password"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showForgotPasswordDialog = true }) {
                                Text("Quên mật khẩu?", color = RescuePrimary, fontSize = 13.sp)
                            }
                        }

                        // Inline Error Alert
                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AlertRedLight,
                                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AlertRed, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AlertRedDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { authViewModel.clearMessages() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = AlertRedDark, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                authViewModel.signIn(loginEmail, loginPass, autoCreateIfNotFound = true, onSuccess = onAuthSuccess)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_submit_login"),
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("ĐĂNG NHẬP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "HOẶC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        // Google Sign-In Button
                        GoogleSignInButton(
                            isLoading = isLoading,
                            text = "Đăng nhập bằng Google",
                            onClick = {
                                authViewModel.signInWithGoogle(context, onSuccess = onAuthSuccess)
                            }
                        )

                        // Switch to Register Prompt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chưa có tài khoản?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(
                                onClick = {
                                    selectedTab = 1
                                    regEmail = loginEmail
                                    regPass = loginPass
                                    regConfirmPass = loginPass
                                    authViewModel.clearMessages()
                                }
                            ) {
                                Text("Đăng ký ngay", color = RescuePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                // REGISTER FORM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = regDisplayName,
                            onValueChange = { regDisplayName = it },
                            label = { Text("Họ và tên *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_name"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("Số điện thoại liên hệ *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_phone"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Email đăng nhập *") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regPass,
                            onValueChange = { regPass = it },
                            label = { Text("Mật khẩu (tối thiểu 6 ký tự) *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { regPassVisible = !regPassVisible }) {
                                    Icon(
                                        imageVector = if (regPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (regPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_pass"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regConfirmPass,
                            onValueChange = { regConfirmPass = it },
                            label = { Text("Xác nhận mật khẩu *") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Loại tài khoản đăng ký:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = regRole == AppConfig.UserRole.USER,
                                onClick = { regRole = AppConfig.UserRole.USER },
                                label = { Text("Chủ Xe / Khách Hàng", fontSize = 11.5.sp, fontWeight = if (regRole == AppConfig.UserRole.USER) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = regRole == AppConfig.UserRole.STAFF,
                                onClick = { regRole = AppConfig.UserRole.STAFF },
                                label = { Text("Kỹ Thuật Viên", fontSize = 11.5.sp, fontWeight = if (regRole == AppConfig.UserRole.STAFF) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = if (regRole == AppConfig.UserRole.STAFF) "Phương tiện cứu hộ phục vụ (Tùy chọn):" else "Thông tin phương tiện (Tùy chọn):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Vehicle type selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppConfig.VEHICLE_TYPES.take(2).forEach { type ->
                                FilterChip(
                                    selected = regVehicleType == type,
                                    onClick = { regVehicleType = type },
                                    label = { Text(type, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = regVehicleName,
                            onValueChange = { regVehicleName = it },
                            label = { Text(if (regRole == AppConfig.UserRole.STAFF) "Loại xe cứu hộ (vd: Hyundai Mighty sàn trượt, Xe cẩu kéo)" else "Tên xe (vd: Toyota Vios, Honda AirBlade)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = regLicensePlate,
                            onValueChange = { regLicensePlate = it },
                            label = { Text("Biển số xe (vd: 30K - 888.99)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_plate"),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                authViewModel.signUp(
                                    email = regEmail,
                                    pass = regPass,
                                    confirmPass = regConfirmPass,
                                    displayName = regDisplayName,
                                    phone = regPhone,
                                    vehicleType = regVehicleType,
                                    vehicleName = regVehicleName,
                                    licensePlate = regLicensePlate,
                                    role = regRole,
                                    onSuccess = onAuthSuccess
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_submit_register"),
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("TẠO TÀI KHOẢN MỚI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "HOẶC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        // Google Sign-In Button
                        GoogleSignInButton(
                            isLoading = isLoading,
                            text = "Đăng ký nhanh bằng Google",
                            onClick = {
                                authViewModel.signInWithGoogle(context, onSuccess = onAuthSuccess)
                            }
                        )

                        // Switch to Login Prompt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Đã có tài khoản?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(
                                onClick = {
                                    selectedTab = 0
                                    loginEmail = regEmail
                                    loginPass = regPass
                                    authViewModel.clearMessages()
                                }
                            ) {
                                Text("Đăng nhập ngay", color = RescuePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Security & Privacy Trust Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bảo mật tài khoản & dữ liệu cứu hộ theo tiêu chuẩn Google Play",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(loginEmail) }
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Đặt Lại Mật Khẩu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nhập email đã đăng ký. Hệ thống Firebase sẽ gửi liên kết để bạn đặt lại mật khẩu mới.")
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email của bạn") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.sendPasswordReset(resetEmail) {
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                ) {
                    Text("Gửi Liên Kết")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Đăng nhập bằng Google"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_google_sign_in"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        enabled = !isLoading
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val stroke = s * 0.22f
        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)
        val red = Color(0xFFEA4335)

        val rect = Rect(stroke / 2, stroke / 2, s - stroke / 2, s - stroke / 2)
        val style = Stroke(width = stroke, cap = StrokeCap.Butt)

        // Red arc (top)
        drawArc(red, 180f, 135f, false, topLeft = rect.topLeft, size = rect.size, style = style)
        // Yellow arc (bottom-left)
        drawArc(yellow, 135f, 90f, false, topLeft = rect.topLeft, size = rect.size, style = style)
        // Green arc (bottom)
        drawArc(green, 45f, 90f, false, topLeft = rect.topLeft, size = rect.size, style = style)
        // Blue arc (right-bottom)
        drawArc(blue, 315f, 90f, false, topLeft = rect.topLeft, size = rect.size, style = style)

        // Center crossbar of 'G'
        drawLine(
            color = blue,
            start = Offset(s / 2, s / 2),
            end = Offset(s - stroke / 2, s / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )
    }
}
