package com.example.alphacinema.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun formatFirestoreDate(timestamp: Timestamp?): String? {
    if (timestamp == null) return null
    return SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(timestamp.toDate())
}
