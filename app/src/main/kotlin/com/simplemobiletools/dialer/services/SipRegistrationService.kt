package com.simplemobiletools.dialer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SettingsActivity
import com.simplemobiletools.dialer.helpers.SIP_REGISTRATION_CHANNEL_ID
import com.simplemobiletools.dialer.helpers.SIP_REGISTRATION_NOTIFICATION_ID
import com.simplemobiletools.dialer.helpers.SipManagerWrapper

class SipRegistrationService : Service() {

    companion object {
        fun getStartIntent(context: Context) = Intent(context, SipRegistrationService::class.java)
        fun getStopIntent(context: Context) = Intent(context, SipRegistrationService::class.java).also {
            it.action = ACTION_STOP
        }

        private const val ACTION_STOP = "com.simplemobiletools.dialer.sip.STOP_REGISTRATION"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(SIP_REGISTRATION_NOTIFICATION_ID, buildNotification())

        val sipWrapper = SipManagerWrapper.getInstance(this)
        sipWrapper.register { success, error ->
            if (!success) {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        SipManagerWrapper.getInstance(this).unregister()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SIP_REGISTRATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.sip_registration_active))
            .setContentText(getString(R.string.sip_registration_running_in_background))
            .setSmallIcon(R.drawable.ic_phone_vector)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SIP_REGISTRATION_CHANNEL_ID,
                getString(R.string.sip_registration_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.sip_registration_channel_description)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
