package com.jiaze.sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.jiaze.autotestapp.R;
import com.jiaze.common.AutoTestService;
import com.jiaze.common.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;


/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.sms
 * Create by jz-pf
 * on 2019/7/17
 * =========================================
 */
public class SmsTestService extends AutoTestService {

    public static final String TAG = "SmsTestService";
    private static final String TEST_PARAM = "SmsTest";
    private static final String SMS_SENT_ACTION = "com.jiaze.action.SMS_SENT_ACTION";
    private static final String SMS_DELIVERED_ACTION = "com.jiaze.action.SMS_DELIVERED_ACTION";
    public static final String SMS_TEST_PARAMS_SAVE_PATH = "SmsTestParams";

    public static final int MSG_ID_WAIT_RESULT_TIMEOUT = 1;
    public static final int MSG_ID_SMS_SENT_SUCCESS = 2;
    public static final int MSG_ID_SMS_SENT_FAILURE = 3;

    private String phoneNumber;
    private String smsBody;
    private SmsManager smsManager;
    private int waitTimeout = 0;
    private boolean isLongSms = false;
    private SmsSentResultReceiver smsSentResultReceiver = new SmsSentResultReceiver();
    private SmsDeliverBroadcastReceiver smsDeliverBroadcastReceiver = new SmsDeliverBroadcastReceiver();

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case MSG_ID_SMS_SENT_SUCCESS:
                    successCount++;
                    Log.d(TAG, "handleMessage: sent the message success, successTimes = " + successCount);
                    if (mHandler != null && waitTimeoutTask != null){
                        mHandler.removeCallbacks(waitTimeoutTask);
                    }
                    runNextTime = true;
                    break;

                case MSG_ID_SMS_SENT_FAILURE:
                    failedCount++;
                    Log.d(TAG, "handleMessage: send the sms failure, failedCount = " + failedCount);
                    if (mHandler != null && waitTimeoutTask != null){
                        mHandler.removeCallbacks(waitTimeoutTask);
                    }
                    runNextTime = true;
                    break;

                case MSG_ID_WAIT_RESULT_TIMEOUT:
                    failedCount++;
                    Log.d(TAG, "handleMessage: send the sms timeout, failedCount = " + failedCount);
                    if (mHandler != null && waitTimeoutTask != null){
                        mHandler.removeCallbacks(waitTimeoutTask);
                    }
                    runNextTime = true;
                    break;
            }
            return false;
        }
    });

    Runnable waitTimeoutTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: wait sms send result timeout, send the message");
            mHandler.sendEmptyMessage(MSG_ID_WAIT_RESULT_TIMEOUT);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: SmsTestService is Start");
        smsManager = SmsManager.getDefault();
        registerSmsDeliverReceiver();
//        getTestParams();
//        if (isStartTest){
//            Log.d(TAG, "onCreate: isStartTest is true, start test");
//            new TestThread().start();
//        }else {
//            Log.d(TAG, "onCreate: isStartTest is false, need not do anythings");
//        }
    }

    @Override
    public void stopTest() {
        isInTesting = false;
    }

    @Override
    protected void runTestLogic() {
        Log.d(TAG, "runTestLogic: start runLogical");
        registerSmsResultReceiver();
        startMmsApp();
        for (; (testTimes>totalRunTimes) && isInTesting;){
            totalRunTimes++;
            Log.d(TAG, "runTestLogic: smsBody = " + smsBody);
            if (isLongSms){
                Log.d(TAG, "runTestLogic: sendOneLongSms, send the long message");
                sendOneLongSms(phoneNumber, smsBody + totalRunTimes);
            }else {
                Log.d(TAG, "runTestLogic: sendOneSms, send the short message");
                sendOneSms(phoneNumber, smsBody + totalRunTimes);
            }
            runNextTime = false;
            mHandler.postDelayed(waitTimeoutTask, waitTimeout * 1000);
            while (!runNextTime){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            storeTestResult(SMS_TEST_RESULT_FILENAME);
        }
        unregisterSmsResultReceiver();
    }

    private void startMmsApp(){
        Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
        sendIntent.setData(Uri.parse("smsto:" + phoneNumber));
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sendIntent);
    }

    private void sendOneSms(String number, String smsString){
        Intent itSend = new Intent(SMS_SENT_ACTION);
        Intent itDeliver = new Intent(SMS_DELIVERED_ACTION);

        /**判断短信是否已经发送了**/
        PendingIntent sentPI = PendingIntent.getBroadcast(this.getApplicationContext(), 0, itSend, 0);

        /**如果短信被发送端接收了， 通知告诉短信发送端短信已经被接收**/
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this.getApplicationContext(), 0, itDeliver, 0);

        /**发送SMS短信, 注意倒数的两个PendingIntent参数**/
        smsManager.sendTextMessage(number, null, smsString, sentPI, deliveredPI);
    }

    private void sendOneLongSms(String number, String smsString){
        Intent itSend = new Intent(SMS_SENT_ACTION);
        Intent itDeliver = new Intent(SMS_DELIVERED_ACTION);

        /**判断短信是否已经发送了**/
        PendingIntent sentPI = PendingIntent.getBroadcast(this.getApplicationContext(), 0, itSend, 0);

        /**如果短信被发送端接收了， 通知告诉短信发送端短信已经被接收**/
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this.getApplicationContext(), 0, itDeliver, 0);

        ArrayList<String> smsArray = smsManager.divideMessage(smsString);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> recIntents = new ArrayList<PendingIntent>();

        for (int i = 0; i < smsArray.size(); i++){
            sentIntents.add(sentPI);
            recIntents.add(deliveredPI);
        }

        /**发送长短信**/
        smsManager.sendMultipartTextMessage(number, null, smsArray, sentIntents, recIntents);
    }

    class SmsSentResultReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: get the smsSentResultReceiver");
            int resultCode = getResultCode();
            try {
                switch (resultCode){
                    case Activity.RESULT_OK:
                        Log.d(TAG, "onReceive: phoneNumber = " + phoneNumber + "send the message success");
                        mHandler.sendEmptyMessage(MSG_ID_SMS_SENT_SUCCESS);
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.d(TAG, "onReceive: phoneNumber = " + phoneNumber + "send the message failure");
                        mHandler.sendEmptyMessage(MSG_ID_SMS_SENT_FAILURE);
                        break;

                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.d(TAG, "onReceive: RESULT_ERROR_RADIO_OFF");
                        break;

                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.d(TAG, "onReceive: RESULT_ERROR_NULL_PDU");
                        break;
                }
            }catch (Exception e){
                e.getStackTrace();
            }

        }
    }

    class SmsDeliverBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: receiver the SmsDeliverBroadcastReceiver, and receiver the message success");
            Toast.makeText(getApplicationContext(), "Receiver the Message", Toast.LENGTH_SHORT).show();
        }
    }


    private void registerSmsResultReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SMS_SENT_ACTION);
        registerReceiver(smsSentResultReceiver, intentFilter);
        Log.d(TAG, "registerSmsResultReceiver: register the smsSentResultReceiver");
    }

    private void unregisterSmsResultReceiver(){
        if (smsSentResultReceiver != null){
            Log.d(TAG, "unregisterSmsResultReceiver: unRegister the smsSentResultReceiver");
            unregisterReceiver(smsSentResultReceiver);
        }
    }

    private void registerSmsDeliverReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SMS_DELIVERED_ACTION);
        registerReceiver(smsDeliverBroadcastReceiver, intentFilter);
        Log.d(TAG, "registerSmsDeliverReceiver: register the smsDeliverBroadcastReceiver");
    }

    private void unregisterSmsDeliverReceiver(){
        if (smsDeliverBroadcastReceiver != null){
            Log.d(TAG, "unregisterSmsDeliverReceiver: unRegister the smsDeliverBroadcastReceiver");
            unregisterReceiver(smsDeliverBroadcastReceiver);
        }
    }

    @Override
    protected int initTestParams(Bundle bundle) {
        boolean isCombinationTest = bundle.getBoolean(getString(R.string.key_is_combination_test), false);
        if (!isCombinationTest){
            storeTestResultDir = Constant.createSaveTestResultPath(TEST_PARAM);
        }
        Log.d(TAG, "initTestParams: get the testResult dir = " + storeTestResultDir);
        testTimes = bundle.getInt(getString(R.string.key_test_times), 0);
        waitTimeout = bundle.getInt(getString(R.string.key_wait_sms_result_time), 20);
        phoneNumber = bundle.getString(getString(R.string.key_phone), null);
        smsBody = bundle.getString(getString(R.string.key_sms_string), "Hello World");
        Log.d(TAG, "initTestParams: phoneNumber = " + phoneNumber + "    smsBody = " + smsBody);
        if (!PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)){
            return -1;
        }

        if (smsBody.getBytes().length > 140){
            isLongSms = true;
        }else {
            isLongSms = false;
        }

        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSmsDeliverReceiver();
    }

    @Override
    protected Class<?> getResultActivity() {
        return SmsTestActivity.class;
    }

    @Override
    protected void saveTmpTestResult() {
        Properties properties = new Properties();
        String fileDir = getFilesDir().getAbsolutePath() + "/" + SMS_TEST_PARAMS_SAVE_PATH;
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
            properties.setProperty(getString(R.string.key_test_times), String.valueOf(testTimes));
            properties.setProperty(getString(R.string.key_phone), phoneNumber);
            properties.setProperty(getString(R.string.key_wait_sms_result_time), String.valueOf(waitTimeout));
            properties.setProperty(getString(R.string.key_sms_string), smsBody);
            properties.setProperty(getString(R.string.key_is_testing), String.valueOf(isInTesting));
            properties.setProperty(getString(R.string.key_is_start_test), String.valueOf(isStartTest));
            properties.setProperty(getString(R.string.key_test_result_path), storeTestResultDir);
            properties.store(outputStream, "save the sms test tmp test result");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTmpTestResult: open sms test param file failed ");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "saveTmpTestResult: store the sms properties failed");
            e.printStackTrace();
        }

        Log.d(TAG, "saveTmpTestResult: Succeed save the tmp test result of PS test");
    }

    @Override
    protected void getTestParams() {
        Properties properties = Constant.loadTestParameter(this, SMS_TEST_PARAMS_SAVE_PATH);
        testTimes = Integer.parseInt(properties.getProperty(getString(R.string.key_test_times), "0"));
        storeTestResultDir = properties.getProperty(getString(R.string.key_test_result_path), null);
        isStartTest = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_start_test), "false"));
        waitTimeout = Integer.parseInt(properties.getProperty(getString(R.string.key_wait_time), "0"));
        isInTesting = Boolean.parseBoolean(properties.getProperty(getString(R.string.key_is_testing), "false"));
        phoneNumber = properties.getProperty(getString(R.string.key_phone));
        smsBody = properties.getProperty(getString(R.string.key_sms_string));
        Log.d(TAG, "getTestParams: testTimes = " + testTimes + "   isStartTest = " + isStartTest + "   isInTesting = " + isInTesting);
        Log.d(TAG, "getTestParams: waitTimeout = " + waitTimeout + "   storeTestResultDir = " + storeTestResultDir + "   phoneNumber = " + phoneNumber);
        Log.d(TAG, "getTestParams: smsBody = " + smsBody);
    }
}
