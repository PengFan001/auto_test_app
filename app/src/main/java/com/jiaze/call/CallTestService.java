package com.jiaze.call;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.drm.DrmStore;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.jiaze.autotestapp.R;
import com.jiaze.common.AutoTestActivity;
import com.jiaze.common.AutoTestService;
import com.jiaze.common.Constant;


/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.call
 * Create by jz-pf
 * on 2019/6/20
 * =========================================
 */
public class CallTestService extends AutoTestService {

    private static final String TEST_PARAM = "CallTest";
    public static final String TAG = "CallTestService";

    public static final int MSG_ID_CALL_OFF_HOOK = 1;
    public static final int MSG_ID_CALL_IDLE = 2;
    public static final int MSG_ID_CALL_RINGING = 3;
    public static final int MSG_ID_WAIT_TIMEOUT = 4;
    public static final int MSG_ID_DURATION_TIMEOUT = 5;


    private TelephonyManager telephonyManager;
    private int runTimes = 0;
    private int waitTimeOutTimes = 0;
    private int durationTimeOutTimes = 0;
    private String phone;

    //通话等待计时器
    Runnable waitTimeOutCalculate = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "==========calculate the call wait Time========");
            //todo 打印相关的log
            mHandler.sendEmptyMessage(MSG_ID_WAIT_TIMEOUT);
        }
    };

    //通话时长计时器
    Runnable durationTimeOutCalculate = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "==========calculate the call duration Time========");
            //todo 打印相关的log
            mHandler.sendEmptyMessage(MSG_ID_DURATION_TIMEOUT);
        }
    };

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_CALL_OFF_HOOK:
                    successCount++;
                    if (mHandler != null && waitTimeOutCalculate != null) {
                        mHandler.removeCallbacks(waitTimeOutCalculate);
                    }
                    Log.d(TAG, "======dispatchMessage: 呼叫被接听，开始通话时长计时，成功次数SuccessTime加一:" + successCount);
                    mHandler.postDelayed(durationTimeOutCalculate, durationTimeOutTimes * 1000);
                    break;
                case MSG_ID_CALL_IDLE:
                    runNextTime = true;
                    if (mHandler != null && waitTimeOutCalculate != null) {
                        mHandler.removeCallbacks(waitTimeOutCalculate);
                    }
                    Log.d(TAG, "=======dispatchMessage: 空闲状态，没有通话活动" + runNextTime);
                    break;
                case MSG_ID_CALL_RINGING:
                    Log.d(TAG, "handleMessage: Call RINGing 响铃、 第三方来电等待");
                    break;
                case MSG_ID_WAIT_TIMEOUT:
                    failedCount++;
                    //todo 在等待接听超时后挂断电话
                    if (telephonyManager != null && !isCallStateIdle()) {
                        //telephonyManager.endCall();
                    }
                    if (mHandler != null && waitTimeOutCalculate != null) {
                        mHandler.removeCallbacks(waitTimeOutCalculate);
                    }
                    runNextTime = true;
                    Log.d(TAG, "=======dispatchMessage: 等待接听超时, 开始下一次呼叫 runNextTime: " + runNextTime);
                    break;
                case MSG_ID_DURATION_TIMEOUT:
                    //todo 在通话时长达到之后，挂断电话
                    if (telephonyManager != null && !isCallStateIdle()) {
                        //telephonyManager.endCall();
                    }
                    if (mHandler != null && durationTimeOutCalculate != null) {
                        mHandler.removeCallbacks(durationTimeOutCalculate);
                    }
                    Log.d(TAG, "=======dispatchMessage: 通话时间超时, 开始下一次呼叫");
                    break;
            }
            return false;
        }
    });

    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    mHandler.sendEmptyMessage(MSG_ID_CALL_IDLE);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mHandler.sendEmptyMessage(MSG_ID_CALL_OFF_HOOK);
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mHandler.sendEmptyMessage(MSG_ID_CALL_RINGING);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void stopTest() {
        isInTesting = false;
    }

    @Override
    protected void runTestLogic() {
        testTimes = runTimes;
        for (; runTimes > 0 && isInTesting; runTimes--) {
            totalRunTimes++;
            startCallPhone(phone);
            runNextTime = false;
            mHandler.postDelayed(waitTimeOutCalculate, waitTimeOutTimes * 1000);
            while (!runNextTime) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            storeTestResult();
            Log.d(TAG, "runTestLogic: =========finished the one test======");
        }
    }

    private void startCallPhone(String phone) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean isCallStateIdle(){
        if (TelephonyManager.CALL_STATE_IDLE == telephonyManager.getCallState()){
            return true;
        }else {
            return false;
        }
    }

    @Override
    protected int initTestParams(Bundle bundle) {
        storeTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
        Log.d(TAG, "initTestParams: get the testResult dir : " + storeTestResultDir);
        runTimes = bundle.getInt(getString(R.string.key_test_times));
        waitTimeOutTimes = bundle.getInt(getString(R.string.key_wait_time));
        durationTimeOutTimes = bundle.getInt(getString(R.string.key_duration_time));
        phone = bundle.getString(getString(R.string.key_phone));
        if (TextUtils.isEmpty(phone)){
            Toast.makeText(this, R.string.text_phone_null, Toast.LENGTH_SHORT).show();
            return -1;
        }
        return 0;
    }

    @Override
    protected Class<?> getResultActivity() {
        return CallTestActivity.class;
    }
}
