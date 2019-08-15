package com.jiaze.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jiaze.airmode.AirModeTestService;
import com.jiaze.call.CallTestService;
import com.jiaze.combination.CombinationTestService;
import com.jiaze.network.NetworkTestService;
import com.jiaze.ps.PsTestService;
import com.jiaze.reboot.ModuleRebootService;
import com.jiaze.reboot.RebootTestService;
import com.jiaze.sim.SimTestService;
import com.jiaze.sms.SmsTestService;

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
        Intent networkServiceIntent = new Intent(context, NetworkTestService.class);
        context.startService(networkServiceIntent);
        Log.d(TAG, "onReceive: receiver the start NetworkService broadcast, start the networkTestService");
        Intent combinationServiceIntent = new Intent(context, CombinationTestService.class);
        context.startService(combinationServiceIntent);
        Log.d(TAG, "onReceive: receiver the start CombinationTestService broadcast, start the CombinationTestService");
    }
}
