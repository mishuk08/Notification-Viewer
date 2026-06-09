package com.example.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LiquidNotificationMediator {
    private val _incomingNotifications = MutableSharedFlow<NotificationEntity>(extraBufferCapacity = 64)
    val incomingNotifications: SharedFlow<NotificationEntity> = _incomingNotifications.asSharedFlow()

    fun notifyReceived(notification: NotificationEntity) {
        _incomingNotifications.tryEmit(notification)
    }
}
