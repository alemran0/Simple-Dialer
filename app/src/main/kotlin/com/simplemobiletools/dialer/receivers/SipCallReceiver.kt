package com.simplemobiletools.dialer.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.sip.SipAudioCall
import android.net.sip.SipManager
import android.util.Log
import com.simplemobiletools.dialer.activities.SipCallActivity
import com.simplemobiletools.dialer.helpers.SIP_INCOMING_CALL_ACTION
import com.simplemobiletools.dialer.helpers.SipManagerWrapper

@Suppress("DEPRECATION")
class SipCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SipCallReceiver"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SIP_INCOMING_CALL_ACTION) return

        val sipManager = SipManager.newInstance(context) ?: return

        try {
            val call = sipManager.takeAudioCall(intent, object : SipAudioCall.Listener() {
                override fun onError(call: SipAudioCall?, errorCode: Int, errorMessage: String?) {
                    Log.e(TAG, "SIP call error ($errorCode): $errorMessage")
                    call?.close()
                }
            }) ?: return

            val callerUri = call.peerProfile?.uriString ?: ""
            SipManagerWrapper.getInstance(context).setActiveCall(call)

            val callIntent = SipCallActivity.getIncomingCallIntent(context, callerUri)
            callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(callIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming SIP call", e)
        }
    }
}
