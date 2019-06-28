package com.jiaze.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jiaze.reboot.RebootTestService;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.common
 * Create by jz-pf
 * on 2019/6/28
 * =========================================
 */
public class BootRebootServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "BootRebootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, RebootTestService.class);
        context.startService(serviceIntent);
        Log.d(TAG, "onReceive: receiver the start RebootService broadcast, start the rebootService");
    }
}
