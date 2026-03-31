package com.furtabs.paperclip.data

import com.furtabs.paperclip.tracking.database.TrackingEntity

data class BackupData(
    val version: Int = 1,
    val userName: String,
    val userBio: String,
    val history: List<TrackingEntity>,
    val timestamp: Long = System.currentTimeMillis()
)