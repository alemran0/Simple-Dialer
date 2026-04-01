/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 *
 * This file is part of Sipdroid (http://www.sipdroid.org)
 *
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.sipdroid.sipua.ui;

import android.content.Context;
import android.preference.PreferenceManager;

import org.zoolu.sip.provider.SipStack;

/**
 * Settings constants and helpers for sipdroid.
 * This is a stub that provides the constants sipdroid core code expects.
 * Actual values are stored in the app's own Config/SharedPreferences.
 */
@SuppressWarnings("deprecation")
public class Settings {

    public static final String PREF_USERNAME = "username";
    public static final String PREF_PASSWORD = "password";
    public static final String PREF_SERVER = "server";
    public static final String PREF_DOMAIN = "domain";
    public static final String PREF_FROMUSER = "fromuser";
    public static final String PREF_PORT = "port";
    public static final String PREF_PROTOCOL = "protocol";
    public static final String PREF_WLAN = "wlan";
    public static final String PREF_3G = "3g";
    public static final String PREF_4G = "4g";
    public static final String PREF_NOTRAIN = "notrain";
    public static final String PREF_VPN = "vpn";
    public static final String PREF_PREF = "pref";
    public static final String PREF_AUTO_ON = "auto_on";
    public static final String PREF_AUTO_ONDEMAND = "auto_on_demand";
    public static final String PREF_AUTO_HEADSET = "auto_headset";
    public static final String PREF_MWI_ENABLED = "MWI_enabled";
    public static final String PREF_NODATA = "nodata";
    public static final String PREF_SIPRINGTONE = "sipringtone";
    public static final String PREF_SEARCH = "search";
    public static final String PREF_EXCLUDEPAT = "excludepat";
    public static final String PREF_EARGAIN = "eargain";
    public static final String PREF_MICGAIN = "micgain";
    public static final String PREF_HEARGAIN = "heargain";
    public static final String PREF_HMICGAIN = "hmicgain";
    public static final String PREF_STUN = "stun";
    public static final String PREF_STUN_SERVER = "stun_server";
    public static final String PREF_STUN_SERVER_PORT = "stun_server_port";
    public static final String PREF_MMTEL = "mmtel";
    public static final String PREF_MMTEL_QVALUE = "mmtel_qvalue";
    public static final String PREF_CALLRECORD = "callrecord";
    public static final String PREF_PAR = "par";
    public static final String PREF_IMPROVE = "improve";
    public static final String PREF_POSURL = "posurl";
    public static final String PREF_CALLBACK = "callback";
    public static final String PREF_CALLTHRU = "callthru";
    public static final String PREF_CALLTHRU2 = "callthru2";
    public static final String PREF_CODECS = "codecs_new";
    public static final String PREF_DNS = "dns";
    public static final String PREF_MESSAGE = "vmessage";
    public static final String PREF_BLUETOOTH = "bluetooth";
    public static final String PREF_KEEPON = "keepon";
    public static final String PREF_ACCOUNT = "account";
    public static final String PREF_SETMODE = "setmode";
    public static final String PREF_OLDVIBRATE = "oldvibrate";
    public static final String PREF_OLDVIBRATE2 = "oldvibrate2";
    public static final String PREF_OLDRING = "oldring";
    public static final String PREF_OLDVALID = "oldvalid";
    public static final String PREF_COMPRESSION = "compression";
    public static final String PREF_ON = "on";
    public static final String PREF_NODEFAULT = "nodefault";
    public static final String PREF_NOPORT = "noport";
    public static final String PREF_PREFIX = "prefix";
    public static final String PREF_WIFI_DISABLED = "wifi_disabled";
    public static final String PREF_ON_VPN = "on_vpn";
    public static final String PREF_AUTO_DEMAND = "auto_demand";
    public static final String PREF_OLDPOLICY = "oldpolicy";
    public static final String PREF_PSTN = "pref";
    public static final String PREF_SIP = "pref";
    public static final String PREF_SIPONLY = "pref";
    public static final String PREF_ASK = "pref";

    public static final String DEFAULT_USERNAME = "";
    public static final String DEFAULT_PASSWORD = "";
    public static final String DEFAULT_SERVER = "pbxes.org";
    public static final String DEFAULT_DOMAIN = "";
    public static final String DEFAULT_FROMUSER = "";
    public static final String DEFAULT_PORT = "" + SipStack.default_port;
    public static final String DEFAULT_PROTOCOL = "udp";
    public static final boolean DEFAULT_WLAN = true;
    public static final boolean DEFAULT_3G = false;
    public static final boolean DEFAULT_4G = false;
    public static final boolean DEFAULT_NOTRAIN = false;
    public static final boolean DEFAULT_VPN = false;
    public static final boolean DEFAULT_AUTO_ON = false;
    public static final boolean DEFAULT_AUTO_ONDEMAND = false;
    public static final boolean DEFAULT_AUTO_HEADSET = false;
    public static final boolean DEFAULT_MWI_ENABLED = true;
    public static final boolean DEFAULT_REGISTRATION = true;
    public static final boolean DEFAULT_NODATA = false;
    public static final String DEFAULT_SIPRINGTONE = "";
    public static final String DEFAULT_SEARCH = "";
    public static final String DEFAULT_EXCLUDEPAT = "";
    public static final float DEFAULT_EARGAIN = 0.25f;
    public static final float DEFAULT_MICGAIN = 0.25f;
    public static final float DEFAULT_HEARGAIN = 0.25f;
    public static final float DEFAULT_HMICGAIN = 1.0f;
    public static final boolean DEFAULT_STUN = false;
    public static final String DEFAULT_STUN_SERVER = "stun.counterpath.com";
    public static final String DEFAULT_STUN_SERVER_PORT = "3478";
    public static final boolean DEFAULT_MMTEL = false;
    public static final String DEFAULT_MMTEL_QVALUE = "1.00";
    public static final boolean DEFAULT_CALLRECORD = false;
    public static final boolean DEFAULT_PAR = false;
    public static final boolean DEFAULT_IMPROVE = false;
    public static final String DEFAULT_POSURL = "";
    public static final boolean DEFAULT_CALLBACK = false;
    public static final boolean DEFAULT_CALLTHRU = false;
    public static final String DEFAULT_CALLTHRU2 = "";
    public static final String DEFAULT_CODECS = null;
    public static final String DEFAULT_DNS = "";
    public static final boolean DEFAULT_MESSAGE = false;
    public static final boolean DEFAULT_BLUETOOTH = false;
    public static final boolean DEFAULT_KEEPON = false;
    public static final int DEFAULT_ACCOUNT = 0;
    public static final boolean DEFAULT_SETMODE = false;
    public static final boolean DEFAULT_OLDVALID = false;
    public static final int DEFAULT_OLDVIBRATE = 0;
    public static final int DEFAULT_OLDVIBRATE2 = 0;
    public static final int DEFAULT_OLDPOLICY = 0;
    public static final int DEFAULT_OLDRING = 0;
    public static final boolean DEFAULT_AUTO_DEMAND = false;
    public static final boolean DEFAULT_WIFI_DISABLED = false;
    public static final boolean DEFAULT_ON_VPN = false;
    public static final boolean DEFAULT_NODEFAULT = false;
    public static final boolean DEFAULT_NOPORT = false;
    public static final boolean DEFAULT_ON = false;
    public static final String DEFAULT_PREFIX = "";
    public static final String DEFAULT_COMPRESSION = null;
    public static final boolean DEFAULT_NOTIFY = false;
    public static final String DEFAULT_PREF = "SIP";

    public static float getEarGain() {
        if (Receiver.mContext == null) return DEFAULT_EARGAIN;
        return PreferenceManager.getDefaultSharedPreferences(Receiver.mContext)
                .getFloat(PREF_EARGAIN, DEFAULT_EARGAIN);
    }

    public static float getMicGain() {
        if (Receiver.mContext == null) return DEFAULT_MICGAIN;
        return PreferenceManager.getDefaultSharedPreferences(Receiver.mContext)
                .getFloat(PREF_MICGAIN, DEFAULT_MICGAIN);
    }

    public static float getHeadsetEarGain() {
        if (Receiver.mContext == null) return DEFAULT_HEARGAIN;
        return PreferenceManager.getDefaultSharedPreferences(Receiver.mContext)
                .getFloat(PREF_HEARGAIN, DEFAULT_HEARGAIN);
    }

    public static float getHeadsetMicGain() {
        if (Receiver.mContext == null) return DEFAULT_HMICGAIN;
        return PreferenceManager.getDefaultSharedPreferences(Receiver.mContext)
                .getFloat(PREF_HMICGAIN, DEFAULT_HMICGAIN);
    }
}
