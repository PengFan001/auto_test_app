package com.jiaze.call;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.CallStateListener;
import android.util.Log;
import android.widget.Toast;

import com.jiaze.autotestapp.R;
import com.jiaze.common.AutoTestService;
import com.jiaze.common.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;


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
    public static final String CALL_TEST_PARAMS_SAVE_PATH = "CallTestParams";


    private static final int CALL_IDLE = 1;
    private static final int CALL_ACTIVE = 2;
    private static final int CALL_INCOMING = 3;
    private static final int CALL_WAITING = 4;
    private static final int CALL_DIALING = 5;
    private static final int CALL_REDIALING = 6;
    private static final int CALL_ONHOLD = 7;
    private static final int CALL_DISCONNECTING = 8;
    private static final int CALL_DISCONNECTED = 9;
    private static final int CONFERENCED = 10;
    private static final int MSG_ID_WAIT_TIMEOUT = 11;
    private static final int MSG_ID_DURATION_TIMEOUT = 12;

    private static final int MSG_ID_CALL_IDLE = 13;
    private static final int MSG_ID_CALL_RINGING = 14;
    private static final int MSG_ID_CALL_OFF_HOOK = 15;
    private static final int MSG_ID_CALL_DIALING = 16;
    private static final int MSG_ID_HOOK_OFF_TIMEOUT = 17;


    private TelephonyManager telephonyManager;
    private int runTimes = 0;
    private int waitTimeOutTimes = 0;
    private int durationTimeOutTimes = 0;
    private String phone;


    //拨号等待计时器
    Runnable waitTimeOutCalculate = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "==========the call wait time out, will end the call========");
            //todo 打印相关的log
            mHandler.sendEmptyMessage(MSG_ID_WAIT_TIMEOUT);
        }
    };

    //通话时长计时器
    Runnable durationTimeOutCalculate = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "==========the call duration time out, will end the call========");
            //todo 打印相关的log
            mHandler.sendEmptyMessage(MSG_ID_DURATION_TIMEOUT);
        }
    };

    //接听等待计时器
    Runnable hookOffTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: ==============20 seconds not hook off wiil be auto end call===========");
            mHandler.sendEmptyMessage(MSG_ID_HOOK_OFF_TIMEOUT);
        }
    };

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_CALL_DIALING:
                    if (isInTesting){
                        Log.d(TAG, "handleMessage: MSG_ID_CALL_DIALING : Call the Phone Success , wait been hook off");
                        successCount++;
                        if (mHandler != null && waitTimeOutCalculate != null){
                            mHandler.removeCallbacks(waitTimeOutCalculate);
                            Log.d(TAG, "handleMessage: MSG_ID_CALL_DIALING, Call the phone success, remove the waitTimeout Task");
                        }

                        Log.d(TAG, "handleMessage: MSG_ID_CALL_DIALING, 30s not hook off, the call will be auto end");
                        mHandler.postDelayed(hookOffTimeout, 30 * 1000);
                    }else {
                        Log.d(TAG, "handleMessage: MSG_ID_CALL_DIALING the coming Call is be OFF HOOK");
                        Toast.makeText(getApplicationContext(), getString(R.string.text_call_been_hook_off), Toast.LENGTH_SHORT).show();
                    }

                    break;

                case MSG_ID_CALL_OFF_HOOK:
                    if (isInTesting){
                        Log.d(TAG, "handleMessage: MSG_ID_CALL_OFF_HOOK, the call been hook off");
                        //successCount++;
                        if (mHandler != null && hookOffTimeout != null) {
                            mHandler.removeCallbacks(hookOffTimeout);
                            Log.d(TAG, "handleMessage: MSG_ID_CALL_OFF_HOOK : remove the hookOffTimeout");
                        }
                        Log.d(TAG, "======dispatchMessage: the phone is be hook off, start the calculate the duration time，SuccessTime + 1:" + successCount);
                        mHandler.postDelayed(durationTimeOutCalculate, durationTimeOutTimes * 1000);
                    }else {
//                        Log.d(TAG, "handleMessage: MSG_ID_CALL_OFF_HOOK the coming Call is be OFF HOOK");
//                        Toast.makeText(getApplicationContext(), getString(R.string.text_call_been_hook_off), Toast.LENGTH_SHORT).show();
                    }
                    break;

                case MSG_ID_CALL_IDLE:
                    runNextTime = true;
                    if (mHandler != null && waitTimeOutCalculate != null) {
                        mHandler.removeCallbacks(waitTimeOutCalculate);
                        Log.d(TAG, "handleMessage: MSG_ID_CALL_IDLE : remove the waitTimeOutCalculate");
                    }
                    if (mHandler != null && durationTimeOutCalculate != null){
                        mHandler.removeCallbacks(durationTimeOutCalculate);
                        Log.d(TAG, "handleMessage: MSG_ID_CALL_IDLE : remove the durationTimeOutCalculate");
                    }
                    Log.d(TAG, "=======dispatchMessage: the call is end or not call, we will runNextTimes = " + runNextTime);
                    break;

                case MSG_ID_CALL_RINGING:
                    Log.d(TAG, "handleMessage: Call RINGING 响铃、 第三方来电等待");
                    Toast.makeText(getApplicationContext(), getString(R.string.text_call_coming), Toast.LENGTH_SHORT).show();
                    break;

                case MSG_ID_WAIT_TIMEOUT:
                    failedCount++;
                    //todo 拨号超时后，测试失败，开始下一次测试
                    if (telephonyManager != null && !isCallStateIdle()) {
                        telephonyManager.endCall();
                    }
                    if (mHandler != null && waitTimeOutCalculate != null) {
                        mHandler.removeCallbacks(waitTimeOutCalculate);
                        Log.d(TAG, "handleMessage: MSG_ID_WAIT_TIMEOUT : remove the waitTimeOutCalculate");
                    }
                    runNextTime = true;
                    Log.d(TAG, "=======dispatchMessage: wait hook off timeout runNextTime: " + runNextTime);
                    break;

                case MSG_ID_HOOK_OFF_TIMEOUT:
                    if (telephonyManager != null && !isCallStateIdle()) {
                        telephonyManager.endCall();
                    }
                    if (mHandler != null && hookOffTimeout != null) {
                        mHandler.removeCallbacks(hookOffTimeout);
                        Log.d(TAG, "handleMessage: MSG_ID_HOOK_OFF_TIMEOUT : remove the hookOffTimeoutCalculate");
                    }
                    runNextTime = true;
                    Log.d(TAG, "=======dispatchMessage: MSG_ID_HOOK_OFF_TIMEOUT, wait hook off timeout runNextTime: " + runNextTime);
                    break;

                case MSG_ID_DURATION_TIMEOUT:
                    //todo 在通话时长达到之后，挂断电话
                    if (telephonyManager != null && !isCallStateIdle()) {
                        telephonyManager.endCall();
                    }
                    if (mHandler != null && durationTimeOutCalculate != null) {
                        mHandler.removeCallbacks(durationTimeOutCalculate);
                        Log.d(TAG, "handleMessage: MSG_ID_DURATION_TIMEOUT : remove the durationTimeOutCalculate");
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
                    mHandler.sendEmptyMessage(MSG_ID_CALL_DIALING);
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
        telephonyManager.addCallStateListener(new CallStateListener() {
            @Override
            public void onCallStateChanged(int callId, int state, String number) {
                Log.d(TAG, "onCallStateChanged: get the callId = " + callId + "  state = " + state + "  number = " + number);
                switch (state){
                    case CALL_ACTIVE:
                        Log.d(TAG, "onCallStateChanged: the call be hook off");
                        mHandler.sendEmptyMessage(MSG_ID_CALL_OFF_HOOK);
                        break;

//                    case CALL_DIALING:
//                        Log.d(TAG, "onCallStateChanged: the call is calling ");
//                        mHandler.sendEmptyMessage(MSG_ID_CALL_DIALING);
//                        break;
                }
            }
        });
//        getTestParams();
//        if (isStartTest){
//            Log.d(TAG, "onCreate: isStarttest is true, start the test");
//            new TestThread().start();
//        }else{
//            Log.d(TAG, "onCreate: isStartTest is false, need not do anythings");
//        }
    }

    @Override
    public void stopTest() {
        isInTesting = false;
    }

    @Override
    protected void runTestLogic() {
        testTimes = runTimes;
        for (; runTimes > 0 && isInTesting; runTimes--) {
            Log.d(TAG, "runTestLogic: wait 3 second to call the last time");
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            totalRunTimes++;
            Log.d(TAG, "runTestLogic: Call test totalRunTimes = " + totalRunTimes);
            startCallPhone(phone);
            mHandler.postDelayed(waitTimeOutCalculate, waitTimeOutTimes * 1000);
            Log.d(TAG, "runTestLogic: Call the Phone");
            runNextTime = false;
            Log.d(TAG, "runTestLogic: start calculate the wait time");
            while (!runNextTime) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            storeTestResult(CALL_TEST_RESULT_FILENAME);
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

        boolean isCombinationTest = bundle.getBoolean(getString(R.string.key_is_combination_test), false);
        if (!isCombinationTest){
            storeTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
        }
        Log.d(TAG, "initTestParams: get the testResult dir : " + storeTestResultDir);
        runTimes = bundle.getInt(getString(R.string.key_test_times));
        waitTimeOutTimes = bundle.getInt(getString(R.string.key_wait_time));
        durationTimeOutTimes = bundle.getInt(getString(R.string.key_duration_time));
        phone = bundle.getString(getString(R.string.key_phone));
        if (!PhoneNumberUtils.isGlobalPhoneNumber(phone)){
            return -1;
        }
        return 0;
    }

    @Override
    protected Class<?> getResultActivity() {
        return CallTestActivity.class;
    }

    @Override
    protected void saveTmpTestResult() {
        Properties properties = new Properties();
        String fileDir = getFilesDir().getAbsolutePath() + "/" + CALL_TEST_PARAMS_SAVE_PATH;
        File file = new File(fileDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveTmpTestResult: Create the tmp test Result file success");
            }catch (IOException e){
                Log.d(TAG, "saveTmpTestResult: Create the tmp test Result file Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(file);
            properties.setProperty(getString(R.string.key_test_times), String.valueOf(runTimes));
            properties.setProperty(getString(R.string.key_phone), phone);
            properties.setProperty(getString(R.string.key_wait_time), String.valueOf(waitTimeOutTimes));
            properties.setProperty(getString(R.string.key_duration_time), String.valueOf(durationTimeOutTimes));
            properties.setProperty(getString(R.string.key_is_testing), String.valueOf(isInTesting));
            properties.setProperty(getString(R.string.key_is_start_test), String.valueOf(isStartTest));
            properties.setProperty(getString(R.string.key_test_result_path), storeTestResultDir);
            properties.store(outputStream, "save the call test tmp test result");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTmpTestResult: open call test param file failed ");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "saveTmpTestResult: store the call properties failed");
            e.printStackTrace();
        }

        Log.d(TAG, "saveTmpTestResult: Succeed save the tmp test result of PS test");
    }

    @Override
    protected void getTestParams() {
        Properties properties = Constant.loadTestParameter(this, CALL_TEST_PARAMS_SAVE_PATH);
        runTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_test_times), "0"));
        storeTestResultDir = properties.getProperty(getString(R.string.key_test_result_path), null);
        isStartTest = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_start_test), "false"));
        waitTimeOutTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_wait_time), "0"));
        durationTimeOutTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_duration_time), "0"));
        isInTesting = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_testing), "false"));
        phone = properties.getProperty(getString(R.string.key_phone));
        Log.d(TAG, "getTestParams: testTimes = " + runTimes + "   isStartTest = " + isStartTest + "   isInTesting = " + isInTesting + "    storeTestResultDir = " + storeTestResultDir);
        Log.d(TAG, "getTestParams: phone = " + phone + "   waitTimeOutTimes = " + waitTimeOutTimes + "    durationTimeOutTimes = " + durationTimeOutTimes);
    }
}
