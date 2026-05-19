@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.alphacinema.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentGold = Color(0xFFF6E29A)

data class GenreItem(val label: String, val slug: String)

val GENRE_LIST = listOf(
    GenreItem("Hành Động", "hanh-dong"),
    GenreItem("Phiêu Lưu", "phieu-luu"),
    GenreItem("Hoạt Hình", "hoat-hinh"),
    GenreItem("Hài", "hai-huoc"),
    GenreItem("Hình Sự", "hinh-su"),
    GenreItem("Tài Liệu", "tai-lieu"),
    GenreItem("Chính Kịch", "chinh-kich"),
    GenreItem("Gia Đình", "gia-dinh"),
    GenreItem("Viễn Tưởng", "vien-tuong"),
    GenreItem("Cổ Trang", "co-trang"),
    GenreItem("Kinh Dị", "kinh-di"),
    GenreItem("Bí Ẩn", "bi-an"),
    GenreItem("Lãng Mạn", "tinh-cam"),
    GenreItem("Khoa Học", "khoa-hoc"),
    GenreItem("Chiến Tranh", "chien-tranh"),
    GenreItem("Thiếu Nhi", "tre-em"),
    GenreItem("Tâm Lý", "tam-ly"),
    GenreItem("Thể Thao", "the-thao"),
    GenreItem("Âm Nhạc", "am-nhac"),
    GenreItem("Học Đường", "hoc-duong"),
    GenreItem("Võ Thuật", "vo-thuat"),
    GenreItem("Thần Thoại", "than-thoai"),
    GenreItem("Kinh Điển", "kinh-dien"),
    GenreItem("Kỳ Ảo", "phim-18"),
)

@Composable
fun GenreBottomSheet(
    onGenreSelected: (genres: List<GenreItem>, sortField: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedGenres by remember { mutableStateOf(emptySet<GenreItem>()) }
    var selectedSort by remember { mutableStateOf("modified.time") } // "modified.time" or "_id"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B1E2E),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 2.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.5f)) {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thể loại",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Genre chips - scrollable area
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GENRE_LIST.forEach { genre ->
                    val isSelected = genre in selectedGenres
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) AccentGold.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) AccentGold
                                else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedGenres = if (isSelected) {
                                    selectedGenres - genre
                                } else {
                                    selectedGenres + genre
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = genre.label,
                            color = if (isSelected) AccentGold else Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Fixed bottom: Sort + Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Sắp xếp:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SortChip(
                        label = "Mới nhất",
                        isSelected = selectedSort == "modified.time",
                        onClick = { selectedSort = "modified.time" }
                    )
                    SortChip(
                        label = "Xem nhiều",
                        isSelected = selectedSort == "_id",
                        onClick = { selectedSort = "_id" }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (selectedGenres.isEmpty()) return@Button
                        onGenreSelected(selectedGenres.toList(), selectedSort)
                    },
                    enabled = selectedGenres.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.2f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (selectedGenres.isEmpty()) {
                            "Lọc kết quả"
                        } else {
                            "Lọc ${selectedGenres.size} thể loại"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AccentGold.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (isSelected) AccentGold else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) AccentGold else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
