package com.simplemobiletools.dialer.activities

import android.content.Context
import android.content.Intent
import android.net.sip.SipAudioCall
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.databinding.ActivitySipCallBinding
import com.simplemobiletools.dialer.helpers.SipManagerWrapper
import com.simplemobiletools.dialer.helpers.SipManagerWrapper.Companion.CALL_TIMEOUT_SECONDS

@Suppress("DEPRECATION")
class SipCallActivity : SimpleActivity() {

    companion object {
        private const val TAG = "SipCallActivity"
        private const val EXTRA_CALLER_URI = "caller_uri"
        private const val EXTRA_TARGET_NUMBER = "target_number"
        private const val EXTRA_IS_INCOMING = "is_incoming"

        fun getIncomingCallIntent(context: Context, callerUri: String): Intent {
            return Intent(context, SipCallActivity::class.java).apply {
                putExtra(EXTRA_CALLER_URI, callerUri)
                putExtra(EXTRA_IS_INCOMING, true)
            }
        }

        fun getOutgoingCallIntent(context: Context, targetNumber: String): Intent {
            return Intent(context, SipCallActivity::class.java).apply {
                putExtra(EXTRA_TARGET_NUMBER, targetNumber)
                putExtra(EXTRA_IS_INCOMING, false)
            }
        }
    }

    private val binding by viewBinding(ActivitySipCallBinding::inflate)
    private val sipWrapper by lazy { SipManagerWrapper.getInstance(this) }

    private var isIncoming = false
    private var callerUri = ""
    private var targetNumber = ""
    private var callDurationSeconds = 0
    private val durationHandler = Handler(Looper.getMainLooper())
    private val durationRunnable = object : Runnable {
        override fun run() {
            callDurationSeconds++
            updateCallDuration()
            durationHandler.postDelayed(this, 1000)
        }
    }

    private val callListener = object : SipAudioCall.Listener() {
        override fun onCallEstablished(call: SipAudioCall?) {
            Log.d(TAG, "SIP call established")
            runOnUiThread {
                binding.sipCallStatus.text = getString(R.string.ongoing_call)
                binding.sipCallAcceptButton.visibility = android.view.View.GONE
                durationHandler.post(durationRunnable)
            }
            call?.startAudio()
            call?.setSpeakerMode(false)
        }

        override fun onCallEnded(call: SipAudioCall?) {
            Log.d(TAG, "SIP call ended")
            runOnUiThread {
                binding.sipCallStatus.text = getString(R.string.call_ended)
                finishCall()
            }
        }

        override fun onError(call: SipAudioCall?, errorCode: Int, errorMessage: String?) {
            Log.e(TAG, "SIP call error ($errorCode): $errorMessage")
            runOnUiThread {
                toast(R.string.unknown_error_occurred)
                finishCall()
            }
        }

        override fun onRinging(call: SipAudioCall?, caller: android.net.sip.SipSession?) {
            runOnUiThread {
                binding.sipCallStatus.text = getString(R.string.is_calling)
            }
        }

        override fun onRingingBack(call: SipAudioCall?) {
            runOnUiThread {
                binding.sipCallStatus.text = getString(R.string.dialing)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Keep screen on and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
        callerUri = intent.getStringExtra(EXTRA_CALLER_URI) ?: ""
        targetNumber = intent.getStringExtra(EXTRA_TARGET_NUMBER) ?: ""

        val displayNumber = if (isIncoming) callerUri else targetNumber
        binding.sipCallerUri.text = displayNumber

        if (isIncoming) {
            binding.sipCallStatus.text = getString(R.string.is_calling)
            binding.sipCallAcceptButton.visibility = android.view.View.VISIBLE
            binding.sipCallAcceptButton.setOnClickListener { acceptIncomingCall() }
            // Attach this activity's listener to the incoming call stored by SipCallReceiver
            sipWrapper.attachCallListener(callListener)
        } else {
            binding.sipCallStatus.text = getString(R.string.dialing)
            binding.sipCallAcceptButton.visibility = android.view.View.GONE
            startOutgoingCall()
        }

        binding.sipCallEndButton.setOnClickListener { endCall() }
    }

    private fun startOutgoingCall() {
        sipWrapper.placeCall(targetNumber, callListener)
        binding.sipCallStatus.text = getString(R.string.dialing)
    }

    private fun acceptIncomingCall() {
        try {
            val call = sipWrapper.activeAudioCall ?: run {
                toast(R.string.unknown_error_occurred)
                finishCall()
                return
            }
            call.answerCall(CALL_TIMEOUT_SECONDS)
            call.startAudio()
            call.setSpeakerMode(false)
            binding.sipCallStatus.text = getString(R.string.ongoing_call)
            binding.sipCallAcceptButton.visibility = android.view.View.GONE
            durationHandler.post(durationRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting SIP call", e)
            toast(e.message ?: getString(R.string.unknown_error_occurred))
            finishCall()
        }
    }

    private fun endCall() {
        sipWrapper.endActiveCall()
        finishCall()
    }

    private fun updateCallDuration() {
        val minutes = callDurationSeconds / 60
        val seconds = callDurationSeconds % 60
        binding.sipCallDuration.text = String.format("%02d:%02d", minutes, seconds)
        binding.sipCallDuration.visibility = android.view.View.VISIBLE
    }

    private fun finishCall() {
        durationHandler.removeCallbacks(durationRunnable)
        finish()
    }

    override fun onDestroy() {
        durationHandler.removeCallbacks(durationRunnable)
        super.onDestroy()
    }
}
