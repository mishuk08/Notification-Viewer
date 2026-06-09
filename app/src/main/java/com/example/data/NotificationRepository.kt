package com.example.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()

    suspend fun insert(notification: NotificationEntity): Long {
        return notificationDao.insertNotification(notification)
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteById(id: Long) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun deleteOldNotifications(cutoffTimestamp: Long) {
        notificationDao.deleteOldNotifications(cutoffTimestamp)
    }

    suspend fun clearAll() {
        notificationDao.clearAllNotifications()
    }
}
