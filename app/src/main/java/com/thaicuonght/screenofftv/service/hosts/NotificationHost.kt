package com.thaicuonght.screenofftv.service.hosts

import com.thaicuonght.screenofftv.i18n.localizedString
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleOwner
import com.thaicuonght.screenofftv.ScreenOffTVNotificationChannels
import com.thaicuonght.screenofftv.R
import com.thaicuonght.screenofftv.service.ScreenOffTVService

private const val TAG = "NotificationHost"

class NotificationHost<T>(
    private val owner: T,
) : LifecycleOwner by owner
        where T : Context, T : LifecycleOwner {

    companion object {
        const val NOTIFICATION_ID = 1
    }

    private val manager = NotificationManagerCompat.from(owner)

    private val builder =
        NotificationCompat.Builder(owner, ScreenOffTVNotificationChannels.Service.id)
            .setSmallIcon(R.drawable.screen_off_tv_24px)
            .setContentTitle(owner.localizedString(R.string.ScreenOffTV_service_is_running))
            .setOnlyAlertOnce(true)
            .setOngoing(true)

    private val basicIntent
        get() = Intent(
            owner, ScreenOffTVService::class.java
        )

    private fun Intent.toPendingIntent(requestCode: Int = 0) = PendingIntent.getService(
        owner,
        requestCode,
        this,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun NotificationCompat.Builder.turnScreenOnAction(): NotificationCompat.Builder {
        val intent = basicIntent.apply {
            val value = ScreenOffTVService.EXTRA_VALUE_SCREEN_ON
            putExtra(ScreenOffTVService.EXTRA_SCREEN, value)
        }
        val text = owner.localizedString(R.string.Turn_screen_on)
        val action = NotificationCompat.Action.Builder(
            null, text, intent.toPendingIntent(0)
        ).build()
        return addAction(action)
    }

    private fun NotificationCompat.Builder.turnScreenOffAction(): NotificationCompat.Builder {
        val intent = basicIntent.apply {
            val value = ScreenOffTVService.EXTRA_VALUE_SCREEN_OFF
            putExtra(ScreenOffTVService.EXTRA_SCREEN, value)
        }
        val text = owner.localizedString(R.string.Turn_screen_off)
        val action = NotificationCompat.Action.Builder(
            null, text, intent.toPendingIntent(1)
        ).build()
        return addAction(action)
    }

    private fun NotificationCompat.Builder.stopServiceAction(): NotificationCompat.Builder {
        val intent = basicIntent.apply {
            val value = ScreenOffTVService.EXTRA_VALUE_STOP
            putExtra(ScreenOffTVService.EXTRA_STOP, value)
        }
        val text = owner.localizedString(R.string.Stop)
        val action = NotificationCompat.Action.Builder(
            null, text, intent.toPendingIntent(5)
        ).build()
        return addAction(action)
    }

    fun notify(
        isScreenOn: Boolean
    ) = builder
        .setContentTitle(owner.localizedString(R.string.ScreenOffTV_service_is_running))
        .setContentText(
            "${owner.localizedString(R.string.Expected_screen_state)} : ${
                getCurrentStateText(
                    isScreenOn
                )
            }"
        )
        .clearActions()
        .let {
            if (isScreenOn) it.turnScreenOffAction()
            else it.turnScreenOnAction()
        }
        .stopServiceAction()
        .build().also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (
                    owner.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    manager.notify(NOTIFICATION_ID, it)
                }
            } else {
                manager.notify(NOTIFICATION_ID, it)
            }
        }

    private fun getCurrentStateText(isScreenOn: Boolean): String {
        return when (isScreenOn) {
            true -> owner.localizedString(R.string.On)
            false -> owner.localizedString(R.string.Off)
        }
    }

}