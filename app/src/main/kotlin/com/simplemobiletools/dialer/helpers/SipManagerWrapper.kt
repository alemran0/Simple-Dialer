package com.simplemobiletools.dialer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.sip.SipAudioCall
import android.net.sip.SipManager
import android.net.sip.SipProfile
import android.net.sip.SipRegistrationListener
import android.util.Log
import com.simplemobiletools.dialer.extensions.config

enum class SipRegistrationState {
    DISCONNECTED, CONNECTING, REGISTERED, ERROR
}

@Suppress("DEPRECATION")
class SipManagerWrapper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SipManagerWrapper"
        private const val REGISTRATION_EXPIRY_SECONDS = 600
        const val CALL_TIMEOUT_SECONDS = 30

        @Volatile
        private var instance: SipManagerWrapper? = null

        fun getInstance(context: Context): SipManagerWrapper {
            return instance ?: synchronized(this) {
                instance ?: SipManagerWrapper(context.applicationContext).also { instance = it }
            }
        }
    }

    private var sipManager: SipManager? = null
    private var sipProfile: SipProfile? = null

    @Volatile
    var registrationState: SipRegistrationState = SipRegistrationState.DISCONNECTED
        private set

    private val stateListeners = mutableListOf<(SipRegistrationState) -> Unit>()

    var activeAudioCall: SipAudioCall? = null
        private set

    val isRegistered: Boolean
        get() = registrationState == SipRegistrationState.REGISTERED

    @SuppressLint("MissingPermission")
    fun initialize() {
        if (!SipManager.isApiSupported(context)) {
            Log.w(TAG, "SIP API not supported on this device")
            return
        }
        if (!SipManager.isVoIpSupported(context)) {
            Log.w(TAG, "VoIP not supported on this device")
            return
        }
        sipManager = SipManager.newInstance(context)
    }

    fun addStateListener(listener: (SipRegistrationState) -> Unit) {
        synchronized(stateListeners) {
            stateListeners.add(listener)
        }
    }

    fun removeStateListener(listener: (SipRegistrationState) -> Unit) {
        synchronized(stateListeners) {
            stateListeners.remove(listener)
        }
    }

    private fun notifyStateChanged(state: SipRegistrationState) {
        registrationState = state
        val listeners = synchronized(stateListeners) { stateListeners.toList() }
        listeners.forEach { it(state) }
    }

    @SuppressLint("MissingPermission")
    fun register(callback: (success: Boolean, error: String?) -> Unit = { _, _ -> }) {
        val cfg = context.config
        if (!cfg.sipEnabled || cfg.sipServer.isBlank() || cfg.sipUsername.isBlank()) {
            callback(false, "SIP not configured")
            return
        }

        if (sipManager == null) {
            initialize()
        }
        val manager = sipManager ?: run {
            callback(false, "SIP not supported")
            return
        }

        try {
            unregisterInternal()

            val profileBuilder = SipProfile.Builder(cfg.sipUsername, cfg.sipServer)
                .setPassword(cfg.sipPassword)
                .setPort(cfg.sipPort)

            if (cfg.sipDisplayName.isNotBlank()) {
                profileBuilder.setDisplayName(cfg.sipDisplayName)
            }

            val profile = profileBuilder.build()
            sipProfile = profile

            notifyStateChanged(SipRegistrationState.CONNECTING)

            manager.register(profile, REGISTRATION_EXPIRY_SECONDS, object : SipRegistrationListener {
                override fun onRegistering(localProfileUri: String?) {
                    Log.d(TAG, "SIP registering: $localProfileUri")
                    notifyStateChanged(SipRegistrationState.CONNECTING)
                }

                override fun onRegistrationDone(localProfileUri: String?, expiryTime: Long) {
                    Log.d(TAG, "SIP registered: $localProfileUri, expiry: $expiryTime")
                    notifyStateChanged(SipRegistrationState.REGISTERED)
                    callback(true, null)
                }

                override fun onRegistrationFailed(localProfileUri: String?, errorCode: Int, errorMessage: String?) {
                    Log.e(TAG, "SIP registration failed ($errorCode): $errorMessage")
                    notifyStateChanged(SipRegistrationState.ERROR)
                    callback(false, errorMessage)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "SIP register exception", e)
            notifyStateChanged(SipRegistrationState.ERROR)
            callback(false, e.message)
        }
    }

    @SuppressLint("MissingPermission")
    fun unregister() {
        unregisterInternal()
        notifyStateChanged(SipRegistrationState.DISCONNECTED)
    }

    @SuppressLint("MissingPermission")
    private fun unregisterInternal() {
        try {
            activeAudioCall?.close()
            activeAudioCall = null

            val profile = sipProfile
            val manager = sipManager
            if (profile != null && manager != null) {
                manager.unregister(profile, null)
            }
            try {
                profile?.let { manager?.close(it.uriString) }
            } catch (ignored: Exception) {
            }
            sipProfile = null
        } catch (e: Exception) {
            Log.e(TAG, "SIP unregister exception", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun placeCall(targetNumber: String, listener: SipAudioCall.Listener?) {
        val manager = sipManager ?: return
        val profile = sipProfile ?: return

        val targetUri = if (targetNumber.startsWith("sip:")) {
            targetNumber
        } else {
            val server = context.config.sipServer
            "sip:$targetNumber@$server"
        }

        try {
            val call = manager.makeAudioCall(
                profile.uriString,
                targetUri,
                listener,
                CALL_TIMEOUT_SECONDS
            )
            activeAudioCall = call
        } catch (e: Exception) {
            Log.e(TAG, "SIP makeAudioCall exception", e)
        }
    }

    fun endActiveCall() {
        try {
            activeAudioCall?.endCall()
            activeAudioCall?.close()
            activeAudioCall = null
        } catch (e: Exception) {
            Log.e(TAG, "SIP endCall exception", e)
        }
    }

    fun setActiveCall(call: SipAudioCall) {
        activeAudioCall = call
    }

    @Suppress("DEPRECATION")
    fun attachCallListener(listener: SipAudioCall.Listener) {
        activeAudioCall?.setListener(listener)
    }
}
