package com.simplemobiletools.dialer.helpers

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import com.simplemobiletools.dialer.activities.SipCallActivity
import com.simplemobiletools.dialer.extensions.config
import org.sipdroid.sipua.UserAgent
import org.sipdroid.sipua.ui.Receiver
import org.sipdroid.sipua.ui.Settings

enum class SipRegistrationState {
    DISCONNECTED, CONNECTING, REGISTERED, ERROR
}

@Suppress("DEPRECATION")
class SipManagerWrapper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SIP_REG"
        const val CALL_TIMEOUT_SECONDS = 30
        private const val REGISTRATION_TIMEOUT_MS = 30_000L

        @Volatile
        private var instance: SipManagerWrapper? = null

        fun getInstance(context: Context): SipManagerWrapper {
            return instance ?: synchronized(this) {
                instance ?: SipManagerWrapper(context.applicationContext).also { instance = it }
            }
        }
    }

    @Volatile
    var registrationState: SipRegistrationState = SipRegistrationState.DISCONNECTED
        private set

    private val stateListeners = mutableListOf<(SipRegistrationState) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val registrationTimeoutRunnable = Runnable {
        if (registrationState == SipRegistrationState.CONNECTING) {
            Log.w(TAG, "Registration timed out after ${REGISTRATION_TIMEOUT_MS / 1000}s — transitioning to ERROR")
            notifyStateChanged(SipRegistrationState.ERROR)
        }
    }

    val isRegistered: Boolean
        get() = registrationState == SipRegistrationState.REGISTERED

    init {
        Receiver.stateListener = object : Receiver.StateListener {
            override fun onCallStateChanged(state: Int, caller: String?) {
                Log.d(TAG, "onCallStateChanged: state=$state caller=$caller")
                when (state) {
                    UserAgent.UA_STATE_INCOMING_CALL -> {
                        val callIntent = SipCallActivity.getIncomingCallIntent(context, caller ?: "")
                        callIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        context.startActivity(callIntent)
                    }
                    else -> {}
                }
            }

            override fun onRegistered() {
                Log.d(TAG, "onRegistered: SIP registration succeeded")
                cancelRegistrationTimeout()
                notifyStateChanged(SipRegistrationState.REGISTERED)
            }

            override fun onUnregistered() {
                Log.d(TAG, "onUnregistered: SIP unregistered")
                cancelRegistrationTimeout()
                notifyStateChanged(SipRegistrationState.DISCONNECTED)
            }

            override fun onRegistrationFailed(reason: String?) {
                Log.w(TAG, "onRegistrationFailed: reason=$reason")
                cancelRegistrationTimeout()
                notifyStateChanged(SipRegistrationState.ERROR)
            }
        }
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

    private fun cancelRegistrationTimeout() {
        mainHandler.removeCallbacks(registrationTimeoutRunnable)
    }

    private fun notifyStateChanged(state: SipRegistrationState) {
        registrationState = state
        val listeners = synchronized(stateListeners) { stateListeners.toList() }
        listeners.forEach { it(state) }
    }

    private fun writeSipConfigToPrefs() {
        val cfg = context.config
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().apply {
            putString(Settings.PREF_USERNAME, cfg.sipUsername)
            putString(Settings.PREF_PASSWORD, cfg.sipPassword)
            putString(Settings.PREF_SERVER, cfg.sipServer)
            putString(Settings.PREF_DOMAIN, cfg.sipServer)
            putString(Settings.PREF_PORT, cfg.sipPort.toString())
            putString(Settings.PREF_PROTOCOL, cfg.sipTransport.lowercase())
            putString(Settings.PREF_FROMUSER, if (cfg.sipDisplayName.isNotBlank()) cfg.sipDisplayName else "")
            putBoolean(Settings.PREF_WLAN, true)
            putBoolean(Settings.PREF_3G, true)
            putBoolean(Settings.PREF_4G, true)
            putBoolean(Settings.PREF_MWI_ENABLED, false)
        }.apply()
    }

    fun initialize() {
        Receiver.mContext = context
    }

    fun register(callback: (success: Boolean, error: String?) -> Unit = { _, _ -> }) {
        val cfg = context.config
        if (!cfg.sipEnabled || cfg.sipServer.isBlank() || cfg.sipUsername.isBlank()) {
            callback(false, "SIP not configured")
            return
        }

        try {
            writeSipConfigToPrefs()
            Receiver.mContext = context
            Log.d(TAG, "register: starting SIP engine for user=${cfg.sipUsername} server=${cfg.sipServer}")
            notifyStateChanged(SipRegistrationState.CONNECTING)

            // Schedule a timeout so the UI never stays stuck on "Connecting…" indefinitely.
            cancelRegistrationTimeout()
            mainHandler.postDelayed(registrationTimeoutRunnable, REGISTRATION_TIMEOUT_MS)

            val engine = Receiver.engine(context)
            engine.StartEngine()
            // Registration happens asynchronously; Receiver.registered() will call our stateListener
            callback(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "SIP register exception", e)
            cancelRegistrationTimeout()
            notifyStateChanged(SipRegistrationState.ERROR)
            callback(false, e.message)
        }
    }

    fun unregister() {
        cancelRegistrationTimeout()
        try {
            val engine = Receiver.mSipdroidEngine
            engine?.halt()
        } catch (e: Exception) {
            Log.e(TAG, "SIP unregister exception", e)
        }
        notifyStateChanged(SipRegistrationState.DISCONNECTED)
    }

    fun placeCall(targetNumber: String) {
        val server = context.config.sipServer
        val targetUri = if (targetNumber.startsWith("sip:")) {
            targetNumber
        } else {
            "sip:$targetNumber@$server"
        }

        try {
            writeSipConfigToPrefs()
            Receiver.mContext = context
            val engine = Receiver.engine(context)
            if (!engine.isRegistered(0)) {
                engine.StartEngine()
            }
            engine.call(targetUri, false)
        } catch (e: Exception) {
            Log.e(TAG, "SIP placeCall exception", e)
        }
    }

    fun answerCall() {
        try {
            Receiver.mSipdroidEngine?.answercall()
        } catch (e: Exception) {
            Log.e(TAG, "SIP answerCall exception", e)
        }
    }

    fun endCall() {
        try {
            Receiver.mSipdroidEngine?.ua?.hangup()
        } catch (e: Exception) {
            Log.e(TAG, "SIP endCall exception", e)
        }
    }

    fun getCurrentCallState(): Int {
        return Receiver.call_state
    }
}
