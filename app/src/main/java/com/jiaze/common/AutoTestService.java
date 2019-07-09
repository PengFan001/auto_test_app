package com.jiaze.common;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.jiaze.autotestapp.R;

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
    protected int testTimes = 0;
    protected int totalRunTimes = 0;
    protected int successCount = 0;
    protected int failedCount = 0;
    protected String storeTestResultDir;

    protected boolean runNextTime = false;
    protected boolean isInTesting = false;

    protected PowerManager.WakeLock mWakeLock;

    public abstract void stopTest();
    protected abstract void runTestLogic();
    protected abstract int initTestParams(Bundle bundle);
    protected abstract Class<?> getResultActivity();

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
            return -1;
        }
        resetResultValue();
        new TestThread().start();
        return 0;
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
            runTestLogic();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isInTesting = false;
            startResultActivity(getResultActivity());
        }
    }

    protected void startResultActivity(Class<?> resultActivity){
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getString(R.string.key_result), storeTestResultDir + "/" + "testResult");
        startActivity(intent);
    }

    protected void storeTestResult(){
        File file = new File(storeTestResultDir + "/" + "testResult");
        Log.d(TAG, "storeTestResult: get the storeTestDir: " + storeTestResultDir + "/" + "testResult");
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
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_log_dir) + storeTestResultDir);

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
