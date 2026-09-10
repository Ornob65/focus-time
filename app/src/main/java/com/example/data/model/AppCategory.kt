package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AppCategory(val displayName: String, val color: Color) {
    SOCIAL("Social Media", Color(0xFFE11D48)),
    PRODUCTIVITY("Productivity", Color(0xFF10B981)),
    ENTERTAINMENT("Entertainment", Color(0xFF8B5CF6)),
    EDUCATION("Education", Color(0xFF06B6D4)),
    UTILITY("Utility", Color(0xFFF59E0B)),
    OTHER("Other", Color(0xFF64748B))
}
