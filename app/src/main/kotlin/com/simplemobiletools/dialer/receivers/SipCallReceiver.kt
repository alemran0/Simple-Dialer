package com.simplemobiletools.dialer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * SipCallReceiver is kept for backward compatibility but is no longer needed
 * for incoming SIP calls with the sipdroid stack. Incoming calls are now handled
 * directly by SipdroidEngine via the Receiver.onState() callback.
 */
class SipCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SipCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        // Incoming SIP calls are now handled by SipdroidEngine via Receiver.onState()
    }
}
