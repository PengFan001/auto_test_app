package com.jiaze.airmode;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
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
 * the Package:com.jiaze.airmode
 * Create by jz-pf
 * on 2019/7/15
 * =========================================
 */
public class AirModeTestService extends Service {

    private static final String TAG = "AirModeTestService";
    private static final String AIR_MODE_TEST_PARAM_SAVE_PATH = "AirModeTestParams";
    private static final String TEST_PARAM = "AirModeTest";

    private int airModeTestTime = 0;
    private String airModeState = null;
    private int totalRunTime = 0;
    private int openTimes = 0;
    private int closeTimes = 0;
    private int successTimes = 0;
    private int failedTimes = 0;
    private int openSuccessTimes = 0;
    private int closeSuccessTimes = 0;
    private int openFailedTimes = 0;
    private int closeFailedTimes = 0;
    private int state = 2;
    private int lastState = 0;
    private static boolean isTesting = false;
    private static boolean isRunNextTime = false;
    private String storeAirModeTestResultDir;
    private PowerManager powerManager;
    private PowerManager.WakeLock mWakeLock;
    private AirModeTestBinder airModeTestBinder = new AirModeTestBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Air Mode Test Service is start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return airModeTestBinder;
    }

    class AirModeTestBinder extends Binder{
        public void startTest(Bundle bundle){
            airModeTestTime = bundle.getInt(getString(R.string.key_air_mode_test_time));
            storeAirModeTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "startTest: Create the Air Mode Test Resulr Dir : " + storeAirModeTestResultDir);
            new AirModeTestThread().start();
        }

        public void stopTest(){
            isTesting = false;
        }

        public String getAirModeState(){
            readAirModeState();
            return airModeState;
        }

        public boolean isInTesting(){
            return isTesting;
        }
    }

    class AirModeTestThread extends Thread{
        AirModeTestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "run: start air mode Test");
           // mWakeLock.acquire();
            isTesting = true;
            runLogical();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isTesting = false;
            showResultActivity(AirModeTestActivity.class);
            Log.d(TAG, "run: finished the test, then will show you the AirMode Test result");
            resetTestValue();
        }
    }

    private void runLogical(){

        for (; airModeTestTime > 0 && isTesting; airModeTestTime--){
            totalRunTime++;
            Log.d(TAG, "runLogical: air Mode testTime reduce 1, rest test time = " + airModeTestTime);
            Log.d(TAG, "run: start test the air mode close and open ");
            readAirModeState();
            lastState = state;
            if (isAirModeOpened()){
                /**
                 * if air mode was opened, we will try to close it
                 */
                closeTimes++;
                switchAirMode(0);
                Log.d(TAG, "run: send the close air mode broadcast");

                try {
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                readAirModeState();
                Log.d(TAG, "runLogical: the lastState of air Mode : " + lastState + " ===== now state of air Mode : " + state);
                if (switchAirModeResult()){
                    closeSuccessTimes++;
                    sendAirModeChangeState(airModeState);
                    Log.d(TAG, "runLogical: close the air mode success, then will open it, close SuccessTimes = " + closeSuccessTimes);

                    openTimes++;
                    lastState = state;
                    switchAirMode(1);
                    try {
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    readAirModeState();
                    Log.d(TAG, "runLogical: the lastState of air Mode : " + lastState + " ===== now state of air Mode : " + state);
                    if (switchAirModeResult()){
                        openSuccessTimes++;
                        successTimes++;
                        sendAirModeChangeState(airModeState);
                        Log.d(TAG, "runLogical: open the air mode success, openSuccessTimes = " + openSuccessTimes + "   air mode switch successTimes = " + successTimes);
                    }else {
                        openFailedTimes++;
                        failedTimes++;
                        Log.d(TAG, "runLogical: open the air mode failed, openFailedTimes = " + openSuccessTimes);
                    }

                }else {
                    closeFailedTimes++;
                    failedTimes++;
                    Log.d(TAG, "runLogical: close the air mode failed, close FailedTimes = " + closeFailedTimes + "    and air mode switch failed, failedTimes = " + failedTimes);
                }
            }else {
                /**
                 * if air mode was closed, we will try to open it
                 */
                openTimes++;
                switchAirMode(1);
                Log.d(TAG, "run: send the open air mode broadcast");

                try {
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                readAirModeState();
                Log.d(TAG, "runLogical: the lastState of air Mode : " + lastState + " ===== now state of air Mode : " + state);

                if (switchAirModeResult()){
                    openSuccessTimes++;
                    sendAirModeChangeState(airModeState);
                    Log.d(TAG, "runLogical: open airMode success, then will close it, openSuccessTimes = " + openSuccessTimes);

                    closeTimes++;
                    lastState = state;
                    switchAirMode(0);
                    try {
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    readAirModeState();
                    Log.d(TAG, "runLogical: the lastState of air Mode : " + lastState + " ===== now state of air Mode : " + state);

                    if (switchAirModeResult()){
                        closeSuccessTimes++;
                        successTimes++;
                        sendAirModeChangeState(airModeState);
                        Log.d(TAG, "runLogical: close airMode Success, closeSuccessTimes = " + closeSuccessTimes + "switch AirMode SuccessTimes = " + successTimes);
                    }else {
                        closeFailedTimes++;
                        failedTimes++;
                        Log.d(TAG, "runLogical: close air mode failed, closeFailedTimes = " + closeFailedTimes + "     and switch airMode failedTimes = " + failedTimes);
                    }

                }else {
                    openFailedTimes++;
                    failedTimes++;
                    Log.d(TAG, "runLogical: open the air mode failed, openFailedTimes = " + openFailedTimes + "     switch air mode failedTimes = " + failedTimes);
                }
            }
        }
        saveAirModeTestResult();
    }

    private void sendAirModeChangeState(String airMode){
        Intent intent = new Intent("com.jiaze.action.AIR_MODE_STATE_CHANGED");
        intent.putExtra("state", airMode);
        sendBroadcast(intent);
    }

    /**
     * if air mode is open return true;
     * @return
     */
    private boolean isAirModeOpened(){
        int state = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 2);
        Log.d(TAG, "isAirModeOpened: air mode state : " + state);
        if (state == 0){
            return false;
        }else{
            return true;
        }
    }

    private void switchAirMode(int value){
        Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, value);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", true);
        sendBroadcast(intent);
    }

    /**
     * this function is use to jude the air mode change result, if change succeed return true, else return false;
     * @return the result of air mode change
     */
    private boolean switchAirModeResult(){
        if (Math.abs(lastState - state) == 1){
            return true;
        }else {
           return false;
        }
    }

    private void readAirModeState(){
        Log.d(TAG, "getAirModeState: start get the AirMode State");
        state = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 2);
        if (state == 0){
            airModeState = getString(R.string.air_mode_close);
            Log.d(TAG, "getAirModeState: air mode state = " + state + "  airModeState = " + airModeState);
        }else if (state == 1){
            airModeState = getString(R.string.air_mode_open);
            Log.d(TAG, "getAirModeState: air mode state = " + state + "  airModeState = " + airModeState);
        }else if (state == 2){
            Log.d(TAG, "getAirModeState: get Air Mode failed, state = " + state);
        }
    }

    private void saveAirModeTestResult(){
        Log.d(TAG, "saveAirModeTestResult: Start save the AirMode Test Result");
        File file = new File(storeAirModeTestResultDir + "/" + "testResult");
        Log.d(TAG, "saveAirModeTestResult: get the storeAirModeTestResultDir : " + storeAirModeTestResultDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveAirModeTestResult: =====Create the AirMode Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveAirModeTestResult: =====Create the AirMode Test Result File Failed");
                e.printStackTrace();
            }
        }
        
        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTime);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_switch_succeed_time) + successTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_switch_failed_time) + failedTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_open_time) + openTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_open_success_time) + openSuccessTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_open_failed_time) + openFailedTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_close_time) + closeTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_close_success_time) + closeSuccessTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_close_failed_time) + closeFailedTimes);

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {
            fileWriter= new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "saveAirModeTestResult: Save the Air Mode test Result File Failed");
        } catch (IOException e) {
            Log.d(TAG, "saveAirModeTestResult: Save the Air Mode test Result File Failed");
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
    
    private void resetTestValue(){
        totalRunTime = 0;
        openTimes = 0;
        openSuccessTimes = 0;
        openFailedTimes = 0;
        closeTimes = 0;
        closeSuccessTimes = 0;
        closeFailedTimes = 0;
        successTimes = 0;
        failedTimes = 0;
        airModeState = null;
    }

    private void showResultActivity(Class<?> resultActivity){
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getString(R.string.key_test_result_path), storeAirModeTestResultDir + "/" + "testResult" );
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
