package com.jiaze.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.jiaze.autotestapp.R;
import com.jiaze.common.Constant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.network
 * Create by jz-pf
 * on 2019/7/16
 * =========================================
 */
public class NetworkTestService extends Service {

    private static final String TAG = "NetworkTestService";
    private static final String NETWORK_TEST_PARAM_SAVE_PATH = "NetworkTestParam";
    private static final String TEST_PARAM = "NetworkTest";
    private static final int IN_SERVICE = 0;
    private static final int OUT_OF_SERVICE = 1;
    private static final int EMERGENCY_ONLY = 2;
    private static final int POWER_OFF = 3;
    private static final int FINISHED_ONE_TIME_TEST = 4;

    private int networkTestTime = 0;
    private String serviceState = null;
    private int totalRunTimes = 0;
    private static int inServiceTime = 0;
    private static int outServiceTime = 0;
    private static int powerOffTime = 0;
    private static int emergencyTime = 0;
    private static boolean runNextTime = false;
    private static boolean isTesting = false;
    private static boolean isRegistered = false;
    private TelephonyManager telephonyManager;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;
    private String storeNetworkTestResultDir;
    private NetworkTestBinder networkTestBinder = new NetworkTestBinder();
    private MyPhoneStateListener myPhoneStateListener = new MyPhoneStateListener();
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case FINISHED_ONE_TIME_TEST:
                    runNextTime = true;
                    if (mHandler != null && getServiceStateTask != null){
                        mHandler.removeCallbacks(getServiceStateTask);
                    }
                    break;
                case IN_SERVICE:
                    inServiceTime++;
                    Log.d(TAG, "handleMessage: inServiceTime + 1: " + inServiceTime);
                    serviceState = getString(R.string.text_in_service);
                    break;
                case OUT_OF_SERVICE:
                    outServiceTime++;
                    Log.d(TAG, "handleMessage: outServiceTime + 1:" + outServiceTime);
                    serviceState = getString(R.string.text_out_of_service);
                    break;
                case EMERGENCY_ONLY:
                    emergencyTime++;
                    Log.d(TAG, "handleMessage: emergencyTime + 1:" + emergencyTime);
                    serviceState = getString(R.string.text_emergency_only);
                    break;
                case POWER_OFF:
                    powerOffTime++;
                    Log.d(TAG, "handleMessage: powerOffTime + 1:" + powerOffTime);
                    serviceState = getString(R.string.text_power_off);
                    break;
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: The Network Test Service is start");
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return networkTestBinder;
    }

    class NetworkTestBinder extends Binder{
        public void startTest(Bundle bundle){
            isTesting = true;
            networkTestTime = bundle.getInt(getString(R.string.key_network_test_time));
            storeNetworkTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "startTest: Create the storeNetworkTestResultDir success : " + storeNetworkTestResultDir);
            new NetworkTestThread().start();
        }

        public void stopTest(){
            Log.d(TAG, "stopTest: stop the Test");
            networkTestTime = 0;
        }

        public boolean isInTesting(){
            return isTesting;
        }

        public String getServiceState(){
            return myPhoneStateListener.getServiceState();
        }

        public void isRegistered(boolean isRegister){
            isRegistered = isRegister;
        }
    }

    class NetworkTestThread extends Thread{

        NetworkTestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            mWakeLock.acquire();
            isTesting = true;
            runLogical();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isTesting = false;
            showResultActivity(NetworkTestActivity.class);
            Log.d(TAG, "run: finished the test, then will show you the test result");
            resetTestValue();
        }
    }

    private void runLogical(){
        for (;networkTestTime > 0 && isTesting; networkTestTime--){
            totalRunTimes++;
            Log.d(TAG, "runLogical: totalRunTimes = " + totalRunTimes);
            runNextTime = false;
            Log.d(TAG, "runLogical: 120 seconds later will run next time");
            mHandler.postDelayed(getServiceStateTask, 120 * 1000);
            Log.d(TAG, "runLogical: is runNextTime = " + runNextTime);
            while (!runNextTime){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            switch (myPhoneStateListener.getNetworkState()){
                case ServiceState.STATE_IN_SERVICE:
                    Log.d(TAG, "runLogical: serviceState = " + myPhoneStateListener.getNetworkState());
                    mHandler.sendEmptyMessage(IN_SERVICE);
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    Log.d(TAG, "runLogical: serviceState = " + myPhoneStateListener.getNetworkState());
                    mHandler.sendEmptyMessage(OUT_OF_SERVICE);
                    break;
                case ServiceState.STATE_EMERGENCY_ONLY:
                    Log.d(TAG, "runLogical: serviceState = " + myPhoneStateListener.getNetworkState());
                    mHandler.sendEmptyMessage(EMERGENCY_ONLY);
                    break;
                case ServiceState.STATE_POWER_OFF:
                    Log.d(TAG, "runLogical: serviceState = " + myPhoneStateListener.getNetworkState());
                    mHandler.sendEmptyMessage(POWER_OFF);
                    break;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: The show test Result Broadcast isRegister = " + isRegistered);
                while (!isRegistered){
                    try {
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (isRegistered){
                        Intent intent = new Intent("com.jiaze.action.NETWORK_TEST_FINISHED");
                        intent.putExtra(getString(R.string.key_result), storeNetworkTestResultDir + "/" + "testResult");
                        sendBroadcast(intent);
                        Log.d(TAG, "run: send the network test finished broadcast");
                        break;
                    }
                }
            }
        }).start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        saveNetworkTestResult();
    }

    Runnable getServiceStateTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: get the service state : ");
            mHandler.sendEmptyMessage(FINISHED_ONE_TIME_TEST);
        }
    };

    class MyPhoneStateListener extends PhoneStateListener{

        private ServiceState serviceState;
        private String state = null;

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.d(TAG, "onServiceStateChanged: serviceState is Change, serviceState = " + serviceState.getState());
            this.serviceState = serviceState;
        }

        public String getServiceState(){
            switch (serviceState.getState()){
                case ServiceState.STATE_IN_SERVICE:
                    Log.d(TAG, "getServiceState: serviceState = "+ serviceState.getState());
                    state = getString(R.string.text_in_service);
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    Log.d(TAG, "getServiceState: serviceState = "+ serviceState.getState());
                    state = getString(R.string.text_out_of_service);
                    break;
                case ServiceState.STATE_EMERGENCY_ONLY:
                    Log.d(TAG, "getServiceState: serviceState = "+ serviceState.getState());
                    state = getString(R.string.text_emergency_only);
                    break;
                case ServiceState.STATE_POWER_OFF:
                    Log.d(TAG, "getServiceState: serviceState = "+ serviceState.getState());
                    state = getString(R.string.text_power_off);
                    break;
            }

            return state;
        }

        public int getNetworkState(){
            return serviceState.getState();
        }
    }

    private void saveNetworkTestResult(){
        Log.d(TAG, "saveNetworkTestResult: Start save the Network Test Result");
        File file = new File(storeNetworkTestResultDir + "/" + "testResult");
        Log.d(TAG, "saveNetworkTestResult: get the storeNetworkTestResultDir : " + storeNetworkTestResultDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveNetworkTestResult: =====Create the Network Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveNetworkTestResult: =====Create the Network Test Result File Failed");
                e.printStackTrace();
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_in_service_time) + inServiceTime);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_out_of_service_time) + outServiceTime);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_power_off_time) + powerOffTime);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_emergency_time) + emergencyTime);

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {
            fileWriter= new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "saveNetworkTestResult: Save the Network test Result File Failed");
        } catch (IOException e) {
            Log.d(TAG, "saveNetworkTestResult: Save the Network test Result File Failed");
            e.printStackTrace();
        }finally {
            if (bufferedWriter != null){
                try {
                    bufferedWriter.close();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void showResultActivity(Class<?> resultActivity){
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getString(R.string.key_test_result_path), storeNetworkTestResultDir + "/" + "testResult" );
        startActivity(intent);
    }

    private void resetTestValue(){
        totalRunTimes = 0;
        inServiceTime = 0;
        outServiceTime = 0;
        emergencyTime = 0;
        powerOffTime = 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()){
            mWakeLock.release();
        }
    }
}
