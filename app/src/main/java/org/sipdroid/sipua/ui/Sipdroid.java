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

/**
 * Minimal stub for Sipdroid class used by core sipdroid code.
 */
public class Sipdroid {

    public static final boolean release = true;
    public static final boolean market = false;

    public static String getVersion() {
        return "3.1.8";
    }

    public static String getVersion(Context context) {
        return getVersion();
    }

    public static boolean on(Context context) {
        return true;
    }

    public static void on(Context context, boolean on) {
        // No-op in stub
    }
}
