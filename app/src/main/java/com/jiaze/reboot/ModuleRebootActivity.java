package com.jiaze.reboot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jiaze.autotestapp.R;
import com.jiaze.common.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class ModuleRebootActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "ModuleRebootActivity";
    private static final String MODULE_TEST_PARAMS_SAVE_PATH = "ModuleTestParams";
    private static final int MODULE_STATE_CHANGED = 1;
    private static final int MSG_ID_TEST_FINISHED = 2;
    private static final int MSG_ID_GOT_TEST_RESULT = 3;

    private TextView tvModuleState;
    private EditText etTestTime;
    private TextView tvTestResult;
    private Button btnStart;
    private ModuleRebootService.ModuleRebootBinder moduleRebootBinder;
    private ModuleStateBroadcast moduleStateBroadcast;
    private IntentFilter intentFilter;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case MODULE_STATE_CHANGED:
                    tvModuleState.setText(msg.obj.toString());
                    break;
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(getString(R.string.btn_start_test));
                    tvModuleState.setText(moduleRebootBinder.getModuleState());
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
                    tvTestResult.setText((String) msg.obj);
                    break;
            }
            return false;
        }
    });

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            moduleRebootBinder = (ModuleRebootService.ModuleRebootBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the ModuleReboot Test Service");
            btnStart.setEnabled(true);
            tvModuleState.setText(moduleRebootBinder.getModuleState());
            getTestResult();
            if (moduleRebootBinder.isInTesting()){
                btnStart.setText(getString(R.string.btn_stop_test));
            }else {
                btnStart.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            moduleRebootBinder = null;
            btnStart.setEnabled(false);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_ID_TEST_FINISHED;
        msg.obj = intent.getStringExtra(getString(R.string.key_test_result_path));
        msg.sendToTarget();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_reboot);
        initUI();
        intentFilter = new IntentFilter("com.jiaze.action.MODULE_POWER_STATE_CHANGE");
        moduleStateBroadcast = new ModuleStateBroadcast();
        registerReceiver(moduleStateBroadcast, intentFilter);
        Log.d(TAG, "onCreate: register the ModuleStateBroadcast");
        bindModuleRebootTestService();
    }

    private void getTestResult(){
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.key_test_result_path))){
            Log.d(TAG, "getResultPath: get the module reboot test result and show it");
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            msg.obj = intent.getStringExtra(getString(R.string.key_test_result_path));
            msg.sendToTarget();
        }else {
            Log.d(TAG, "getResultPath: no module reboot test result need to show");
        }
    }

    private void bindModuleRebootTestService(){
        Intent intent = new Intent(this, ModuleRebootService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, MODULE_TEST_PARAMS_SAVE_PATH);
        String moduleTestTimes = properties.getProperty(getString(R.string.key_module_test_time), "1");
        tvModuleState = (TextView) findViewById(R.id.module_state);
        etTestTime = (EditText) findViewById(R.id.module_reboot_times);
        tvTestResult = (TextView) findViewById(R.id.module_reboot_test_result);
        btnStart = (Button) findViewById(R.id.module_reboot_btn);
        etTestTime.setText(moduleTestTimes);
        etTestTime.requestFocus();
        etTestTime.setSelection(etTestTime.getText().length());
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_module_test_time), Integer.parseInt(etTestTime.getText().toString()));
        return bundle;
    }

    private int saveTestParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + MODULE_TEST_PARAMS_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveTestParams: Create the Module Reboot Test Params Save File Succeed");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create the Module Reboot Test Params Save File Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();
            if (TextUtils.isEmpty(etTestTime.getText().toString())){
                Toast.makeText(this, getString(R.string.text_test_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }else {
                if (Integer.parseInt(etTestTime.getText().toString()) == 0){
                    Toast.makeText(this, getString(R.string.text_test_time_is_zero), Toast.LENGTH_SHORT).show();
                    return -1;
                }
            }
            properties.setProperty(getString(R.string.key_module_test_time), etTestTime.getText().toString());
            properties.store(outputStream, "ModuleRebootParams");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTestParams: Failed to Create FileOutputStream Create");
            e.printStackTrace();
            return -2;
        } catch (IOException e) {
            Log.d(TAG, "saveTestParams: Failed to store the properties to the File");
            e.printStackTrace();
            return -2;
        }

        return 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.module_reboot_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    if (saveTestParams() == 0){
                        saveTestParams();
                        moduleRebootBinder.startTest(getTestParameter());
                        btnStart.setText(getString(R.string.btn_stop_test));
                    }else {

                    }
                }else {
                    moduleRebootBinder.stopTest();
                    btnStart.setText(getString(R.string.btn_start_test));
                }
                break;
        }
    }

    class ModuleStateBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: receive the ModuleStateChangeBroadcast");
            if (intent.hasExtra("state")){
                Message msg = mHandler.obtainMessage();
                msg.what = MODULE_STATE_CHANGED;
                msg.obj = intent.getStringExtra("state");
                Log.d(TAG, "onReceive: get the mode power state : " + msg.obj.toString());
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
        if (moduleStateBroadcast != null){
            unregisterReceiver(moduleStateBroadcast);
        }
    }
}
