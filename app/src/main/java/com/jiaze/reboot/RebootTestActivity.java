package com.jiaze.reboot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jiaze.autotestapp.R;
import com.jiaze.common.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class RebootTestActivity extends Activity implements View.OnClickListener {

    private static final String REBOOT_TEST_PARAM_SAVE_PATH = "RebootTestParams";
    private static final String TAG = "RebootTestActivity";
    private static final int MSG_ID_TEST_FINISHED = 1;
    private static final int MSG_ID_GOT_TEST_RESULT = 2;

    private EditText etRebootTimes;
    private Button btnTest;
    private TextView tvRebootResult;
    private RebootTestService.RebootTestBinder rebootTestBinder;
    private IntentFilter intentFilter;
    private RebootTestFinishBroadcastReceiver rebootTestFinishBroadcastReceiver;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case MSG_ID_TEST_FINISHED:
                    btnTest.setText(getString(R.string.btn_start_test));
                    final String resultPath = (String) msg.obj;
                    Log.d(TAG, "handleMessage: finish the test, load the testResult from resultPath: " + resultPath);
                    final Message getResult = mHandler.obtainMessage();
                    getResult.what = MSG_ID_GOT_TEST_RESULT;
                    if (TextUtils.isEmpty(resultPath)){
                        Log.d(TAG, "handleMessage: the result dir isn't exist : " + resultPath);
                    }
                    Constant.showTestResult(resultPath, getResult);
                    break;

                case MSG_ID_GOT_TEST_RESULT:
                    Log.d(TAG, "handleMessage: testResult: " + msg.obj);
                    tvRebootResult.setText((String) msg.obj);
                    break;
            }
            return false;
        }
    });

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rebootTestBinder = (RebootTestService.RebootTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the RebootTestService Succeed");
            rebootTestBinder.isRegister(true);
            btnTest.setEnabled(true);
            if (rebootTestBinder.isInTesting()){
                btnTest.setText(getString(R.string.btn_stop_test));
            }else {
                btnTest.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rebootTestBinder = null;
            btnTest.setEnabled(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reboot_test);
        initUi();
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.jiaze.action.REBOOT_TEST_FINISHED");
        rebootTestFinishBroadcastReceiver = new RebootTestFinishBroadcastReceiver();
        registerReceiver(rebootTestFinishBroadcastReceiver, intentFilter);
        Log.d(TAG, "onCreate: register the RebootTestFinishBroadcastReceiver");
        bindRebootTestService();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.reboot_btn:
                if (btnTest.getText().equals(getString(R.string.btn_start_test))){
                    saveTestParams();
                    btnTest.setText(R.string.btn_stop_test);
                    rebootTestBinder.startTest(getTestParameter());
                }else {
                    btnTest.setText(R.string.btn_start_test);
                    rebootTestBinder.stopTest();
                }
                break;
        }
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_reboot_test_time), Integer.parseInt(etRebootTimes.getText().toString()));
        return bundle;
    }

    private void bindRebootTestService(){
        Intent intent = new Intent(this, RebootTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void initUi(){
        Properties properties = Constant.loadTestParameter(this, REBOOT_TEST_PARAM_SAVE_PATH);
        String testTime = properties.getProperty(getString(R.string.key_reboot_test_time), "1");
        etRebootTimes = (EditText) findViewById(R.id.reboot_times);
        btnTest = (Button) findViewById(R.id.reboot_btn);
        tvRebootResult = (TextView) findViewById(R.id.reboot_test_result);
        if (Integer.parseInt(testTime) == 0){
            etRebootTimes.setText(getString(R.string.reboot_default_value));
        }else {
            etRebootTimes.setText(testTime);
        }
        etRebootTimes.setText(testTime);
        etRebootTimes.requestFocus();
        etRebootTimes.setSelection(etRebootTimes.getText().length());
        btnTest.setOnClickListener(this);
        //在连接好RebootTestService时在将按键设为可点击，以防bug
        btnTest.setEnabled(false);
    }

    private void saveTestParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + REBOOT_TEST_PARAM_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create the Reboot Test Params File Failed");
                e.printStackTrace();
            }
        }
        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();
            properties.setProperty(getString(R.string.key_reboot_test_time), etRebootTimes.getText().toString());
            properties.setProperty(getString(R.string.key_is_reboot_testing), String.valueOf(false));
            try {
                properties.store(outputStream, "RebootParameter");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Failed to store the properties to the File");
                e.printStackTrace();
            }
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.d(TAG, "saveTestParams: Close OutputStream failed");
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTestParams: Create FileOutputStream Failed");
            e.printStackTrace();
        }
    }

    private class RebootTestFinishBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: receiver the Reboot Test Finished Receiver");
            if (intent.hasExtra(getString(R.string.key_result))){
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_ID_TEST_FINISHED;
                msg.obj = intent.getStringExtra(getString(R.string.key_result));
                Log.d(TAG, "onReceive: get the reboot test result path : "  + msg.obj.toString());
                msg.sendToTarget();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null){
            unbindService(connection);
        }

        if (rebootTestFinishBroadcastReceiver != null){
            unregisterReceiver(rebootTestFinishBroadcastReceiver);
        }
    }
}
