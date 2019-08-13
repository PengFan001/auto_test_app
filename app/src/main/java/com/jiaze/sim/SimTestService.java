package com.jiaze.sim;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.jiaze.autotestapp.R;
import com.jiaze.common.Constant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.sim
 * Create by jz-pf
 * on 2019/7/6
 * =========================================
 */
public class SimTestService extends Service {
    private static final String TAG = "SimTestService";
    private static final String SIM_TEST_PARAM_SAVE_PATH = "SimTestParam";
    private static final String TEST_PARAM = "SimTest";

    private int simTestTime = 0;
    private String simState = null;
    private int totalRunTimes = 0;
    private int unknownTimes = 0;
    private int readyTimes = 0;
    private int absentTimes = 0;
    private int pinRequiredTimes = 0;
    private int pukRequiredTimes = 0;
    private int netWorkLockTimes = 0;
    private static boolean runtNextTime = false;
    private static boolean isTesting = false;
    private static boolean isReboot = false;
    private static boolean isStop = false;
    private boolean isStartTest = false;
    private PowerManager powerManager;
    private PowerManager.WakeLock mWakeLock;
    private TelephonyManager telephonyManager;
    private String storeSimTestResultDir;
    private SimTestBinder simTestBinder = new SimTestBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: The Sim Test Service is start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        getTestParams();
        if (isStartTest){
            if (isTesting){
                Log.d(TAG, "onCreate: continue the the last time test, simTestTime = " + simTestTime);
                new SimTestThread().start();
            }else {
                Log.d(TAG, "onCreate: isStartTest is true, start the test");
                new SimTestThread().start();
            }
        }else {
            Log.d(TAG, "onCreate: isStartTest is false, need not do anything");
            resetTestValue();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return simTestBinder;
    }

    class SimTestBinder extends Binder{
        public void startTest(Bundle bundle){
            isStop = false;
            isStartTest = true;
            simTestTime = bundle.getInt(getString(R.string.key_sim_test_time));
            storeSimTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "startTest: Create the Sim Test Result Dir: " + storeSimTestResultDir);
            saveTestParamsAndTmpResult();
            Constant.delLog(powerManager);
            //new SimTestThread().start();
        }

        public void stopTest(){
            isTesting = false;
            isStartTest = false;
            saveTestParamsAndTmpResult();
        }

        public String getSimState(){
            return simState;
        }

        public boolean isInTesting(){
            return isTesting;
        }
    }

    private void getTestParams(){
        Properties properties = Constant.loadTestParameter(this, SIM_TEST_PARAM_SAVE_PATH);
        simTestTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_test_time), "0"));
        isTesting = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_sim_testing), "false"));
        totalRunTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_run_times), "0"));
        absentTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_absent_times),"0"));
        unknownTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_unknown_times), "0"));
        readyTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_ready_times), "0"));
        pinRequiredTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_pin_times), "0"));
        pukRequiredTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_puk_times), "0"));
        netWorkLockTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_netWork_times), "0"));
        storeSimTestResultDir = properties.getProperty(getString(R.string.key_test_result_path));
        isStartTest = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_start_test), "false"));
        Log.d(TAG, "getTestParams: simTestTime = " + simTestTime + "   isStartTest = " + isStartTest + "     isTesting = " + isTesting + "     totalRunTimes = "+ totalRunTimes);
        Log.d(TAG, "getTestParams: absentTimes = " + absentTimes + "     unknownTimes = " + unknownTimes + "     readyTimes = " + readyTimes);
        Log.d(TAG, "getTestParams: pinRequiredTimes = " + pinRequiredTimes + "     pukRequiredTimes = " + pukRequiredTimes + "    netWorkLockTimes = " + netWorkLockTimes);
    }

    class SimTestThread extends Thread{
        SimTestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(7 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "run: start the simTest");
            mWakeLock.acquire();
            isTesting = true;
            Constant.openTTLog();
            Constant.readTTLog(Constant.getTestResultFileName(storeSimTestResultDir));
            runLogical();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isTesting = false;
            Constant.closeTTLog();
            showResultActivity(SimTestActivity.class);
            Log.d(TAG, "run: finished the test, then will show you the sim test result");
            resetTestValue();
            saveTestParamsAndTmpResult();
        }
    }

    private void runLogical(){
        Log.d(TAG, "runLogical: start run runLogical");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "runLogical run: isReboot = " + isReboot);
                Log.d(TAG, "runLogical run: isStop = " + isStop);
                while (!isReboot){
                    try {
                        Thread.sleep(3000);
                        Log.d(TAG, "runLogical run: isReboot = " + isReboot);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isStop){
                        Log.d(TAG, "runLogical run: stop the RebootThread ");
                        break;
                    }
                }

                if (isStop){
                    Log.d(TAG, "runLogical run: stop the RebootThread");
                }else {
                    if (powerManager != null){
                        powerManager.reboot("Sim test reboot");
                    }else {
                        Log.d(TAG, "runLogical run: please init the powerManager");
                    }
                }
            }
        }).start();
        while (simTestTime > 0 && isTesting){
            totalRunTimes++;
            Log.d(TAG, "runLogical: totalRunTimes = " + totalRunTimes);
            runtNextTime = false;
            simTestTime = simTestTime - 1;
            Log.d(TAG, "runLogical: rest the test time, simTestTime = " + simTestTime);
            readSimState();
            saveSimTestResult();
            saveTestParamsAndTmpResult();
            if (simTestTime - 1 >= 0){
                isReboot = true;
                Log.d(TAG, "runLogical: set isReboot true: ");
            }
            if (simTestTime == 0){
                runtNextTime = true;
            }
            Log.d(TAG, "runLogical: runNextTime : " + runtNextTime);
            while (!runtNextTime){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        isStop = true;
        saveSimTestResult();
    }

    private void saveTestParamsAndTmpResult(){
        Properties properties = new Properties();
        String fileDir = getFilesDir().getAbsolutePath() + "/" + SIM_TEST_PARAM_SAVE_PATH;
        File file = new File(fileDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveTestParamsAndTmpResult: reduce the sim test reboot time error");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParamsAndTmpResult: Create the reboot Test Parameter File Failed");
                e.printStackTrace();
            }
        }
        try {
            OutputStream outputStream = new FileOutputStream(file);
            properties.setProperty(getString(R.string.key_sim_test_time), String.valueOf(simTestTime));
            properties.setProperty(getString(R.string.key_test_result_path), storeSimTestResultDir);
            properties.setProperty(getString(R.string.key_sim_unknown_times), String.valueOf(unknownTimes));
            properties.setProperty(getString(R.string.key_sim_ready_times), String.valueOf(readyTimes));
            properties.setProperty(getString(R.string.key_sim_absent_times), String.valueOf(absentTimes));
            properties.setProperty(getString(R.string.key_sim_pin_times), String.valueOf(pinRequiredTimes));
            properties.setProperty(getString(R.string.key_sim_puk_times), String.valueOf(pukRequiredTimes));
            properties.setProperty(getString(R.string.key_sim_netWork_times), String.valueOf(netWorkLockTimes));
            properties.setProperty(getString(R.string.key_is_sim_testing), String.valueOf(isTesting));
            properties.setProperty(getString(R.string.key_sim_run_times), String.valueOf(totalRunTimes));
            properties.setProperty(getString(R.string.key_is_start_test), String.valueOf(isStartTest));
            properties.store(outputStream, "finished one time sim test");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "runLogical: open sim test params file failed");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "runLogical: store the sim properties failed");
            e.printStackTrace();
        }
        Log.d(TAG, "saveTestParamsAndTmpResult: save the tmp test Result success");
    }

    private void readSimState(){
        Log.d(TAG, "readSimState: start read the Sim State");
        if (telephonyManager != null){
            switch (telephonyManager.getSimState()){
                case TelephonyManager.SIM_STATE_ABSENT:
                    absentTimes =  absentTimes + 1;
                    simState = Constant.SIM_STATE_ABSENT;
                    Log.d(TAG, "readSimState: " + simState);
                    break;
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                    netWorkLockTimes = netWorkLockTimes + 1;
                    simState = Constant.SIM_STATE_NETWORK_LOCK;
                    Log.d(TAG, "readSimState: " + simState);
                    break;
                case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                    pinRequiredTimes = pinRequiredTimes + 1;
                    simState = Constant.SIM_STATE_PIN_REQUIRED;
                    Log.d(TAG, "readSimState: " + simState);
                    break;
                case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                    pukRequiredTimes = pukRequiredTimes + 1;
                    simState = Constant.SIM_STATE_PUK_REQUIRED;
                    Log.d(TAG, "readSimState: " + simState);
                    break;
                case TelephonyManager.SIM_STATE_READY:
                    readyTimes = readyTimes + 1;
                    simState = Constant.SIM_STATE_READY;
                    Log.d(TAG, "readSimState: " + simState);
                    break;
                case TelephonyManager.SIM_STATE_UNKNOWN:
                    unknownTimes = unknownTimes + 1;
                    simState = Constant.SIM_STATE_UNKNOWN;
                    Log.d(TAG, "readSimState: " + simState);
                    break;
            }
        }else {
            Log.d(TAG, "readSimState: telephonyManager not be init, please init the telephonyManager");
        }
    }

    private void saveSimTestResult(){
        Log.d(TAG, "saveSimTestResult: Start save the sim test Result");
        File file = new File(storeSimTestResultDir + "/" + "testResult");
        Log.d(TAG, "saveSimTestResult: get the storeSimTestParamsDir : " + storeSimTestResultDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveSimTestResult: =====Create The Sim Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveSimTestResult: =====Create The Sim Test Result File Failed");
                e.printStackTrace();
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_absent) + absentTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_unknown) + unknownTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_netWork_locked) + netWorkLockTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_pin_required) + pinRequiredTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_puk_required) + pukRequiredTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_ready) + readyTimes);

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "saveSimTestResult: save the sim test Result Success");
        } catch (IOException e) {
            Log.d(TAG, "saveSimTestResult: save the sim test Result Failed");
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
        intent.putExtra(getString(R.string.key_result), storeSimTestResultDir + "/" + "testResult");
        startActivity(intent);
    }

    private void resetTestValue(){
        totalRunTimes = 0;
        absentTimes = 0;
        readyTimes = 0;
        pinRequiredTimes = 0;
        pukRequiredTimes = 0;
        netWorkLockTimes = 0;
        unknownTimes = 0;
        isTesting = false;
        isStartTest = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
