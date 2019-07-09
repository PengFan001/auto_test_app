package com.jiaze.sim;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
    private static final int SIM_TEST_REBOOT = 6;
    private static final int SIM_TEST_NOT_REBOOT = 7;
    private static final int FINISHED_TEST = 8;

    private int simTestTime = 0;
    private String simState = null;
    private int totalRunTimes = 0;
    private int unknownTimes = 0;
    private int readyTimes = 0;
    private int absentTimes = 0;
    private int pinRequiredTimes = 0;
    private int pukRequiredTimes = 0;
    private int netWorkLockTimes = 0;
    private boolean runtNextTime = false;
    private boolean isTesting = false;
    private PowerManager powerManager;
    private TelephonyManager telephonyManager;
    private String storeSimTestResultDir;
    private SimTestBinder simTestBinder = new SimTestBinder();
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case SIM_TEST_REBOOT:
                    if (powerManager != null){
                        if (mHandler != null){
                            mHandler.removeCallbacks(rebootTask);
                        }
                        powerManager.reboot("Sim test reboot");
                    }else {
                        Log.d(TAG, "handleMessage: Please init the powerManager");
                    }
                    break;
                case FINISHED_TEST:
                    runtNextTime  = true;
                    Log.d(TAG, "handleMessage: runNextTime : " + runtNextTime);
                    break;
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: The Sim Test Service is start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        getTestParams();
        if (simTestTime > 0){
            Log.d(TAG, "onCreate: continue the last test");
            getTmpTestResult();
            runLogical();
        }else {
            Log.d(TAG, "onCreate: need't to continue the last test");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return simTestBinder;
    }

    //SimTestService 提供的服务
    class SimTestBinder extends Binder{
        public void startTest(Bundle bundle){
            isTesting = true;
            simTestTime = bundle.getInt(getString(R.string.key_sim_test_time));
            storeSimTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "getTestParameter: Create the Reboot Test Result Dir: " + storeSimTestResultDir);
            runLogical();
        }

        public void stopTest(){
            simTestTime = 0;
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
        Log.d(TAG, "getTestParams: get the sim test times" + simTestTime);
    }

    private void getTmpTestResult(){
        Properties properties = Constant.loadTestParameter(this, SIM_TEST_PARAM_SAVE_PATH);
        totalRunTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_run_times)));
        absentTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_absent_times)));
        unknownTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_unknown_times)));
        readyTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_ready_times)));
        pinRequiredTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_pin_times)));
        pukRequiredTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_puk_times)));
        netWorkLockTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_netWork_times)));
        storeSimTestResultDir = properties.getProperty(getString(R.string.key_test_result_path));
    }


    //设置重启计时器，在计时一段时间之后进行重启
    Runnable rebootTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "rebootTask run: wait 10 second, then the device will be reboot");
            mHandler.sendEmptyMessage(SIM_TEST_REBOOT);
        }
    };

    private void runLogical(){
        while(simTestTime > 0){
            totalRunTimes++;
            runtNextTime = false;
            if (simTestTime - 1 > 0){
                mHandler.postDelayed(rebootTask, 5000);
            }
            simTestTime = simTestTime - 1;
            Log.d(TAG, "runLogical: simTestTime reduce 1 : " + simTestTime);
            readSimState();
            saveSimTestResult();
            saveTestParamsAndTmpResult();
            if (simTestTime == 0){
                Log.d(TAG, "runLogical: send the finished test message");
                boolean isSend = mHandler.sendEmptyMessage(FINISHED_TEST);
                Log.d(TAG, "runLogical:  send the finished test message result isSend: " + isSend);
            }
            Log.d(TAG, "runLogical: runNextTime : " + runtNextTime);
            while (!runtNextTime){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "runLogical: finished the test, then will show you the testResult");
            isTesting = false;
        }
    }

    private void saveTestParamsAndTmpResult(){
        Properties properties = new Properties();
        String fileDir = getFilesDir().getAbsolutePath() + "/" + SIM_TEST_PARAM_SAVE_PATH;
        File file = new File(fileDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "runLogical: reduce the sim test reboot time error");
            } catch (IOException e) {
                Log.d(TAG, "runLogical: Create the reboot Test Parameter File Failed");
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
            properties.setProperty(getString(R.string.key_sim_run_times), String.valueOf(totalRunTimes));
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
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_log_dir) + storeSimTestResultDir);

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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
