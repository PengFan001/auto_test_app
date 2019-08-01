package com.jiaze.ps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
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
 * the Package:com.jiaze.ps
 * Create by jz-pf
 * on 2019/7/23
 * =========================================
 */
public class PsTestService extends Service {

    private static final String TAG = "PsTestService";
    private static final String PS_TEST_PARAM_SAVE_PATH = "PsTestParams";
    private static final String TEST_PARAM = "PsTest";

    private static final int DATA_CONNECTED = 1;
    private static final int DATA_CONNECTING = 2;
    private static final int DATA_DISCONNECTED = 3;
    private static final int DATA_SUSPEND = 4;
    private static final int CONNECTING_TIMEOUT = 5;
    
    private boolean isInTesting = false;
    private int psTestTimes = 0;
    private int totalRunTimes = 0;
    private int successTime = 0;
    private int failTime = 0;
    private int networkState = 0; //默认设置为未连接
    private boolean runNextTime = false;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connectivityManager;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;
    private String psState;
    private String storePsTestResultDir;
    private PsTestBinder psTestBinder = new PsTestBinder();

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case CONNECTING_TIMEOUT:
                    failTime++;
                    if (mHandler != null &&connectingTimeout != null){
                        mHandler.removeCallbacks(connectingTimeout);
                    }
                    Log.d(TAG, "handleMessage: connecting time out, switch failed. failTime + 1 = " + failTime);
                    runNextTime = true;
                    break;
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: PS Test Service is Start");
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return psTestBinder;
    }

    /**有关PSTestService所提供的服务**/
    class PsTestBinder extends Binder{
        public void startTest(Bundle bundle){
            psTestTimes = bundle.getInt(getString(R.string.key_ps_test_time), 1);
            Log.d(TAG, "startTest: psTestTimes = " + psTestTimes);
            storePsTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "startTest: Create the storePsTestResultDir success : " + storePsTestResultDir);
            new PsTestThread().start();
        }

        public void stopTest(){
            isInTesting = false;
        }

        public boolean isInTesting(){
            return isInTesting;
        }

        public String getPsState(){
            int state = telephonyManager.getDataState();
            switch (state){
                case TelephonyManager.DATA_CONNECTED:
                    psState = getString(R.string.text_net_connected);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
                case TelephonyManager.DATA_CONNECTING:
                    psState = getString(R.string.text_net_connecting);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
                case TelephonyManager.DATA_DISCONNECTED:
                    psState = getString(R.string.text_net_disconnect);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
                case TelephonyManager.DATA_SUSPENDED:
                    psState = getString(R.string.text_net_suspend);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
            }
            return psState;
        }
    }

    /**监听设备移动网络状态**/
    PhoneStateListener myPhoneStateListener = new PhoneStateListener(){
        @Override
        public void onDataConnectionStateChanged(int state) {
            super.onDataConnectionStateChanged(state);
            switch (state){
                case TelephonyManager.DATA_CONNECTED:
                    psState = getString(R.string.text_net_connected);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
                case TelephonyManager.DATA_CONNECTING:
                    psState = getString(R.string.text_net_connecting);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
                case TelephonyManager.DATA_DISCONNECTED:
                    psState = getString(R.string.text_net_disconnect);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
                case TelephonyManager.DATA_SUSPENDED:
                    psState = getString(R.string.text_net_suspend);
                    networkState = state;
                    Log.d(TAG, "onDataConnectionStateChanged: state = " + state + "  psState = " + psState);
                    break;
            }
        }
    };

    class PsTestThread extends Thread{
        PsTestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "run: start ps Test");
            mWakeLock.acquire();
            isInTesting = true;
            runLogical();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isInTesting = false;
            resetTestValue();
            showResultActivity(PsTestActivity.class);
            Log.d(TAG, "run: finished the test, then will show you the Ps Test Result");
        }
    }

    private void runLogical(){
        for (; psTestTimes > 0 && isInTesting; psTestTimes--){
            totalRunTimes++;
            runNextTime = false;
            Log.d(TAG, "runLogical: totalRunTimes = " + totalRunTimes);
            int currentNetworkState = telephonyManager.getDataState();
            if (currentNetworkState == TelephonyManager.DATA_CONNECTED){
                // if current network state is connect, we close it
                Log.d(TAG, "runLogical: current network state is connect, we close it");
                connectivityManager.setMobileDataEnabled(false);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(!runNextTime){
                    int state = telephonyManager.getDataState();
                    if (state == TelephonyManager.DATA_DISCONNECTED){
                        successTime++;
                        Log.d(TAG, "runLogical: close the data success, successTime+1 = " + successTime);
                        runNextTime = true;
                    }else {
                        failTime++;
                        Log.d(TAG, "runLogical: close the data failed, failTime+1 = " + failTime);
                        Log.d(TAG, "runLogical: get the state = " + state);
                        runNextTime = true;
                    }
                }
                
            }else if (currentNetworkState == TelephonyManager.DATA_DISCONNECTED){
                // if current network state is disconnect, we open it
                Log.d(TAG, "runLogical: current network state is disconnect, we open it");
                connectivityManager.setMobileDataEnabled(true);
                mHandler.postDelayed(connectingTimeout, 10 * 1000);
                while (!runNextTime){
                    int state = telephonyManager.getDataState();
                    if (state == TelephonyManager.DATA_CONNECTING){
                        Log.d(TAG, "runLogical: wait the data connecting =================");
                    }else if (state == TelephonyManager.DATA_CONNECTED){
                        successTime++;
                        Log.d(TAG, "runLogical: open the data success, successTime+1 = " + successTime);
                        if (mHandler != null && connectingTimeout != null){
                            mHandler.removeCallbacks(connectingTimeout);
                        }
                        runNextTime = true;
                    }else {
                        failTime++;
                        Log.d(TAG, "runLogical: open the data failed, failedTime+1 = " + failTime);
                        Log.d(TAG, "runLogical: state = " + state);
                        if (mHandler != null && connectingTimeout != null){
                            mHandler.removeCallbacks(connectingTimeout);
                        }
                        runNextTime = true;
                    }
                }
            }else if (currentNetworkState == TelephonyManager.DATA_CONNECTING){
                // if current network state is connecting, we wait sometimes, the test
                mHandler.postDelayed(connectingTimeout, 10 * 1000);
                while (!runNextTime){
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "runLogical: data connecting time out, runNextTime");
            }else if (currentNetworkState == TelephonyManager.DATA_SUSPENDED){
                // if current network state is suspend, we close it
                Log.d(TAG, "runLogical: current network state is connect, we close it");
                connectivityManager.setMobileDataEnabled(false);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(!runNextTime){
                    int state = telephonyManager.getDataState();
                    if (state == TelephonyManager.DATA_DISCONNECTED){
                        successTime++;
                        Log.d(TAG, "runLogical: close the data success, successTime+1 = " + successTime);
                        if (mHandler != null && connectingTimeout != null){
                            mHandler.removeCallbacks(connectingTimeout);
                        }
                        runNextTime = true;
                    }else {
                        failTime++;
                        Log.d(TAG, "runLogical: close the data failed, failTime+1 = " + failTime);
                        Log.d(TAG, "runLogical: get the state = " + state);
                        if (mHandler != null && connectingTimeout != null){
                            mHandler.removeCallbacks(connectingTimeout);
                        }
                        runNextTime = true;
                    }
                }
            }
        }

        savePsTestResult();
    }

    Runnable connectingTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: connecting timeout ");
            mHandler.sendEmptyMessage(CONNECTING_TIMEOUT);
        }
    };
    
    private void savePsTestResult(){
        Log.d(TAG, "savePsTestResult: Start save the PsTestResult");
        File file = new File(storePsTestResultDir + "/" + "testResult");
        Log.d(TAG, "savePsTestResult: get the storePsTestResultDir = " + storePsTestResultDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "savePsTestResult: =====Created the Ps Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "savePsTestResult: =====Created the Ps Test Result File Failed");
                e.printStackTrace();
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_switch_succeed_time) + successTime);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_switch_failed_time) + failTime);

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "savePsTestResult: Save the PsTestResult Succeed");
        } catch (IOException e) {
            Log.d(TAG, "savePsTestResult: Save the PsTestResult Failed");
            e.printStackTrace();
        }finally {
            if (bufferedWriter != null){
                try {
                    bufferedWriter.close();
                    fileWriter.close();
                } catch (IOException e) {
                    Log.d(TAG, "savePsTestResult: close the bufferWriter or fileWriter failed");
                    e.printStackTrace();
                }
            }
        }
    }

    private void resetTestValue(){
        psTestTimes = 0;
        successTime = 0;
        failTime = 0;
        totalRunTimes = 0;
    }

    private void showResultActivity(Class<?> resultActivity){
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getString(R.string.key_test_result_path), storePsTestResultDir + "/" + "testResult" );
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()){
            mWakeLock.release();
        }
    }

}
