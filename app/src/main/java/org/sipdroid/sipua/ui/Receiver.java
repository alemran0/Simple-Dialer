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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;

import org.sipdroid.sipua.SipdroidEngine;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.Connection;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified Receiver stub - bridges sipdroid state changes to the app.
 * This replaces the full sipdroid Receiver which depended on sipdroid's own UI.
 */
public class Receiver extends BroadcastReceiver {

    public static final int REGISTER_NOTIFICATION = 1;
    public static final int REGISTER_NOTIFICATION_0 = 1;
    public static final int MWI_NOTIFICATION = 10;
    public static final int CALL_NOTIFICATION = 11;
    public static final int AUTO_ANSWER_NOTIFICATION = 12;
    public static final int MISSED_CALL_NOTIFICATION = 13;

    public static int docked = -1;
    public static int headset = -1;
    public static int bluetooth = -1;
    public static int last_bluetooth_state = -1;
    public static int last2_bluetooth_state = -1;
    public static long last_bluetooth_time = 0;
    public static SipdroidEngine mSipdroidEngine;
    public static Context mContext;
    public static Service sContext;
    public static Call ccCall;
    public static Connection ccConn;
    public static int call_state;
    public static int call_end_reason = -1;
    public static String pstn_state;
    public static long pstn_time;
    public static String MWI_account;
    public static long expire_time;
    public static boolean on_wlan = true;
    public static List<Long> changeTimestamps = new ArrayList<>();

    /** Callback interface for state changes - implemented by SipManagerWrapper */
    public interface StateListener {
        void onCallStateChanged(int state, String caller);
        void onRegistered();
        void onUnregistered();
        void onRegistrationFailed(String reason);
    }

    public static StateListener stateListener;

    public static synchronized SipdroidEngine engine(Context context) {
        if (mSipdroidEngine == null) {
            mContext = context.getApplicationContext();
            mSipdroidEngine = new SipdroidEngine();
        }
        return mSipdroidEngine;
    }

    public static void onState(int state, String caller) {
        call_state = state;
        if (stateListener != null) {
            stateListener.onCallStateChanged(state, caller);
        }
    }

    public static void registered() {
        if (stateListener != null) {
            stateListener.onRegistered();
        }
    }

    public static void failed(String reason) {
        if (stateListener != null) {
            stateListener.onRegistrationFailed(reason);
        }
    }

    public static void stopRingtone() {
        // No-op in this stub
    }

    public static void onText(int type, String text, int mInCallResId, long base) {
        // No-op in this stub - notification handling done by SipManagerWrapper
    }

    public static void alarm(int renew_time, Class<?> cls) {
        if (mContext == null) return;
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(mContext, cls);
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (renew_time > 0) {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + renew_time * 1000L, pi);
        } else {
            am.cancel(pi);
        }
    }

    public static synchronized void reRegister(int renew_time) {
        if (mSipdroidEngine != null) {
            alarm(renew_time, LoopAlarm.class);
        }
    }

    public static int getRecentNetworkTypeChangesCount() {
        return 0;
    }

    public static boolean isFast(int i) {
        return true;
    }

    public static void url(String opt) {
        // No-op in this stub
    }

    public static void moveTop() {
        // No-op in this stub
    }

    public static void progress() {
        // No-op in this stub
    }

    public static int speakermode() {
        return AudioManager.MODE_IN_CALL;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This stub does not handle broadcast events
        // Network change handling is not needed for basic SIP functionality
    }
}
