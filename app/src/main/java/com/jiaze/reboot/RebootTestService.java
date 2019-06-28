package com.jiaze.reboot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jiaze.autotestapp.R;
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
 * the Package:com.jiaze.reboot
 * Create by jz-pf
 * on 2019/6/28
 * =========================================
 */
public class RebootTestService extends Service {

    private static final String REBOOT_TEST_PARAM_SAVE_PATH = "RebootTestParams";
    private static final String TEST_PARAM = "RebootTest";
    private static final String TAG = "RebootTestService";
    private int rebootTestTime = 0;
    private PowerManager powerManager;
    private String storeRebootTestResultDir;
    private RebootTestBinder rebootTestBinder = new RebootTestBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: the RebootTestService is start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        getTestParameter();
        runLogical(rebootTestTime, false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: binder the RebootTestBinder, you can get the function by this Binder");
        return rebootTestBinder;
    }

    //RebootTestService 提供的服务均写在这个里面
    class RebootTestBinder extends Binder{
        public void startTest(Bundle bundle){
            int testTime = bundle.getInt(getString(R.string.key_reboot_test_time));
            runLogical(testTime, true);
        }

        public void stopTest(){
            rebootTestTime = 0;
        }

    }

    private void getTestParameter(){
        Properties properties = Constant.loadTestParameter(this, REBOOT_TEST_PARAM_SAVE_PATH);
        rebootTestTime = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_test_time), "0"));
        Log.d(TAG, "getTestParameter: get the reboot test times" + rebootTestTime);
        storeRebootTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
        Log.d(TAG, "getTestParameter: Create the Reboot Test Result Dir: " + storeRebootTestResultDir);
    }

    private void runLogical(int testTimes, boolean btnReboot){
        if (btnReboot){
            rebootTestTime = testTimes;
        }
        while (rebootTestTime > 0){
            rebootTestTime = rebootTestTime -1;
            Log.d(TAG, "runLogical: ==========reboot times : " + rebootTestTime);
            Properties properties = new Properties();
            String fileDir = getFilesDir().getAbsolutePath() + "/" + REBOOT_TEST_PARAM_SAVE_PATH;
            File file = new File(fileDir);
            if (!file.exists()){
                try {
                    file.createNewFile();
                    Log.d(TAG, "runLogical: reduce the rebootTimes error");
                } catch (IOException e) {
                    Log.d(TAG, "runLogical: Create the Reboot Test Parameter File Failed");
                    e.printStackTrace();
                }
            }
            try {
                OutputStream outputStream = new FileOutputStream(file);
                properties.setProperty(getString(R.string.key_reboot_test_time), String.valueOf(rebootTestTime));
                try {
                    properties.store(outputStream, "finished one Reboot Test");
                } catch (IOException e) {
                    Log.d(TAG, "runLogical: Failed to save the properties to the file");
                    e.printStackTrace();
                }
                if (outputStream != null){
                    outputStream.close();
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "runLogical: Create FileOutputStream Failed");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "runLogical: close OutputStream failed");
                e.printStackTrace();
            }

            if (powerManager != null){
                powerManager.reboot("get reboot code");
                Log.d(TAG, "runLogical: the System is start reboot");
            }
        }
        Log.d(TAG, "runLogical: Don't need to reboot" + rebootTestTime);
    }

    private void saveRebootTestResult(){
        //todo save the reboot test result
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
