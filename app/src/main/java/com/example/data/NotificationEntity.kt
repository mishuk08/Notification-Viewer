package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val appName: String,
    val platform: String, // e.g. "phone", "sms", "whatsapp", "messenger", "telegram", "instagram", "gmail", "other"
    val type: String, // "CALL" or "MESSAGE"
    val timestamp: Long = System.currentTimeMillis(),
    val senderAvatarSeed: Int = (0..1000).random(), // For generating unique liquid-styled user avatars in Compose
    val isRead: Boolean = false
)
