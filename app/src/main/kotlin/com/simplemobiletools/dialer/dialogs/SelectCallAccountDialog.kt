package com.simplemobiletools.dialer.dialogs

import android.annotation.SuppressLint
import android.telecom.PhoneAccountHandle
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.databinding.DialogSelectSimBinding
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.getAvailableSIMCardLabels

/**
 * Chooser dialog that lets the user pick SIM1, SIM2, or SIP for an outgoing call.
 * sipOptionLabel is the display name shown for the SIP account option (e.g. "SIP: user@server").
 * onSelection is called with:
 *   - handle != null, isSip == false  → use the given SIM handle
 *   - handle == null, isSip == true   → use SIP
 */
@SuppressLint("MissingPermission")
class SelectCallAccountDialog(
    private val activity: BaseSimpleActivity,
    private val phoneNumber: String,
    private val sipOptionLabel: String,
    private val onSelection: (handle: PhoneAccountHandle?, isSip: Boolean) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogSelectSimBinding::inflate)

    init {
        binding.selectSimRememberHolder.visibility = android.view.View.GONE

        val sims = activity.getAvailableSIMCardLabels().sortedBy { it.id }
        sims.forEachIndexed { index, simAccount ->
            val radioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
                text = simAccount.label
                id = index
                setOnClickListener {
                    onSelection(simAccount.handle, false)
                    dialog?.dismiss()
                }
            }
            binding.selectSimRadioGroup.addView(
                radioButton,
                RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }

        // Add SIP option at the end
        val sipRadioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
            text = sipOptionLabel
            id = sims.size
            setOnClickListener {
                onSelection(null, true)
                dialog?.dismiss()
            }
        }
        binding.selectSimRadioGroup.addView(
            sipRadioButton,
            RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        activity.getAlertDialogBuilder().apply {
            activity.setupDialogStuff(binding.root, this) { alertDialog ->
                dialog = alertDialog
            }
        }
    }
}
