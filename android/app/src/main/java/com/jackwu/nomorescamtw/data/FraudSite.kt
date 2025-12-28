package com.jackwu.nomorescamtw.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fraud_sites")
data class FraudSite(
    @PrimaryKey val url: String, // Normalized URL (no http/https, no trailing slash)
    val name: String,
    val count: Int,
    val startDate: String,
    val endDate: String
)
