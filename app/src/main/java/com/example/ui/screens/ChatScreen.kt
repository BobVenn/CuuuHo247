package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.ChatMessage
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    request: RescueRequest,
    currentUserProfile: UserProfile?,
    chatMessages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onCallUser: (() -> Unit)? = null
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("vi", "VN")) }

    val isCustomer = currentUserProfile?.role == AppConfig.UserRole.USER || currentUserProfile?.uid == request.userId
    val otherPartyName = if (isCustomer) {
        request.staffName?.ifBlank { null } ?: "Kỹ thuật viên cứu hộ"
    } else {
        request.userName.ifBlank { "Khách hàng" }
    }
    val otherPartyRole = if (isCustomer) "Kỹ Thuật Viên 24/7" else "Khách Hàng Cần Cứu Hộ"

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherPartyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$otherPartyRole • Đơn #${request.id.takeLast(6)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (onCallUser != null) {
                        IconButton(onClick = onCallUser) {
                            Icon(Icons.Default.Phone, contentDescription = "Gọi thoại qua App", tint = SafeGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column {
                    // Quick Canned Emergency Chips - tailored for Customer vs Technician
                    val quickMessages = if (isCustomer) {
                        listOf(
                            "Tôi đang đứng ở lề đường bên phải",
                            "Khoảng bao lâu nữa anh tới nơi?",
                            "Xe của tôi không đề nổ được",
                            "Anh mang giúp dây câu bình nhé",
                            "Tôi đã bật đèn cảnh báo hazard",
                            "Vị trí trên bản đồ là chính xác"
                        )
                    } else {
                        listOf(
                            "Chào bạn, tôi đã nhận đơn và đang xuất phát!",
                            "Tôi đang di chuyển, khoảng 5 - 10 phút nữa tới nơi",
                            "Bạn đứng vị trí an toàn, giữ liên lạc nhé",
                            "Tôi đã đến hiện trường và đang kiểm tra xe",
                            "Sự cố đã được khắc phục xong rồi bạn nhé"
                        )
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickMessages) { quickText ->
                            SuggestionChip(
                                onClick = { onSendMessage(quickText) },
                                label = { Text(quickText, fontSize = 11.5.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (isCustomer) "Nhắn tin cho Kỹ thuật viên..." else "Nhắn tin cho Khách hàng...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_chat_message"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3
                        )

                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = RescuePrimary),
                            modifier = Modifier.testTag("btn_send_chat")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pending notice if customer is waiting for technician acceptance
            if (isCustomer && request.staffName.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Đang điều phối kỹ thuật viên tiếp nhận đơn. Tin nhắn bạn gửi sẽ hiển thị trực tiếp cho KTV ngay khi nhận đơn.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (chatMessages.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Chưa có tin nhắn nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCustomer) {
                                "Hãy gửi tin nhắn để trao đổi chi tiết tình trạng xe hoặc vị trí chính xác với đội cứu hộ."
                            } else {
                                "Gửi tin nhắn xác nhận lộ trình hoặc hướng dẫn an toàn cho khách hàng tại hiện trường."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Chat Message List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages) { msg ->
                        val isMe = (msg.senderId == currentUserProfile?.uid) ||
                                (msg.senderRole == currentUserProfile?.role && msg.senderName == currentUserProfile?.displayName)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                color = if (isMe) RescuePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    if (!isMe) {
                                        val roleLabel = if (msg.senderRole == AppConfig.UserRole.STAFF) "Kỹ thuật viên" else "Khách hàng"
                                        Text(
                                            text = "${msg.senderName} ($roleLabel)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RescuePrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Text(
                                        text = msg.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        fontSize = 10.sp,
                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
