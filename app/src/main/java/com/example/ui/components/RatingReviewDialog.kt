package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingReviewDialog(
    staffName: String?,
    issueType: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(5) }
    var commentText by remember { mutableStateOf("") }
    val quickTags = remember {
        listOf(
            "⚡ Đến rất nhanh (15p)",
            "🔧 Tay nghề thợ giỏi",
            "😊 Thái độ nhiệt tình",
            "💰 Giá cả minh bạch",
            "🛡️ Cứu hộ an toàn",
            "⭐ Rất hài lòng"
        )
    }

    val ratingLabels = mapOf(
        1 to "Rất không hài lòng 😞",
        2 to "Tạm được 😐",
        3 to "Bình thường 🙂",
        4 to "Hài lòng & Tốt 😊",
        5 to "Rất xuất sắc & Tuyệt vời! 🌟"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("rating_review_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Đánh Giá Dịch Vụ", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.StarRate, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(34.dp))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bạn thấy dịch vụ của ${staffName ?: "Kỹ thuật viên"} thế nào?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sự cố: $issueType",
                        fontSize = 11.5.sp,
                        color = OnSurfaceVariantLight
                    )
                }

                // Interactive 5 Star Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { selectedStars = i },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= selectedStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$i sao",
                                tint = if (i <= selectedStars) EmergencyGold else Color(0xFFCBD5E1),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Rating Mood Label
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Text(
                        text = ratingLabels[selectedStars] ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Quick tags
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickTags.forEach { tag ->
                        val isSelected = commentText.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                commentText = if (isSelected) {
                                    commentText.replace(tag, "").trim()
                                } else {
                                    if (commentText.isBlank()) tag else "$commentText, $tag"
                                }
                            },
                            label = { Text(tag, fontSize = 10.5.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                // Comment input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Nhận xét đóng góp ý kiến (Tùy chọn)") },
                    placeholder = { Text("Thợ đến nhanh, thay lốp rất cẩn thận...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rating_comment"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4
                )

                // Submit Button
                Button(
                    onClick = {
                        val finalComment = commentText.ifBlank { ratingLabels[selectedStars] ?: "Dịch vụ tốt" }
                        onSubmit(selectedStars, finalComment)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_rating"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyGold)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GỬI ĐÁNH GIÁ $selectedStars SAO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
