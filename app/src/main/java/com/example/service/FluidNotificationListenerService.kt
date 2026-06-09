package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.LiquidNotificationMediator
import com.example.data.NotificationDatabase
import com.example.data.NotificationEntity
import com.example.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FluidNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        val database = NotificationDatabase.getDatabase(applicationContext)
        repository = NotificationRepository(database.notificationDao())
        Log.i("FluidNotificationListener", "Fluid Intercept Service Started Successfully!")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Skip ongoing system notifications or empty triggers
        if (sbn.isOngoing) return

        val packageName = sbn.packageName ?: ""
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getString(Notification.EXTRA_TITLE) ?: ""
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        // Filter out blank system/layout spam
        if (title.isBlank() && message.isBlank()) return

        // Determine Platform & App Info
        val platformInfo = mapPackageToPlatform(packageName)
        val appName = platformInfo.appName
        val platform = platformInfo.platform
        val type = if (platform == "phone" || title.contains("call", ignoreCase = true) || message.contains("call", ignoreCase = true)) {
            "CALL"
        } else {
            "MESSAGE"
        }

        val entity = NotificationEntity(
            title = title,
            message = message,
            appName = appName,
            platform = platform,
            type = type,
            timestamp = System.currentTimeMillis()
        )

        serviceScope.launch {
            // Persist locally in Database
            val insertedId = repository.insert(entity)
            val insertedEntity = entity.copy(id = insertedId)
            
            // Broadcast event in memory to trigger liquid animations
            LiquidNotificationMediator.notifyReceived(insertedEntity)
        }
    }

    private data class PlatformMapping(val platform: String, val appName: String)

    private fun mapPackageToPlatform(packageName: String): PlatformMapping {
        return when {
            packageName.contains("whatsapp", ignoreCase = true) -> 
                PlatformMapping("whatsapp", "WhatsApp")
            packageName.contains("facebook.orca", ignoreCase = true) || packageName.contains("messenger", ignoreCase = true) -> 
                PlatformMapping("messenger", "Messenger")
            packageName.contains("telegram", ignoreCase = true) -> 
                PlatformMapping("telegram", "Telegram")
            packageName.contains("instagram", ignoreCase = true) -> 
                PlatformMapping("instagram", "Instagram")
            packageName.contains("gmail", ignoreCase = true) -> 
                PlatformMapping("gmail", "Gmail")
            packageName.contains("android.apps.messaging", ignoreCase = true) || packageName.contains("mms", ignoreCase = true) || packageName.contains("sms", ignoreCase = true) -> 
                PlatformMapping("sms", "SMS")
            packageName.contains("telecom", ignoreCase = true) || packageName.contains("dialer", ignoreCase = true) || packageName.contains("phone", ignoreCase = true) -> 
                PlatformMapping("phone", "Phone Call")
            else -> {
                // Infer stylized app name from package
                val simpleName = packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                PlatformMapping("other", simpleName)
            }
        }
    }
}
