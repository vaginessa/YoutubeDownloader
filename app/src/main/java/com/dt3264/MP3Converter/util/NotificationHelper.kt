package com.dt3264.MP3Converter.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.dt3264.MP3Converter.CONVERTER_NOTIFICATION_CHANNEL
import com.dt3264.MP3Converter.R

/**
 * Created by Khang NT on 1/4/18.
 * Email: khang.neon.1997@gmail.com
 */

class NotificationHelper(private val appContext: Context) {
    private val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        @SuppressLint("NewApi") // kotlin lint bug
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create channel
            if (notificationManager.getNotificationChannel(CONVERTER_NOTIFICATION_CHANNEL) == null) {
                val notificationChannel = NotificationChannel(
                        CONVERTER_NOTIFICATION_CHANNEL,
                        appContext.getString(R.string.notification_channel_converter),
                        NotificationManager.IMPORTANCE_LOW
                )
                notificationChannel.description =
                        appContext.getString(R.string.notification_channel_converter_des)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
    }

    fun createConverterNotification(): NotificationCompat.Builder =
            NotificationCompat.Builder(appContext, CONVERTER_NOTIFICATION_CHANNEL)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_convert_white_24dp)
                    .setContentTitle(appContext.getString(R.string.app_name))
                    .setContentText(appContext.getString(R.string.converter_service_running))

    fun notify(id: Int, notificationBuilder: NotificationCompat.Builder) {
        notificationManager.notify(id, notificationBuilder.build())
    }

    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }

}