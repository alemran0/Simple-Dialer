package com.simplemobiletools.dialer.helpers

import com.simplemobiletools.commons.helpers.TAB_CALL_HISTORY
import com.simplemobiletools.commons.helpers.TAB_CONTACTS
import com.simplemobiletools.commons.helpers.TAB_FAVORITES

// shared prefs
const val SPEED_DIAL = "speed_dial"
const val REMEMBER_SIM_PREFIX = "remember_sim_"
const val GROUP_SUBSEQUENT_CALLS = "group_subsequent_calls"
const val OPEN_DIAL_PAD_AT_LAUNCH = "open_dial_pad_at_launch"
const val DISABLE_PROXIMITY_SENSOR = "disable_proximity_sensor"
const val DISABLE_SWIPE_TO_ANSWER = "disable_swipe_to_answer"
const val SHOW_TABS = "show_tabs"
const val FAVORITES_CONTACTS_ORDER = "favorites_contacts_order"
const val FAVORITES_CUSTOM_ORDER_SELECTED = "favorites_custom_order_selected"
const val WAS_OVERLAY_SNACKBAR_CONFIRMED = "was_overlay_snackbar_confirmed"
const val DIALPAD_VIBRATION = "dialpad_vibration"
const val DIALPAD_BEEPS = "dialpad_beeps"
const val HIDE_DIALPAD_NUMBERS = "hide_dialpad_numbers"
const val ALWAYS_SHOW_FULLSCREEN = "always_show_fullscreen"

// SIP shared prefs
const val SIP_ENABLED = "sip_enabled"
const val SIP_SERVER = "sip_server"
const val SIP_USERNAME = "sip_username"
const val SIP_PASSWORD = "sip_password"
const val SIP_DISPLAY_NAME = "sip_display_name"
const val SIP_PORT = "sip_port"
const val SIP_TRANSPORT = "sip_transport"
const val SIP_BACKGROUND_REGISTRATION = "sip_background_registration"

// SIP notification
const val SIP_REGISTRATION_NOTIFICATION_ID = 3
const val SIP_REGISTRATION_CHANNEL_ID = "sip_registration_channel"
const val SIP_CALL_NOTIFICATION_ID = 4
const val SIP_CALL_CHANNEL_ID = "sip_call_channel"

// SIP broadcast actions
private const val SIP_PATH = "com.simplemobiletools.dialer.sip."
const val SIP_INCOMING_CALL_ACTION = SIP_PATH + "incoming_call"

const val ALL_TABS_MASK = TAB_CONTACTS or TAB_FAVORITES or TAB_CALL_HISTORY

val tabsList = arrayListOf(TAB_CONTACTS, TAB_FAVORITES, TAB_CALL_HISTORY)

private const val PATH = "com.simplemobiletools.dialer.action."
const val ACCEPT_CALL = PATH + "accept_call"
const val DECLINE_CALL = PATH + "decline_call"

const val DIALPAD_TONE_LENGTH_MS = 150L // The length of DTMF tones in milliseconds

const val MIN_RECENTS_THRESHOLD = 30
