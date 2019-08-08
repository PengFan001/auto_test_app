package com.jiaze.reboot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.jiaze.autotestapp.R;
import com.jiaze.common.Constant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.reboot
 * Create by jz-pf
 * on 2019/7/31
 * =========================================
 */
public class ModuleRebootService extends Service {

    private static final String TAG = "ModuleRebootService";
    private static final String MODULE_TEST_PARAMS_SAVE_PATH = "ModuleTestParams";
    private static final String TEST_PARAM = "ModuleRebootTest";
    private static final String POWER_STATE_PATH = "sys/misc-config/spower_key";    //this path can read and write
    private static final String POWER_PATH = "sys/misc-config/spower";  //this path write-only
    
    private int moduleTestTime = 0;
    private String moduleState;
    private int totalRunTimes = 0;
    private int successTimes = 0;
    private int failedTimes = 0;
    private int upTimes = 0;
    private int downTimes = 0;
    private int upSuccessTimes = 0;
    private int upFailedTimes = 0;
    private int downSuccessTimes = 0;
    private int downFailedTimes = 0;
    private static boolean isInTesting = false;
    private boolean isRunNextTime = false;
    private PowerManager powerManager;
    private PowerManager.WakeLock mWakeLock;
    private String storeModuleTestResultDir;
    private ModuleRebootBinder moduleRebootBinder = new ModuleRebootBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return moduleRebootBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: the Module Reboot Test Service is Start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    class ModuleRebootBinder extends Binder{
        public void startTest(Bundle bundle){
            moduleTestTime = bundle.getInt(getString(R.string.key_module_test_time));
            storeModuleTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "startTest: Create the storeModuleTestResultDir = " + storeModuleTestResultDir);
            new ModuleRebootTestThread().start();
        }

        public void stopTest(){
            Log.d(TAG, "stopTest: stop the test");
            isInTesting = false;
        }

        public boolean isInTesting(){
            return isInTesting;
        }

        public String getModuleState(){
            if (getPowerState() == 0){
                moduleState = getString(R.string.text_down_power);
            }else if (getPowerState() == 1){
                moduleState = getString(R.string.text_up_power);
            }else {
                Log.d(TAG, "getModuleState: get the spower_key value error");
                return null;
            }
            return moduleState;
        }
    }

    class ModuleRebootTestThread extends Thread{
        ModuleRebootTestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            mWakeLock.acquire();
            isInTesting = true;
            runLogical();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isInTesting = false;
            showResultActivity(ModuleRebootActivity.class);
            Log.d(TAG, "run: finished the test, then will show you the test result");
            resetValue();
        }
    }

    private void runLogical(){
        Log.d(TAG, "runLogical: start the test runLogical");
        for (; moduleTestTime > 0 && isInTesting; moduleTestTime--){
            totalRunTimes++;
            Log.d(TAG, "runLogical: totalRunTimes = " + totalRunTimes);
            int lastState = getPowerState();
            if (lastState == 0){
                Log.d(TAG, "runLogical: the module is down power, then we will up power");
                upTimes++;
                switchPowerState(getString(R.string.param_up_power));
                if (getPowerState() == Integer.parseInt(getString(R.string.param_up_power))){
                    upSuccessTimes++;
                    Log.d(TAG, "runLogical: up the module power success, upSuccessTimes = " + upSuccessTimes + "then we will down power");
                    sendModuleStateChange(moduleRebootBinder.getModuleState());
                    downTimes++;
                    switchPowerState(getString(R.string.param_down_power));

                    if (getPowerState() == Integer.parseInt(getString(R.string.param_down_power))){
                        downSuccessTimes++;
                        sendModuleStateChange(moduleRebootBinder.getModuleState());
                        successTimes++;
                        Log.d(TAG, "runLogical: down the power success, downSuccessTimes = " + downSuccessTimes + "  switch the module power success = " + successTimes);
                    }else {
                        downFailedTimes++;
                        failedTimes++;
                        Log.d(TAG, "runLogical: down the power failed, switch the module power failed, failedTimes = " + failedTimes);
                    }

                }else {
                    upFailedTimes++;
                    failedTimes++;
                    Log.d(TAG, "runLogical: up the power failed, switch the module power failed, failedTimes = " + failedTimes);
                }
            }else if (lastState == 1){
                Log.d(TAG, "runLogical: the module is up power, then we will down power");
                downTimes++;
                switchPowerState(getString(R.string.param_down_power));
                if (getPowerState() == Integer.parseInt(getString(R.string.param_down_power))){
                    //successTimes++;
                    downSuccessTimes++;
                    Log.d(TAG, "runLogical: down the module power success, downSuccessTimes = " + downSuccessTimes + "  then we will up power");
                    sendModuleStateChange(moduleRebootBinder.getModuleState());

                    upTimes++;
                    switchPowerState(getString(R.string.param_up_power));
                    if (getPowerState() == Integer.parseInt(getString(R.string.param_up_power))){
                        upSuccessTimes++;
                        sendModuleStateChange(moduleRebootBinder.getModuleState());
                        successTimes++;
                        Log.d(TAG, "runLogical: up the module power success, switch the module power success, successTimes = " + successTimes);
                    }else {
                        upFailedTimes++;
                        failedTimes++;
                        Log.d(TAG, "runLogical: up the module power failed, switch the module power failed, failedTimes = " + failedTimes);
                    }

                }else {
                    downFailedTimes++;
                    failedTimes++;
                    Log.d(TAG, "runLogical: down module power failed, switch the module power failed, failedTimes = " + failedTimes);
                }
            }else {
                Log.d(TAG, "runLogical: the module state is error, test failed, please check the power path");
                failedTimes++;
            }
        }

        saveModuleTestResult();
    }

    private void switchPowerState(String value){
        Log.d(TAG, "switchPowerState: set the spower file value = " + value);
        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        File spowerFile = new File(POWER_PATH);
        if (!spowerFile.exists()){
            Log.d(TAG, "switchPowerState: the spower file path error");
            return;
        }

        try {
            fileWriter = new FileWriter(spowerFile);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(value);
            Log.d(TAG, "switchPowerState: switch the power state success");
        } catch (IOException e) {
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

        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int getPowerState(){
        Log.d(TAG, "run: reader the power state from the device");
        int state = -1;
        BufferedReader bufferedReader = null;
        FileReader fileReader = null;
        StringBuilder builder = new StringBuilder();
        String line;
        File powerStateFile = new File(POWER_STATE_PATH);
        if (!powerStateFile.exists()){
            Log.d(TAG, "run: the POWER_STATE_PATH is not exist, error");
            return state;
        }
        try {
            fileReader = new FileReader(powerStateFile);
            bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null){
                builder.append(line);
            }
            Log.d(TAG, "run: get the spower_key file value = " + builder.toString());
            state = Integer.parseInt(builder.toString());
            Log.d(TAG, "getPowerState: get the module state = " + state);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "run: read the FileReader Failed");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "run: readLine error");
            e.printStackTrace();
        }finally {
            if (bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.d(TAG, "run: close the Reader buffer");
                    e.printStackTrace();
                }
            }
        }
        
        return state;
    }

    private void sendModuleStateChange(String state){
        Intent intent = new Intent("com.jiaze.action.MODULE_POWER_STATE_CHANGE");
        intent.putExtra("state", state);
        sendBroadcast(intent);
    }

    private void saveModuleTestResult(){
        Log.d(TAG, "saveModuleTestResult: Start Save the Module Test Result");
        File file = new File(storeModuleTestResultDir + "/" + "testResult");
        Log.d(TAG, "saveModuleTestResult: get the storeModuleTestResultDir = " + storeModuleTestResultDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveModuleTestResult: =====Create the Module Reboot Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveModuleTestResult: =====Create the Module Reboot Test Result File Failed");
                e.printStackTrace();
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_switch_succeed_time) + successTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_switch_failed_time) + failedTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_module_up_power_times) + upTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_up_power_success_times) + upSuccessTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_up_power_failed_times) + upFailedTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_module_down_power_times) + downTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_down_power_success_times) + downSuccessTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_down_power_failed_times) + downFailedTimes);

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "saveModuleTestResult: Save the Module Reboot Test Result Success");
        } catch (IOException e) {
            Log.d(TAG, "saveModuleTestResult: Save The Module Reboot Test Result Failed");
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
        intent.putExtra(getString(R.string.key_test_result_path), storeModuleTestResultDir + "/" + "testResult" );
        startActivity(intent);
    }

    private void resetValue(){
        totalRunTimes = 0;
        successTimes = 0;
        failedTimes = 0;
        upTimes = 0;
        upSuccessTimes = 0;
        upFailedTimes = 0;
        downTimes = 0;
        downSuccessTimes = 0;
        downFailedTimes = 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()){
            mWakeLock.release();
        }
    }
}
