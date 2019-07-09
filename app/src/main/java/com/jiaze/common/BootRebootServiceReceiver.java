package com.jiaze.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jiaze.reboot.RebootTestService;
import com.jiaze.sim.SimTestService;

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
        Intent rebootServiceIntent = new Intent(context, RebootTestService.class);
        context.startService(rebootServiceIntent);
        Log.d(TAG, "onReceive: receiver the start RebootTestService broadcast, start the rebootTestService");
        Intent simServiceIntent = new Intent(context, SimTestService.class);
        context.startService(simServiceIntent);
        Log.d(TAG, "onReceive: receiver the start SimTestService broadcast, start the simTestService");
    }
}
