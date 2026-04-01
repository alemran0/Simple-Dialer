package com.simplemobiletools.dialer.activities

import android.os.Bundle
import com.simplemobiletools.dialer.helpers.SipManager

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Replacing with corrected method name
        if (SipManager.isVoipSupported(this)) {
            // VoIP is supported
        }
    }
}