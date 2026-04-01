package com.simplemobiletools.dialer.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.databinding.ActivitySipCallBinding
import com.simplemobiletools.dialer.helpers.SipManagerWrapper
import org.sipdroid.sipua.UserAgent
import org.sipdroid.sipua.ui.Receiver

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

    private val stateListener = object : Receiver.StateListener {
        override fun onCallStateChanged(state: Int, caller: String?) {
            runOnUiThread {
                when (state) {
                    UserAgent.UA_STATE_INCALL -> {
                        binding.sipCallStatus.text = getString(R.string.ongoing_call)
                        binding.sipCallAcceptButton.visibility = View.GONE
                        durationHandler.post(durationRunnable)
                    }
                    UserAgent.UA_STATE_IDLE -> {
                        binding.sipCallStatus.text = getString(R.string.call_ended)
                        finishCall()
                    }
                    UserAgent.UA_STATE_OUTGOING_CALL -> {
                        binding.sipCallStatus.text = getString(R.string.dialing)
                    }
                    UserAgent.UA_STATE_INCOMING_CALL -> {
                        binding.sipCallStatus.text = getString(R.string.is_calling)
                    }
                }
            }
        }

        override fun onRegistered() {}
        override fun onUnregistered() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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

        Receiver.stateListener = stateListener

        if (isIncoming) {
            binding.sipCallStatus.text = getString(R.string.is_calling)
            binding.sipCallAcceptButton.visibility = View.VISIBLE
            binding.sipCallAcceptButton.setOnClickListener { acceptIncomingCall() }
        } else {
            binding.sipCallStatus.text = getString(R.string.dialing)
            binding.sipCallAcceptButton.visibility = View.GONE
            startOutgoingCall()
        }

        binding.sipCallEndButton.setOnClickListener { endCall() }
    }

    private fun startOutgoingCall() {
        sipWrapper.placeCall(targetNumber)
    }

    private fun acceptIncomingCall() {
        try {
            sipWrapper.answerCall()
            binding.sipCallStatus.text = getString(R.string.ongoing_call)
            binding.sipCallAcceptButton.visibility = View.GONE
            durationHandler.post(durationRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting SIP call", e)
            toast(e.message ?: getString(R.string.unknown_error_occurred))
            finishCall()
        }
    }

    private fun endCall() {
        sipWrapper.endCall()
        finishCall()
    }

    private fun updateCallDuration() {
        val minutes = callDurationSeconds / 60
        val seconds = callDurationSeconds % 60
        binding.sipCallDuration.text = String.format("%02d:%02d", minutes, seconds)
        binding.sipCallDuration.visibility = View.VISIBLE
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
