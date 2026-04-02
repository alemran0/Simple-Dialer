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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SettingsActivity
import com.simplemobiletools.dialer.helpers.SIP_REGISTRATION_CHANNEL_ID
import com.simplemobiletools.dialer.helpers.SIP_REGISTRATION_NOTIFICATION_ID
import com.simplemobiletools.dialer.helpers.SipManagerWrapper
import com.simplemobiletools.dialer.helpers.SipRegistrationState
import org.sipdroid.sipua.ui.Receiver

class SipRegistrationService : Service() {

    companion object {
        private const val TAG = "SipRegistrationService"

        fun getStartIntent(context: Context) = Intent(context, SipRegistrationService::class.java)
        fun getStopIntent(context: Context) = Intent(context, SipRegistrationService::class.java).also {
            it.action = ACTION_STOP
        }

        private const val ACTION_STOP = "com.simplemobiletools.dialer.sip.STOP_REGISTRATION"
    }

    private val stateListener: (SipRegistrationState) -> Unit = { state ->
        updateNotification(state)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // sContext must be set before SipdroidEngine.register() / registerMore() / listen()
        // are called, otherwise those methods return immediately as a no-op.
        Receiver.sContext = this
        SipManagerWrapper.getInstance(this).addStateListener(stateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(SIP_REGISTRATION_NOTIFICATION_ID, buildNotification(SipRegistrationState.CONNECTING))

        val sipWrapper = SipManagerWrapper.getInstance(this)
        sipWrapper.register { success, error ->
            if (!success) {
                Log.w(TAG, "SIP registration could not start: $error")
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        SipManagerWrapper.getInstance(this).removeStateListener(stateListener)
        SipManagerWrapper.getInstance(this).unregister()
        Receiver.sContext = null
        super.onDestroy()
    }

    private fun buildNotification(state: SipRegistrationState = SipRegistrationState.CONNECTING): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (state) {
            SipRegistrationState.REGISTERED -> getString(R.string.sip_status_registered)
            SipRegistrationState.CONNECTING -> getString(R.string.sip_status_connecting)
            SipRegistrationState.ERROR -> getString(R.string.sip_status_error)
            SipRegistrationState.DISCONNECTED -> getString(R.string.sip_status_disconnected)
        }

        return NotificationCompat.Builder(this, SIP_REGISTRATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.sip_registration_active))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_phone_vector)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(state: SipRegistrationState) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(SIP_REGISTRATION_NOTIFICATION_ID, buildNotification(state))
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
