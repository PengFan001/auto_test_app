package com.jiaze.common;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.jiaze.autotestapp.R;
import com.jiaze.call.CallTestActivity;
import com.jiaze.callback.NormalTestCallback;
import com.jiaze.sms.SmsTestActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.common
 * Create by jz-pf
 * on 2019/6/19
 * =========================================
 */
public abstract class AutoTestService extends Service {

    private static final String TAG = "AutoTestService";
    private static final int COMBINATION_ONE_TEST_FINISHED = 7;
    private static final int WIFI_DISCONNECTED = 8;
    private static final int FTP_SERVER_CONNECT_FAILED = 9;
    protected static final String CALL_TEST_RESULT_FILENAME = "callTestResult";
    protected static final String SMS_TEST_RESULT_FILENAME = "smsTestResult";
    protected int testTimes = 0;
    protected int totalRunTimes = 0;
    protected int successCount = 0;
    protected int failedCount = 0;
    protected String storeTestResultDir;

    protected boolean runNextTime = false;
    protected static boolean isInTesting = false;
    protected boolean isStartTest = false;

    protected PowerManager powerManager;
    protected PowerManager.WakeLock mWakeLock;
    private NormalTestCallback callback;

    public abstract void stopTest();
    protected abstract void runTestLogic();
    protected abstract int initTestParams(Bundle bundle);
    protected abstract Class<?> getResultActivity();
    protected abstract void saveTmpTestResult();
    protected abstract void getTestParams();

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case COMBINATION_ONE_TEST_FINISHED:
                    callback.testResultCallback(true, successCount, failedCount);
                    Log.d(TAG, "handleMessage: COMBINATION_ONE_TEST_FINISHED, finish one test");
                    resetResultValue();
                    saveTmpTestResult();
                    break;

                case WIFI_DISCONNECTED:
                    Toast.makeText(getApplicationContext(), getString(R.string.text_wifi_disconnected), Toast.LENGTH_SHORT).show();
                    break;

                case FTP_SERVER_CONNECT_FAILED:
                    Toast.makeText(getApplicationContext(), getString(R.string.text_connect_ftp_server_failed), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public class ServiceBinder extends Binder{
        public Service getService(){
            return AutoTestService.this;
        }
    }

    public boolean isInTesting(){
        return isInTesting;
    }

    public int startTest(Bundle bundle){
        if (isInTesting){
            Toast.makeText(this, getString(R.string.text_in_testing), Toast.LENGTH_SHORT).show();
            return -1;
        }
        if (initTestParams(bundle) < 0){
            Toast.makeText(this, getString(R.string.text_phone_null), Toast.LENGTH_SHORT).show();
            return -1;
        }

//        isStartTest = true;
//        saveTmpTestResult();
//        Constant.delLog(powerManager);

        resetResultValue();
        new TestThread().start();
        return 0;
    }

    public void startOneCallTest(String saveDir, String callPhone, int waitTime, int durationTime, NormalTestCallback normalTestCallback){
        Bundle bundle = new Bundle();
        storeTestResultDir = saveDir;
        callback = normalTestCallback;
        bundle.putString(getString(R.string.key_phone), callPhone);
        bundle.putInt(getString(R.string.key_wait_time), waitTime);
        bundle.putInt(getString(R.string.key_duration_time), durationTime);
        bundle.putInt(getString(R.string.key_test_times), 1);
        bundle.putBoolean(getString(R.string.key_is_combination_test), true);
        if (initTestParams(bundle) < 0){
            Log.d(TAG, "startOneCallTest: Call Test Exception");
            mHandler.sendEmptyMessage(COMBINATION_ONE_TEST_FINISHED);
            return;
        }

        resetResultValue();
        new TestOneThread().start();
    }

    public void startOneSmsTest(String saveDir, String smsPhone, int waitResultTime, String smsString, NormalTestCallback normalTestCallback){
        Bundle bundle = new Bundle();
        storeTestResultDir = saveDir;
        callback = normalTestCallback;
        bundle.putString(getString(R.string.key_phone), smsPhone);
        bundle.putInt(getString(R.string.key_wait_sms_result_time), waitResultTime);
        bundle.putString(getString(R.string.key_sms_string), smsString);
        bundle.putInt(getString(R.string.key_test_times), 1);
        bundle.putBoolean(getString(R.string.key_is_combination_test), true);
        if (initTestParams(bundle) < 0){
            Log.d(TAG, "startOneCallTest: Sms Test Exception");
            mHandler.sendEmptyMessage(COMBINATION_ONE_TEST_FINISHED);
            return;
        }
        resetResultValue();
        new TestOneThread().start();
    }

    protected void resetResultValue(){
        totalRunTimes = 0;
        successCount = 0;
        failedCount = 0;
    }

    protected class TestThread extends Thread{

        public TestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            mWakeLock.acquire();
            isInTesting = true;
            Constant.openTTLog();
            if (Constant.isWifiConnected(getApplicationContext())){
                Log.d(TAG, "run: wifi have been connected, jude the ftp server weather be login");
                if (Constant.isUpload(getApplicationContext())){
                    Constant.readAndUploadTTLog(Constant.getTestResultFileName(storeTestResultDir), getApplicationContext());
                }else {
                    mHandler.sendEmptyMessage(FTP_SERVER_CONNECT_FAILED);
                    Constant.readTTLog(Constant.getTestResultFileName(storeTestResultDir));
                }
            }else {
                Log.d(TAG, "run: wifi haven't been connected, don't upload the file to ftp server");
                mHandler.sendEmptyMessage(WIFI_DISCONNECTED);
                Constant.readTTLog(Constant.getTestResultFileName(storeTestResultDir));
            }
            runTestLogic();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isInTesting = false;
            Constant.closeTTLog();
            startResultActivity(getResultActivity());
            resetResultValue();
            saveTmpTestResult();
        }
    }


    protected class TestOneThread extends Thread{

        public TestOneThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mWakeLock.acquire();
            isInTesting = true;
            runTestLogic();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isInTesting = false;
            mHandler.sendEmptyMessage(COMBINATION_ONE_TEST_FINISHED);
        }
    }

    protected void startResultActivity(Class<?> resultActivity){
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (resultActivity.equals(CallTestActivity.class)){
            intent.putExtra(getString(R.string.key_result), storeTestResultDir + "/" + CALL_TEST_RESULT_FILENAME);
        }else if (resultActivity.equals(SmsTestActivity.class)){
            intent.putExtra(getString(R.string.key_result), storeTestResultDir + "/" + SMS_TEST_RESULT_FILENAME);
        }
        startActivity(intent);
    }

    protected void storeTestResult(String fileName){
        File file = new File(storeTestResultDir + "/" + fileName);
        Log.d(TAG, "storeTestResult: get the storeTestDir: " + storeTestResultDir + "/" + fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "storeTestResult: =====Create Test Result Succeed=====");
            } catch (IOException e) {
                Log.d(TAG, "storeTestResult: =====Create Test Result Failed=====");
                e.printStackTrace();
                return;
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + testTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_total_runTimes) + totalRunTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_succeed_times) +  successCount);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_failed_times) +  failedCount);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_succeed_rate) + (successCount / totalRunTimes) * 100 + "%");

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "===============Save the Test Result Succeed===============");
        } catch (IOException e) {
            Log.d(TAG, "===============Save the Test Result Failed===============");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mWakeLock.isHeld()){
            mWakeLock.release();
        }
    }

}
