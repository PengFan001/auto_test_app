package com.jiaze.autotestapp;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.os.Bundle;

import com.jiaze.airmode.AirModeTestActivity;
import com.jiaze.call.CallTestActivity;
import com.jiaze.common.Constant;
import com.jiaze.network.NetworkTestActivity;
import com.jiaze.ps.PsTestActivity;
import com.jiaze.reboot.RebootTestActivity;
import com.jiaze.sim.SimTestActivity;
import com.jiaze.sms.SmsTestActivity;

public class MainActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.auto_test_main_activity);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Intent intent = new Intent();
        if (preference.getKey().equals(Constant.PRE_KEY_BOOT_OR_SHUTDOWN)){
            intent.setClass(this, RebootTestActivity.class);
        }else if (preference.getKey().equals(Constant.PRE_KEY_SIM)){
            intent.setClass(this, SimTestActivity.class);
        }else if (preference.getKey().equals(Constant.PRE_KEY_NET)){
            intent.setClass(this, NetworkTestActivity.class);
        }else if (preference.getKey().equals(Constant.PRE_KEY_CALL)){
            intent.setClass(this, CallTestActivity.class);
        }else if(preference.getKey().equals(Constant.PRE_KEY_AIR_MODE)){
            intent.setClass(this, AirModeTestActivity.class);
        }else if(preference.getKey().equals(Constant.PRE_KEY_SMS)){
            intent.setClass(this, SmsTestActivity.class);
        }else if(preference.getKey().equals(Constant.PRE_KEY_PPS)){
            intent.setClass(this, PsTestActivity.class);
        }
        startActivity(intent);
        return true;
    }
}
