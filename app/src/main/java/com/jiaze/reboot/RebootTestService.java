package com.jiaze.reboot;

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

import com.jiaze.at.AtSender;
import com.jiaze.autotestapp.R;
import com.jiaze.combination.CombinationTestService;
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
 * the Package:com.jiaze.reboot
 * Create by jz-pf
 * on 2019/6/28
 * =========================================
 */
public class RebootTestService extends Service {

    private static final String REBOOT_TEST_PARAM_SAVE_PATH = "RebootTestParams";
    private static final String TEST_PARAM = "RebootTest";
    private static final String TAG = "RebootTestService";
    private static final int SEND_JUDEGE_BOOT_MESSAGE = 1;
    private int rebootTestTime = 0;
    private int totalRunTimes = 0;
    private int rebootSuccessTime = 0;
    private int rebootFailedTime = 0;
    private boolean isTesting = false;
    private boolean runNextTime = false;
    private static boolean isReboot = false;
    private static boolean isStop = false;
    private boolean isStartTest = false;
    private PowerManager powerManager;
    private String storeRebootTestResultDir;
    private String command = "AT+CFUN?\r\n";
    private RebootTestBinder rebootTestBinder = new RebootTestBinder();
    private AtSender atSender;
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case SEND_JUDEGE_BOOT_MESSAGE:
                    Log.d(TAG, "handleMessage: SEND_JUDEGE_BOOT_MESSAGE, msg.arg1 = " + msg.arg1);
                    if (msg.arg1 == 0){
                        rebootFailedTime++;
                        Log.d(TAG, "handleMessage: the device reboot failed, rebootFailedTime = " + rebootFailedTime);
                        continueTest();
                    }else {
                        String[] lines = (String[]) msg.obj;
                        for (String s : lines){
                            Log.d(TAG, "handleMessage: s = " + s);
                            if (s.contains("+CFUN")){
                                rebootSuccessTime++;
                                Log.d(TAG, "handleMessage: the device reboot success, rebootSuccessTime = " + rebootSuccessTime);
                                continueTest();
                                return false;
                            }
                        }

                        rebootFailedTime++;
                        Log.d(TAG, "handleMessage: the device reboot failed, rebootFailedTime = " + rebootFailedTime);
                        continueTest();
                    }
                    break;

                default:
                    atSender.handleMessage(msg);
                    break;

            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: the RebootTestService is start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        getTestParameter();
        if (isTesting){
            atSender = new AtSender(this, mHandler);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "onCreate: continue the last time test, rebootTestTime = " + rebootTestTime);
                    Constant.openTTLog();
                    if (Constant.isUpload(getApplicationContext())){
                        Constant.readAndUploadTTLog(Constant.getTestResultFileName(storeRebootTestResultDir), getApplicationContext());
                    }else {
                        Constant.readTTLog(Constant.getTestResultFileName(storeRebootTestResultDir));
                    }
                    Message message = mHandler.obtainMessage(SEND_JUDEGE_BOOT_MESSAGE);
                    int sendResult = atSender.sendATCommand(command, message, false);
                    if (sendResult == -1){
                        Log.d(TAG, "onCreate: open the device dev/STTYEMS42 failed, The Test was suspend, please Check the device module");
                        isTesting = false;
                        Constant.closeTTLog();
                        resetTestValue();
                        saveTestParamsAndTmpResult();
                    }else {
                        Log.d(TAG, "onCreate: send the AT Code: AT+CFUN?");
                    }
                }
            }).start();
        }else {
            Log.d(TAG, "onCreate: isTesting is false, don't need test");
        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (isTesting){
//                    try {
//                        Thread.sleep(5 * 1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d(TAG, "onCreate: continue the last time test, rebootTestTime = " + rebootTestTime);
//                    Constant.openTTLog();
//                    Constant.readTTLog(Constant.getTestResultFileName(storeRebootTestResultDir));
//                    Message message = mHandler.obtainMessage(SEND_JUDEGE_BOOT_MESSAGE);
//                    int sendResult = atSender.sendATCommand(command, message, false);
//                    if (sendResult == -1){
//                        Log.d(TAG, "onCreate: open the device dev/STTYEMS42 failed, The Test was suspend, please Check the device module");
//                        isTesting = false;
//                        resetTestValue();
//                        saveTestParamsAndTmpResult();
//                    }else {
//                        Log.d(TAG, "onCreate: send the AT Code: AT+CFUN?");
//                    }
//                }
//            }
//        }).start();
//        if (isStartTest){
//            atSender = new AtSender(this, mHandler);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    if (isTesting){
//                        try {
//                            Thread.sleep(5 * 1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        Log.d(TAG, "onCreate: continue the last time test, rebootTestTime = " + rebootTestTime);
//                        Constant.openTTLog();
//                        Constant.readTTLog(Constant.getTestResultFileName(storeRebootTestResultDir));
//                        Message message = mHandler.obtainMessage(SEND_JUDEGE_BOOT_MESSAGE);
//                        int sendResult = atSender.sendATCommand(command, message, false);
//                        if (sendResult == -1){
//                            Log.d(TAG, "onCreate: open the device dev/STTYEMS42 failed, The Test was suspend, please Check the device module");
//                            isTesting = false;
//                            resetTestValue();
//                            saveTestParamsAndTmpResult();
//                        }else {
//                            Log.d(TAG, "onCreate: send the AT Code: AT+CFUN?");
//                        }
//                    }else {
//                        Log.d(TAG, "onCreate: isStartTest is true, start the test");
//                        new RebootTestThread().start();
//                    }
//                }
//            }).start();
//        }else {
//            Log.d(TAG, "onCreate: isStartTest is false, need not do anything");
//            resetTestValue();
//        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: binder the RebootTestBinder, you can get the function by this Binder");
        return rebootTestBinder;
    }

    private void continueTest(){
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new RebootTestThread().start();
    }

    private void showResultActivity(Class<?> resultActivity){
        Log.d(TAG, "showResultActivity: ======");
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getString(R.string.key_result), storeRebootTestResultDir + "/" + "rebootTestResult");
        Log.d(TAG, "showResultActivity: ============Start a New Activity=============");
        startActivity(intent);
    }

    //RebootTestService 提供的服务均写在这个里面
    public class RebootTestBinder extends Binder{
        public void startTest(Bundle bundle){
            resetTestValue();
            isStop = false;
            isStartTest = true;
            rebootTestTime = bundle.getInt(getString(R.string.key_reboot_test_time));
            storeRebootTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
            Log.d(TAG, "startTest: Create the Reboot Test Result Save Path : " + storeRebootTestResultDir);
//            saveTestParamsAndTmpResult();
//            Constant.delLog(powerManager);
            new RebootTestThread().start();
        }

        public void stopTest(){
            isReboot = false;
            isStop = true;
            isTesting = false;
            isStartTest = false;
            rebootTestTime = 0;
            saveTestParamsAndTmpResult();
        }

        public boolean isInTesting(){
            return isTesting;
        }

    }

    class RebootTestThread extends Thread{
        RebootTestThread(){
            super();
        }

        @Override
        public void run() {
            super.run();
            isTesting = true;
            Constant.openTTLog();
            Constant.readTTLog(Constant.getTestResultFileName(storeRebootTestResultDir));
            runLogical();
            isTesting = false;
            Constant.closeTTLog();
            Log.d(TAG, "run: isTesting = " + isTesting);
            showResultActivity(RebootTestActivity.class);
            Log.d(TAG, "run: finished the test, then will show you test result");
            resetTestValue();
            saveTestParamsAndTmpResult();
        }
    }

    private void getTestParameter(){
        Properties properties = Constant.loadTestParameter(this, REBOOT_TEST_PARAM_SAVE_PATH);
        rebootTestTime = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_test_time), "0"));
        isTesting = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_reboot_testing), "false"));
        totalRunTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_run_time), "0"));
        rebootSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_success_time), "0"));
        rebootFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_failed_time), "0"));
        isStartTest = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_start_test), "false"));
        storeRebootTestResultDir = properties.getProperty(getString(R.string.key_test_result_path));
        Log.d(TAG, "getTestParameter: rebootTestTime = " + rebootTestTime + "   isStartTest = " + isStartTest + "      isTesting = " + isTesting);
        Log.d(TAG, "getTestParameter: totalRunTimes = " + totalRunTimes + "       rebootSuccessTime = " + rebootSuccessTime + "       rebootFailedTime = " + rebootFailedTime);
    }

    private void runLogical(){
        Log.d(TAG, "runLogical: start run logical");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: isReboot = " + isReboot);
                Log.d(TAG, "run: isStop = " + isStop);
                while (!isReboot){
                    try {
                        Thread.sleep(3 * 1000);
                        Log.d(TAG, "run: isReboot = " + isReboot);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (isStop){
                        Log.d(TAG, "run: stop the RebootThread");
                        break;
                    }
                }

                if (isStop){
                    Log.d(TAG, "run: stop the RebootThread");
                }else {
                    if (powerManager != null){
                        powerManager.reboot("Reboot Test Reboot");
                    }else {
                        Log.d(TAG, "run: Please init the powerManager");
                    }
                }
            }
        }).start();

        while (rebootTestTime > 0 && isTesting){
            totalRunTimes++;
            Log.d(TAG, "runLogical: totalRunTimes = " + totalRunTimes);
            runNextTime = false;
            rebootTestTime = rebootTestTime - 1;
            Log.d(TAG, "runLogical: rebootTestTime reduce 1, rebootTestTime = " + rebootTestTime);
            saveTestParamsAndTmpResult();
            isReboot = true;
            Log.d(TAG, "runLogical: isRunNextTimes " + runNextTime);
            while (!runNextTime){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        isStop = true;

        saveRebootTestResult();
    }

    private void saveTestParamsAndTmpResult(){
        Properties properties = new Properties();
        String fileDir = getFilesDir().getAbsolutePath() + "/" + REBOOT_TEST_PARAM_SAVE_PATH;
        File file = new File(fileDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveTestParamsAndTmpResult: reduce the reboot test reboot time error");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParamsAndTmpResult: Create the reboot Test Parameter File Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(file);
            properties.setProperty(getString(R.string.key_reboot_test_time), String.valueOf(rebootTestTime));
            properties.setProperty(getString(R.string.key_test_result_path), storeRebootTestResultDir);
            properties.setProperty(getString(R.string.key_reboot_success_time), String.valueOf(rebootSuccessTime));
            properties.setProperty(getString(R.string.key_reboot_failed_time), String.valueOf(rebootFailedTime));
            properties.setProperty(getString(R.string.key_reboot_run_time), String.valueOf(totalRunTimes));
            properties.setProperty(getString(R.string.key_is_reboot_testing), String.valueOf(isTesting));
            properties.setProperty(getString(R.string.key_is_start_test), String.valueOf(isStartTest));
            properties.store(outputStream, "finished one time reboot test");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTestParamsAndTmpResult: open reboot test params file failed");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "saveTestParamsAndTmpResult: store the reboot properties failed");
            e.printStackTrace();
        }
        Log.d(TAG, "saveTestParamsAndTmpResult: save the tmp test Result success");
    }


    private void saveRebootTestResult(){
        File file = new File(storeRebootTestResultDir + "/" + "rebootTestResult");
        Log.d(TAG, "saveRebootTestResult: get the storeTestDir: " + storeRebootTestResultDir + "/rebootTestResult");
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveRebootTestResult: =====Create Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveRebootTestResult: =====Create Test Result File Failed");
                e.printStackTrace();
                return;
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTimes);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_reboot_success_time) + rebootSuccessTime);
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_reboot_failed_time) + rebootFailedTime);

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "saveRebootTestResult: Save the test Result Success");
        } catch (IOException e) {
            Log.d(TAG, "saveRebootTestResult: Save the test Result Failed");
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
        totalRunTimes = 0;
        rebootSuccessTime = 0;
        rebootFailedTime = 0;
        isTesting = false;
        rebootTestTime = 0;
        isStartTest = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (atSender != null){
            atSender.destory();
        }
    }

}
