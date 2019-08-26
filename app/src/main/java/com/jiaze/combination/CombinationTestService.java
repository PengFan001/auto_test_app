package com.jiaze.combination;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.jiaze.airmode.AirModeTestService;
import com.jiaze.at.AtSender;
import com.jiaze.autotestapp.R;
import com.jiaze.call.CallTestService;
import com.jiaze.callback.NetworkTestCallback;
import com.jiaze.callback.NormalTestCallback;
import com.jiaze.callback.SimTestCallback;
import com.jiaze.common.AutoTestService;
import com.jiaze.common.Constant;
import com.jiaze.network.NetworkTestService;
import com.jiaze.ps.PsTestService;
import com.jiaze.reboot.ModuleRebootService;
import com.jiaze.reboot.RebootTestService;
import com.jiaze.sim.SimTestService;
import com.jiaze.sms.SmsTestService;

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
 * the Package:com.jiaze.combination
 * Create by jz-pf
 * on 2019/8/13
 * =========================================
 */
public class CombinationTestService extends Service {

    private static final String TAG = "CombinationTestService";
    private static final String COMBINATION_TEST_PARAMS_SAVE_PATH = "CombinationTestParams";
    private static final String TEST_PARAM = "CombinationTest";
    private static final int SEND_JUDEGE_BOOT_MESSAGE = 1;
    private static final int IN_SERVICE_TIMEOUT = 2;
    private static final int WIFI_DISCONNECTED = 8;
    private static final int FTP_SERVER_CONNECT_FAILED = 9;

    private boolean isStartTest = false;
    private boolean continueTest = false;
    private boolean isPaused = false;
    private boolean isStartCallTest = false;
    private boolean isTestFailed = false;
    private static boolean isReboot = false;
    private static boolean isInTesting = false;
    private static boolean rebootIsChecked = false;
    private static boolean simIsChecked = false;
    private static boolean networkIsChecked = false;
    private static boolean callIsChecked = false;
    private static boolean airModeIsChecked = false;
    private static boolean smsIsChecked = false;
    private static boolean psIsChecked = false;
    private static boolean moduleRebootIsChecked = false;
    private int testTime = 0;
    private int totalRunTime = 0;
    /**reboot test result params**/
    private int rebootSuccessTime = 0;
    private int rebootFailedTime = 0;
    /**sim test result params**/
    private int unknownTime = 0;
    private int readyTime = 0;
    private int absentTime = 0;
    private int pinRequiredTime = 0;
    private int pukRequiredTime = 0;
    private int netWorkLockTime = 0;
    /**network test result params**/
    private int inServiceTime = 0;
    private int outServiceTime = 0;
    private int powerOffTime = 0;
    private int emergencyTime = 0;
    /**call test result params**/
    private int callSuccessTime = 0;
    private int callFailedTime = 0;
    /**air mode test result params**/
    private int airModeSuccessTime = 0;
    private int airModeFailedTime = 0;
    /**sms test result params**/
    private int smsSuccessTime = 0;
    private int smsFailedTime = 0;
    /**ps test result params**/
    private int psSuccessTime = 0;
    private int psFailedTime = 0;
    /**module reboot test result params**/
    private int moduleRebootSuccessTime = 0;
    private int moduleRebootFailedTime = 0;
    private int waitTime;
    private int durationTime;
    private int waitResultTime;
    private String callPhone;
    private String smsPhone;
    private String smsString;
    private String storeCombinationResultDir;
    private String networkTestModule = null;
    private String command = "AT+CFUN?\r\n";
    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;
    private CombinationTestBinder combinationTestBinder = new CombinationTestBinder();
    private AtSender atSender;
    private RebootTestService.RebootTestBinder rebootTestBinder;
    private SimTestService.SimTestBinder simTestBinder;
    private NetworkTestService.NetworkTestBinder networkTestBinder;
    private CallTestService callTestService;
    private SmsTestService smsTestService;
    private AirModeTestService.AirModeTestBinder airModeTestBinder;
    private PsTestService.PsTestBinder psTestBinder;
    private ModuleRebootService.ModuleRebootBinder moduleRebootBinder;

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
                        new CombinationTestThread().start();
                    }else {
                        String[] lines = (String[]) msg.obj;
                        for (String s : lines){
                            Log.d(TAG, "handleMessage: s = " + s);
                            if (s.contains("+CFUN")){
                                rebootSuccessTime++;
                                Log.d(TAG, "handleMessage: the device reboot success, rebootSuccessTime = " + rebootSuccessTime);
                                new CombinationTestThread().start();
                                return false;
                            }
                        }

                        rebootFailedTime++;
                        Log.d(TAG, "handleMessage: the device reboot failed, rebootFailedTime = " + rebootFailedTime);
                        new CombinationTestThread().start();
                    }
                    break;

                case WIFI_DISCONNECTED:
                    Toast.makeText(getApplicationContext(), getString(R.string.text_wifi_disconnected), Toast.LENGTH_SHORT).show();
                    break;

                case FTP_SERVER_CONNECT_FAILED:
                    Toast.makeText(getApplicationContext(), getString(R.string.text_connect_ftp_server_failed), Toast.LENGTH_SHORT).show();
                    break;

                case IN_SERVICE_TIMEOUT:
                    Log.d(TAG, "handleMessage: IN_SERVICE_TIMEOUT, call test or sms test failTimes+1");
                    isStartCallTest = true;
                    isTestFailed = true;
                    if (mHandler != null || inServiceTimeout != null){
                        mHandler.removeCallbacks(inServiceTimeout);
                    }
                    break;

                default:
                    atSender.handleMessage(msg);
                    break;
            }
            return false;
        }
    });

    /**bind the signal Instance Test Service**/
    ServiceConnection rebootConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rebootTestBinder = (RebootTestService.RebootTestBinder) service;
            Log.d(TAG, "onServiceConnected: bind the RebootTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rebootTestBinder = null;
        }
    };

    ServiceConnection simConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            simTestBinder = (SimTestService.SimTestBinder) service;
            Log.d(TAG, "onServiceConnected: bind the SimTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            simTestBinder = null;
        }
    };

    ServiceConnection networkConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            networkTestBinder = (NetworkTestService.NetworkTestBinder) service;
            Log.d(TAG, "onServiceConnected: bind the NetworkTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            networkTestBinder = null;
        }
    };

    ServiceConnection callConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            callTestService = (CallTestService) ((AutoTestService.ServiceBinder)service).getService();
            Log.d(TAG, "onServiceConnected: bind the CallTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            callTestService = null;
        }
    };

    ServiceConnection airModeConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            airModeTestBinder = (AirModeTestService.AirModeTestBinder) service;
            Log.d(TAG, "onServiceConnected: bind the AirModeTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            airModeTestBinder = null;
        }
    };

    ServiceConnection smsConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            smsTestService = (SmsTestService) ((AutoTestService.ServiceBinder)service).getService();
            Log.d(TAG, "onServiceConnected: bind the SmsTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            smsConnect = null;
        }
    };

    ServiceConnection psConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            psTestBinder = (PsTestService.PsTestBinder) service;
            Log.d(TAG, "onServiceConnected: bind the PsTestService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            psTestBinder = null;
        }
    };

    ServiceConnection moduleConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            moduleRebootBinder = (ModuleRebootService.ModuleRebootBinder) service;
            Log.d(TAG, "onServiceConnected: bind the ModuleRebootService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            moduleRebootBinder = null;
        }
    };

    /**bind the signal Test Instance Service**/
    private void bindSignalTestService(){
        Intent rebootIntent = new Intent(this, RebootTestService.class);
        bindService(rebootIntent, rebootConnect, Context.BIND_AUTO_CREATE);
        Intent simIntent = new Intent(this, SimTestService.class);
        bindService(simIntent, simConnect, Context.BIND_AUTO_CREATE);
        Intent networkIntent = new Intent(this, NetworkTestService.class);
        bindService(networkIntent, networkConnect, Context.BIND_AUTO_CREATE);
        Intent callIntent = new Intent(this, CallTestService.class);
        bindService(callIntent, callConnect, Context.BIND_AUTO_CREATE);
        Intent airModeIntent = new Intent(this, AirModeTestService.class);
        bindService(airModeIntent, airModeConnect, Context.BIND_AUTO_CREATE);
        Intent smsIntent = new Intent(this, SmsTestService.class);
        bindService(smsIntent, smsConnect, Context.BIND_AUTO_CREATE);
        Intent psIntent = new Intent(this, PsTestService.class);
        bindService(psIntent, psConnect, Context.BIND_AUTO_CREATE);
        Intent moduleIntent = new Intent(this, ModuleRebootService.class);
        bindService(moduleIntent, moduleConnect, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: CombinationTestService is Start");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        bindSignalTestService();
        getTestParams();
        if (isInTesting || isPaused){

            if (rebootIsChecked){
                atSender = new AtSender(this, mHandler);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Constant.openTTLog();
                        if (Constant.isWifiConnected(getApplicationContext())){
                            Log.d(TAG, "run: wifi have been connected, jude the ftp server weather be login");
                            if (Constant.isUpload(getApplicationContext())){
                                Constant.readAndUploadTTLog(Constant.getTestResultFileName(storeCombinationResultDir), getApplicationContext());
                            }else {
                                mHandler.sendEmptyMessage(FTP_SERVER_CONNECT_FAILED);
                                Constant.readTTLog(Constant.getTestResultFileName(storeCombinationResultDir));
                            }
                        }else {
                            Log.d(TAG, "run: wifi haven't been connected, don't upload the file to ftp server");
                            mHandler.sendEmptyMessage(WIFI_DISCONNECTED);
                            Constant.readTTLog(Constant.getTestResultFileName(storeCombinationResultDir));
                        }
                        Message message = mHandler.obtainMessage(SEND_JUDEGE_BOOT_MESSAGE);
                        int sendResult = atSender.sendATCommand(command, message, false);
                        if (sendResult == -1){
                            Log.d(TAG, "onCreate: open the device dev/STTYEMS42 failed, The Test was suspend, please Check the device, then reStart the CombinationTest");
                            isInTesting = false;
                            Constant.closeTTLog();
                            resetTestValue();
                            saveTmpTestResult();
                        }else {
                            Log.d(TAG, "onCreate: send the AT Code: AT+CFUN?");
                        }
                    }
                }).start();
            }else {
                try {
                    Thread.sleep(4 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new CombinationTestThread().start();
            }

//            atSender = new AtSender(this, mHandler);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    Constant.openTTLog();
//                    if (Constant.isWifiConnected(getApplicationContext())){
//                        Log.d(TAG, "run: wifi have been connected, jude the ftp server weather be login");
//                        if (Constant.isUpload(getApplicationContext())){
//                            Constant.readAndUploadTTLog(Constant.getTestResultFileName(storeCombinationResultDir), getApplicationContext());
//                        }else {
//                            mHandler.sendEmptyMessage(FTP_SERVER_CONNECT_FAILED);
//                            Constant.readTTLog(Constant.getTestResultFileName(storeCombinationResultDir));
//                        }
//                    }else {
//                        Log.d(TAG, "run: wifi haven't been connected, don't upload the file to ftp server");
//                        mHandler.sendEmptyMessage(WIFI_DISCONNECTED);
//                        Constant.readTTLog(Constant.getTestResultFileName(storeCombinationResultDir));
//                    }
//                    Message message = mHandler.obtainMessage(SEND_JUDEGE_BOOT_MESSAGE);
//                    int sendResult = atSender.sendATCommand(command, message, false);
//                    if (sendResult == -1){
//                        Log.d(TAG, "onCreate: open the device dev/STTYEMS42 failed, The Test was suspend, please Check the device, then reStart the CombinationTest");
//                        isInTesting = false;
//                        Constant.closeTTLog();
//                        resetTestValue();
//                        saveTmpTestResult();
//                    }else {
//                        Log.d(TAG, "onCreate: send the AT Code: AT+CFUN?");
//                    }
//                }
//            }).start();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return combinationTestBinder;
    }

    class CombinationTestBinder extends Binder{
        public void startTest(Bundle bundle){
            resetTestValue();
            if (initTestParams(bundle) == 0){
                storeCombinationResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
                Log.d(TAG, "startTest: Create the storePsTestResultDir success : " + storeCombinationResultDir);
                new CombinationTestThread().start();
            }else {
                Log.d(TAG, "startTest: the test parmas have invalid parameter, can't start the test");
            }
        }

        public void stopTest(){
            isInTesting = false;
            isStartTest = false;
            testTime = 0;
            isReboot = false;
            isPaused = true;
            saveTmpTestResult();
            Log.d(TAG, "stopTest: isInTesting = " + isInTesting);
        }

        public boolean isInTesting(){
            return isInTesting;
        }
    }

    class CombinationTestThread extends Thread{
        CombinationTestThread(){
            super();
        }
        @Override
        public void run() {
            super.run();
            Log.d(TAG, "run: start combination Test");
            mWakeLock.acquire();
            isInTesting = true;
            Constant.openTTLog();
            if (Constant.isWifiConnected(getApplicationContext())){
                Log.d(TAG, "run: wifi have been connected, jude the ftp server weather be login");
                if (Constant.isUpload(getApplicationContext())){
                    Constant.readAndUploadTTLog(Constant.getTestResultFileName(storeCombinationResultDir), getApplicationContext());
                }else {
                    mHandler.sendEmptyMessage(FTP_SERVER_CONNECT_FAILED);
                    Constant.readTTLog(Constant.getTestResultFileName(storeCombinationResultDir));
                }
            }else {
                Log.d(TAG, "run: wifi haven't been connected, don't upload the file to ftp server");
                mHandler.sendEmptyMessage(WIFI_DISCONNECTED);
                Constant.readTTLog(Constant.getTestResultFileName(storeCombinationResultDir));
            }
            runLogical();
            if (mWakeLock != null && mWakeLock.isHeld()){
                mWakeLock.release();
            }
            isInTesting = false;
            Constant.closeTTLog();
            resetTestValue();
            saveTmpTestResult();
            showResultActivity(CombinationTestActivity.class);
            Log.d(TAG, "run: finished the test, then will show you the Ps Test Result");
        }
    }

    private void runLogical(){
        Log.d(TAG, "runLogical: start the Combination Test");
        while (testTime > 0 && isInTesting){
            totalRunTime++;
            Log.d(TAG, "runLogical: totalRunTime = " + totalRunTime);
            testTime = testTime -1;
            Log.d(TAG, "runLogical: testTime = " + testTime + "   isInTesting = " + isInTesting);
            continueTest = true;
            isReboot = false;
            if (networkIsChecked){
                continueTest = false;
                if (networkTestModule.equals(getString(R.string.text_reboot_device))){
                    if (testTime > 0){
                        isReboot = true;
                    }else {
                        isReboot = false;
                    }
                }
                Log.d(TAG, "runLogical: ==============start networkTest=============");
                networkTestBinder.startOneTest(storeCombinationResultDir, networkTestModule, new NetworkTestCallback() {
                    @Override
                    public void testResultCallback(boolean isFinished, int netInServiceTime, int netOutServiceTime, int netEmergencyTime, int netPowerOffTime) {
                        inServiceTime = inServiceTime + netInServiceTime;
                        outServiceTime = outServiceTime + netOutServiceTime;
                        emergencyTime = emergencyTime + netEmergencyTime;
                        powerOffTime = powerOffTime + netPowerOffTime;
                        continueTest = isFinished;
                    }
                });
            }
            waitTestFinished();

            if (simIsChecked){
                continueTest = false;
                if (testTime > 0){
                    isReboot = true;
                }else {
                    isReboot = false;
                }
                try {
                    Thread.sleep(4 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "runLogical: ==============start simTest=============");
                simTestBinder.startOneTest(storeCombinationResultDir, new SimTestCallback() {
                    @Override
                    public void testResultCallback(boolean isFinished, int simAbsentTime, int simUnknownTime, int simPinRequiredTime, int simPukRequiredTime, int simReadyTime, int simNetworkLockTime) {
                        absentTime = absentTime + simAbsentTime;
                        unknownTime = unknownTime + simUnknownTime;
                        pinRequiredTime = pinRequiredTime + simPinRequiredTime;
                        pukRequiredTime = pukRequiredTime + simPukRequiredTime;
                        readyTime = readyTime + simReadyTime;
                        netWorkLockTime = netWorkLockTime + simNetworkLockTime;
                        continueTest = isFinished;
                    }
                });
            }
            waitTestFinished();

            if (callIsChecked){
                continueTest = false;
                isStartCallTest = false;
                isTestFailed = false;
                Log.d(TAG, "runLogical: ==============start callTest=============");
                Log.d(TAG, "runLogical: isInService = " + networkTestBinder.isInService());
                mHandler.postDelayed(inServiceTimeout, 120 * 1000);
                while (!isStartCallTest){
                    if (networkTestBinder.isInService()){
                        isStartCallTest = true;
                        if (mHandler != null || inServiceTimeout != null){
                            mHandler.removeCallbacks(inServiceTimeout);
                        }
                    }else {
                        try {
                            Thread.sleep(2 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                Log.d(TAG, "runLogical: isTestFailed = " + isTestFailed);
                if (isTestFailed){
                    callFailedTime = callFailedTime + 1;
                    continueTest = true;
                }else {
                    callTestService.startOneCallTest(storeCombinationResultDir, callPhone, waitTime, durationTime, new NormalTestCallback() {
                        @Override
                        public void testResultCallback(boolean isFinished, int successTime, int failedTime) {
                            callSuccessTime = callSuccessTime + successTime;
                            callFailedTime = callFailedTime + failedTime;
                            continueTest = isFinished;
                        }
                    });
                }
            }
            waitTestFinished();

            if (smsIsChecked){
                continueTest = false;
                isStartCallTest = false;
                isTestFailed = false;
                Log.d(TAG, "runLogical: ==============start smsTest=============");
                Log.d(TAG, "runLogical: isInService = " + networkTestBinder.isInService());
                mHandler.postDelayed(inServiceTimeout, 120 * 1000);
                while (!isStartCallTest){
                    if (networkTestBinder.isInService()){
                        isStartCallTest = true;
                        if (mHandler != null || inServiceTimeout != null){
                            mHandler.removeCallbacks(inServiceTimeout);
                        }
                    }else {
                        try {
                            Thread.sleep(2 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Log.d(TAG, "runLogical: isTestFailed = " + isTestFailed);
                if (isTestFailed){
                    smsFailedTime = smsFailedTime + 1;
                    continueTest = true;
                }else {
                    smsTestService.startOneSmsTest(storeCombinationResultDir, smsPhone, waitResultTime, smsString, new NormalTestCallback() {
                        @Override
                        public void testResultCallback(boolean isFinished, int successTime, int failedTime) {
                            smsSuccessTime = smsSuccessTime + successTime;
                            smsFailedTime = smsFailedTime + failedTime;
                            continueTest =  isFinished;
                        }
                    });
                }
            }
            waitTestFinished();

            if (psIsChecked){
                continueTest = false;
                Log.d(TAG, "runLogical: ==============start psTest=============");
                psTestBinder.startOneTest(storeCombinationResultDir, new NormalTestCallback() {
                    @Override
                    public void testResultCallback(boolean isFinished, int successTime, int failedTime) {
                        psSuccessTime = psSuccessTime + successTime;
                        psFailedTime = psFailedTime + failedTime;
                        continueTest = isFinished;
                    }
                });
            }
            waitTestFinished();

            if (airModeIsChecked){
                continueTest = false;
                Log.d(TAG, "runLogical: ==============start airModeTest=============");
                airModeTestBinder.startOneTest(storeCombinationResultDir, new NormalTestCallback() {
                    @Override
                    public void testResultCallback(boolean isFinished, int successTime, int failedTime) {
                        airModeSuccessTime = airModeSuccessTime + successTime;
                        airModeFailedTime = airModeFailedTime + failedTime;
                        continueTest = isFinished;
                    }
                });
            }
            waitTestFinished();

            if (moduleRebootIsChecked){
                continueTest = false;
                Log.d(TAG, "runLogical: ==============start moduleRebootTest=============");
                moduleRebootBinder.startOneTest(storeCombinationResultDir, new NormalTestCallback() {
                    @Override
                    public void testResultCallback(boolean isFinished, int successTime, int failedTime) {
                        moduleRebootSuccessTime = moduleRebootSuccessTime + successTime;
                        moduleRebootFailedTime = moduleRebootFailedTime + failedTime;
                        continueTest = isFinished;
                    }
                });
            }
            waitTestFinished();

            if (rebootIsChecked){
                continueTest = false;
                Log.d(TAG, "runLogical: ==============start rebootTest=============");
                saveTmpTestResult();
                try {
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (powerManager != null){
                    powerManager.reboot("Combination Test Reboot");
                }
            }

            if (isReboot){
                if (isPaused){
                    Log.d(TAG, "runLogical: paused the test, don't reboot");
                }else {
                    Log.d(TAG, "runLogical: the device will reboot and the start the next test");
                    saveTmpTestResult();
                    try {
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (powerManager != null){
                        powerManager.reboot("Combination Test Reboot");
                    }
                }
            }
        }
        saveCombinationTestResult();
    }

    private void waitTestFinished(){
        while (!continueTest){
            try {
                Thread.sleep( 500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    Runnable inServiceTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: wait the inService time, the call test or sms test failed");
            mHandler.sendEmptyMessage(IN_SERVICE_TIMEOUT);
        }
    };

    private int initTestParams(Bundle bundle){
        testTime = bundle.getInt(getString(R.string.key_test_times), 0);
        rebootIsChecked = bundle.getBoolean(getString(R.string.key_reboot_is_checked), false);
        simIsChecked = bundle.getBoolean(getString(R.string.key_sim_is_checked), false);
        networkIsChecked = bundle.getBoolean(getString(R.string.key_network_is_checked), false);
        callIsChecked = bundle.getBoolean(getString(R.string.key_call_is_checked), false);
        airModeIsChecked = bundle.getBoolean(getString(R.string.key_air_mode_is_checked), false);
        smsIsChecked = bundle.getBoolean(getString(R.string.key_sms_is_checked), false);
        psIsChecked = bundle.getBoolean(getString(R.string.key_ps_is_checked), false);
        moduleRebootIsChecked = bundle.getBoolean(getString(R.string.key_module_reboot_is_checked), false);

        if (networkIsChecked){
            networkTestModule = bundle.getString(getString(R.string.key_network_test_module), null);
        }else {
            networkTestModule = null;
        }

        if (callIsChecked){
            callPhone = bundle.getString(getString(R.string.key_call_phone), null);
            waitTime = bundle.getInt(getString(R.string.key_wait_time), 0);
            durationTime = bundle.getInt(getString(R.string.key_duration_time), 0);
            if (!PhoneNumberUtils.isGlobalPhoneNumber(callPhone)){
                Toast.makeText(getApplicationContext(), getString(R.string.text_phone_null), Toast.LENGTH_SHORT).show();
                return -1;
            }
        }else {
            callPhone = null;
            waitTime = 0;
            durationTime = 0;
        }

        if (smsIsChecked){
            smsPhone = bundle.getString(getString(R.string.key_sms_phone), null);
            waitResultTime = bundle.getInt(getString(R.string.key_wait_sms_result_time), 0);
            smsString = bundle.getString(getString(R.string.key_sms_string), null);
            if (!PhoneNumberUtils.isGlobalPhoneNumber(smsPhone)){
                Toast.makeText(getApplicationContext(), getString(R.string.text_phone_null), Toast.LENGTH_SHORT).show();
                return -1;
            }
            if (TextUtils.isEmpty(smsString)){
                Toast.makeText(getApplicationContext(), getString(R.string.text_send_content_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }
        }else {
            smsPhone = null;
            waitResultTime = 0;
            smsString = null;
        }

        if (rebootIsChecked || simIsChecked || networkIsChecked || callIsChecked || airModeIsChecked || smsIsChecked || psIsChecked || moduleRebootIsChecked){
           return 0;
        }else {
            Toast.makeText(getApplicationContext(), getString(R.string.text_combination_is_all_uncheck), Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    private boolean isOnlySimTest(){
        if (simIsChecked){
            if (rebootIsChecked || networkIsChecked || callIsChecked || airModeIsChecked || smsIsChecked || psIsChecked || moduleRebootIsChecked){
                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    private boolean isOnlyNetworkTest(){
        if (networkIsChecked){
            if (rebootIsChecked || simIsChecked || callIsChecked || airModeIsChecked || smsIsChecked || psIsChecked || moduleRebootIsChecked){
                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    private void saveCombinationTestResult(){
        Log.d(TAG, "saveCombinationTestResult: Start save the CombinationTestResult");
        File file = new File(storeCombinationResultDir + "/" + "testResult");
        Log.d(TAG, "saveCombinationTestResult: get the storePsTestResultDir = " + storeCombinationResultDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveCombinationTestResult: =====Created the Combination Test Result File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveCombinationTestResult: =====Created the Combination Test Result File Failed");
                e.printStackTrace();
            }
        }

        StringBuilder testResultBuilder = new StringBuilder();
        testResultBuilder.append("\r\n" + getString(R.string.text_result));
        testResultBuilder.append("\r\n");
        testResultBuilder.append("\r\n" + getString(R.string.text_test_times) + totalRunTime);
        testResultBuilder.append("\r\n");
        if (rebootIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_reboot_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_reboot_success_time) + rebootSuccessTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_reboot_failed_time) + rebootFailedTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_reboot_test_result));
            testResultBuilder.append("\r\n");
        }
        if (simIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_unknown) + unknownTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_absent) + absentTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_ready) + readyTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_pin_required) + pinRequiredTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_puk_required) + pukRequiredTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_state_netWork_locked) + netWorkLockTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sim_test_result));
            testResultBuilder.append("\r\n");
        }
        if (networkIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_network_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_in_service_time) + inServiceTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_out_of_service_time) + outServiceTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_emergency_time) + emergencyTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_power_off_time) + powerOffTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_network_test_result));
            testResultBuilder.append("\r\n");
        }

        if (callIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_call_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_succeed_times) + callSuccessTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_failed_times) + callFailedTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_call_test_result));
            testResultBuilder.append("\r\n");
        }

        if (airModeIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_switch_air_mode_success_time) + airModeSuccessTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_switch_air_mode_failed_time) + airModeFailedTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_air_mode_test_result));
            testResultBuilder.append("\r\n");
        }

        if (smsIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_sms_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_succeed_times) + smsSuccessTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_failed_times) + smsFailedTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_sms_test_result));
            testResultBuilder.append("\r\n");
        }

        if (psIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_ps_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_switch_succeed_time) + psSuccessTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_switch_failed_time) + psFailedTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_ps_test_result));
            testResultBuilder.append("\r\n");
        }

        if (moduleRebootIsChecked){
            testResultBuilder.append("\r\n" + getString(R.string.text_module_reboot_test_result));
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_switch_succeed_time) + moduleRebootSuccessTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_switch_failed_time) + moduleRebootFailedTime);
            testResultBuilder.append("\r\n");
            testResultBuilder.append("\r\n" + getString(R.string.text_module_reboot_test_result));
            testResultBuilder.append("\r\n");
        }

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(testResultBuilder.toString());
            Log.d(TAG, "saveCombinationTestResult: Save the CombinationTestResult Succeed");
        } catch (IOException e) {
            Log.d(TAG, "saveCombinationTestResult: Save the CombinationTestResult Failed");
            e.printStackTrace();
        }finally {
            if (bufferedWriter != null){
                try {
                    bufferedWriter.close();
                    fileWriter.close();
                } catch (IOException e) {
                    Log.d(TAG, "saveCombinationTestResult: close the bufferWriter or fileWriter failed");
                    e.printStackTrace();
                }
            }
        }
    }

    private void getTestParams(){
        Properties properties = Constant.loadTestParameter(this, COMBINATION_TEST_PARAMS_SAVE_PATH);
        testTime = Integer.parseInt(properties.getProperty(getString(R.string.key_test_times), "0"));
        storeCombinationResultDir = properties.getProperty(getString(R.string.key_test_result_path), null);
        isStartTest = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_start_test), "false"));
        isInTesting = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_testing), "false"));
        isPaused = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_paused), "false"));
        totalRunTime = Integer.parseInt(properties.getProperty(getString(R.string.key_total_run_time), "0"));
        isReboot = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_reboot), "false"));
        Log.d(TAG, "getTestParams: testTime = " + testTime + "  isReboot = " + isReboot + "   isStartTest = " + isStartTest + "   isInTesting = " + isInTesting + "   isPaused = " + isPaused + "    storeCombinationResultDir = " + storeCombinationResultDir);


        rebootIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_reboot_is_checked), "false"));
        simIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_sim_is_checked), "false"));
        networkIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_network_is_checked), "false"));
        callIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_call_is_checked), "false"));
        airModeIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_air_mode_is_checked), "false"));
        smsIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_sms_is_checked), "false"));
        psIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_ps_is_checked), "false"));
        moduleRebootIsChecked = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_module_reboot_is_checked), "false"));

        if (networkIsChecked){
            networkTestModule = properties.getProperty(getString(R.string.key_network_test_module), null);
        }else {
            networkTestModule = null;
        }
        
        if (callIsChecked){
            callPhone = properties.getProperty(getString(R.string.key_call_phone), "10086");
            waitTime = Integer.parseInt(properties.getProperty(getString(R.string.key_wait_time), "0"));
            durationTime = Integer.parseInt(properties.getProperty(getString(R.string.key_duration_time), "0"));
        }else {
            callPhone = null;
            waitTime = 0;
            durationTime = 0;
        }
        
        if (smsIsChecked){
            smsPhone = properties.getProperty(getString(R.string.key_sms_phone), "10086");
            waitResultTime = Integer.parseInt(properties.getProperty(getString(R.string.key_wait_sms_result_time), "10"));
            smsString = properties.getProperty(getString(R.string.key_sms_string), "Hello World");
        }else {
            smsPhone = null;
            waitResultTime = 0;
            smsString = null;
        }

        /**get the result params**/
        rebootSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_success_time), "0"));
        rebootFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_reboot_failed_time), "0"));

        absentTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_absent_times), "0"));
        unknownTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_unknown_times), "0"));
        netWorkLockTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_netWork_times), "0"));
        pinRequiredTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_pin_times), "0"));
        pukRequiredTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_puk_times), "0"));
        readyTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sim_ready_times), "0"));

        inServiceTime = Integer.parseInt(properties.getProperty(getString(R.string.key_network_in_service_time), "0"));
        outServiceTime = Integer.parseInt(properties.getProperty(getString(R.string.key_network_out_service_time), "0"));
        emergencyTime = Integer.parseInt(properties.getProperty(getString(R.string.key_network_emergency_time), "0"));
        powerOffTime = Integer.parseInt(properties.getProperty(getString(R.string.key_network_power_off_time), "0"));
        Log.d(TAG, "getTestParams: inServiceTime = " + inServiceTime + "  outServiceTime = " + outServiceTime);

        callSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_call_success_time), "0"));
        callFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_call_failed_time), "0"));

        airModeSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_air_mode_success_time), "0"));
        airModeFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_air_mode_failed_time), "0"));

        smsSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sms_success_time), "0"));
        smsFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_sms_failed_time), "0"));

        psSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_ps_success_time), "0"));
        psFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_ps_failed_time), "0"));

        moduleRebootSuccessTime = Integer.parseInt(properties.getProperty(getString(R.string.key_module_reboot_success_time), "0"));
        moduleRebootFailedTime = Integer.parseInt(properties.getProperty(getString(R.string.key_module_reboot_failed_time), "0"));
    }

    private void saveTmpTestResult(){
        Properties properties = new Properties();
        String fileDir = getFilesDir().getAbsolutePath() + "/" + COMBINATION_TEST_PARAMS_SAVE_PATH;
        File file = new File(fileDir);
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveTmpTestResult: Create the tmp test Result file success");
            }catch (IOException e){
                Log.d(TAG, "saveTmpTestResult: Create the tmp test Result file Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(file);
            /**test status tmp save**/
            properties.setProperty(getString(R.string.key_test_times), String.valueOf(testTime));
            properties.setProperty(getString(R.string.key_test_result_path), storeCombinationResultDir);
            properties.setProperty(getString(R.string.key_is_start_test), String.valueOf(isStartTest));
            properties.setProperty(getString(R.string.key_is_testing), String.valueOf(isInTesting));
            properties.setProperty(getString(R.string.key_total_run_time), String.valueOf(totalRunTime));
            properties.setProperty(getString(R.string.key_is_paused), String.valueOf(isPaused));
            properties.setProperty(getString(R.string.key_is_reboot), String.valueOf(isReboot));
            /**test result tmp save**/
            properties.setProperty(getString(R.string.key_total_run_time), String.valueOf(totalRunTime));
            /**test isChecked save**/
            properties.setProperty(getString(R.string.key_reboot_is_checked), String.valueOf(rebootIsChecked));
            properties.setProperty(getString(R.string.key_sim_is_checked), String.valueOf(simIsChecked));
            properties.setProperty(getString(R.string.key_network_is_checked), String.valueOf(networkIsChecked));
            properties.setProperty(getString(R.string.key_call_is_checked), String.valueOf(callIsChecked));
            properties.setProperty(getString(R.string.key_air_mode_is_checked), String.valueOf(airModeIsChecked));
            properties.setProperty(getString(R.string.key_sms_is_checked), String.valueOf(smsIsChecked));
            properties.setProperty(getString(R.string.key_ps_is_checked), String.valueOf(psIsChecked));
            properties.setProperty(getString(R.string.key_module_reboot_is_checked), String.valueOf(moduleRebootIsChecked));

            /**save test params**/
            if (networkIsChecked){
                properties.setProperty(getString(R.string.key_network_test_module), networkTestModule);
            }
            if (callIsChecked){
                properties.setProperty(getString(R.string.key_call_phone), callPhone);
                properties.setProperty(getString(R.string.key_wait_time), String.valueOf(waitTime));
                properties.setProperty(getString(R.string.key_duration_time), String.valueOf(durationTime));
            }
            if (smsIsChecked){
                properties.setProperty(getString(R.string.key_sms_phone), smsPhone);
                properties.setProperty(getString(R.string.key_wait_sms_result_time), String.valueOf(waitResultTime));
                properties.setProperty(getString(R.string.key_sms_string), smsString);
            }

            /**save result params**/
            properties.setProperty(getString(R.string.key_reboot_success_time), String.valueOf(rebootSuccessTime));
            properties.setProperty(getString(R.string.key_reboot_failed_time), String.valueOf(rebootFailedTime));
            properties.setProperty(getString(R.string.key_sim_unknown_times), String.valueOf(unknownTime));
            properties.setProperty(getString(R.string.key_sim_ready_times), String.valueOf(readyTime));
            properties.setProperty(getString(R.string.key_sim_netWork_times), String.valueOf(netWorkLockTime));
            properties.setProperty(getString(R.string.key_sim_pin_times), String.valueOf(pinRequiredTime));
            properties.setProperty(getString(R.string.key_sim_puk_times), String.valueOf(pukRequiredTime));
            properties.setProperty(getString(R.string.key_sim_absent_times), String.valueOf(absentTime));
            properties.setProperty(getString(R.string.key_network_in_service_time), String.valueOf(inServiceTime));
            properties.setProperty(getString(R.string.key_network_out_service_time), String.valueOf(outServiceTime));
            properties.setProperty(getString(R.string.key_network_emergency_time), String.valueOf(emergencyTime));
            properties.setProperty(getString(R.string.key_network_power_off_time), String.valueOf(powerOffTime));
            properties.setProperty(getString(R.string.key_call_success_time), String.valueOf(callSuccessTime));
            properties.setProperty(getString(R.string.key_call_failed_time), String.valueOf(callFailedTime));
            properties.setProperty(getString(R.string.key_air_mode_success_time), String.valueOf(airModeSuccessTime));
            properties.setProperty(getString(R.string.key_air_mode_failed_time), String.valueOf(airModeFailedTime));
            properties.setProperty(getString(R.string.key_sms_success_time), String.valueOf(smsSuccessTime));
            properties.setProperty(getString(R.string.key_sms_failed_time), String.valueOf(smsFailedTime));
            properties.setProperty(getString(R.string.key_ps_success_time), String.valueOf(psSuccessTime));
            properties.setProperty(getString(R.string.key_ps_failed_time), String.valueOf(psFailedTime));
            properties.setProperty(getString(R.string.key_module_reboot_success_time), String.valueOf(moduleRebootSuccessTime));
            properties.setProperty(getString(R.string.key_module_reboot_failed_time), String.valueOf(moduleRebootFailedTime));

            properties.store(outputStream, "save the combination test tmp test result");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTmpTestResult: open combination test param file failed ");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "saveTmpTestResult: store the combination properties failed");
            e.printStackTrace();
        }

        Log.d(TAG, "saveTmpTestResult: Succeed save the tmp test result of Combination test");
    }

    private void showResultActivity(Class<?> resultActivity){
        Intent intent = new Intent();
        intent.setClass(this, resultActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getString(R.string.key_test_result_path), storeCombinationResultDir + "/" + "testResult" );
        startActivity(intent);
    }

    private void resetTestValue(){
        isStartTest = false;
        isInTesting = false;
        isPaused = false;
        isReboot = false;
        totalRunTime = 0;

        rebootSuccessTime = 0;
        rebootFailedTime = 0;
        unknownTime = 0;
        absentTime = 0;
        pinRequiredTime = 0;
        netWorkLockTime = 0;
        pukRequiredTime = 0;
        readyTime = 0;
        inServiceTime = 0;
        outServiceTime = 0;
        emergencyTime = 0;
        powerOffTime = 0;
        callSuccessTime = 0;
        callFailedTime = 0;
        airModeSuccessTime = 0;
        airModeFailedTime = 0;
        smsSuccessTime = 0;
        smsFailedTime = 0;
        psSuccessTime = 0;
        psFailedTime = 0;
        moduleRebootSuccessTime = 0;
        moduleRebootFailedTime = 0;
    }

    private void unbindSignalTestService(){
        if (rebootConnect != null){
            unbindService(rebootConnect);
        }
        if (simConnect != null){
            unbindService(simConnect);
        }
        if (networkConnect != null){
            unbindService(networkConnect);
        }
        if (callConnect != null){
            unbindService(callConnect);
        }
        if (airModeConnect != null){
            unbindService(airModeConnect);
        }
        if (smsConnect != null){
            unbindService(smsConnect);
        }
        if (psConnect != null){
            unbindService(psConnect);
        }
        if (moduleConnect != null){
            unbindService(moduleConnect);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()){
            mWakeLock.release();
        }
        unbindSignalTestService();
    }
}
