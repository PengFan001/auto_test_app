package com.jiaze.network;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class NetworkTestActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "NetworkTestActivity";
    private static final String NETWORK_TEST_PARAMS_SAVE_PATH = "NetworkTestParam";
    private static final int MSG_ID_TEST_FINISHED = 2;
    private static final int MSG_ID_GOT_TEST_RESULT = 3;

    private TextView tvServiceState;
    private EditText etTestTime;
    private TextView tvTestResult;
    private Button btnStart;
    private RadioGroup rgTestModule;
    private RadioButton rbtnChecked;
    private NetworkTestService.NetworkTestBinder networkTestBinder;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(getString(R.string.btn_start_test));
                    tvServiceState.setText(networkTestBinder.getServiceState());
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
            networkTestBinder = (NetworkTestService.NetworkTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the NetworkTestService");
            btnStart.setEnabled(true);
            tvServiceState.setText(networkTestBinder.getServiceState());
            getTestResult();
//            networkTestBinder.isRegistered(true);
            if (networkTestBinder.isInTesting()){
                btnStart.setText(getString(R.string.btn_stop_test));
            }else {
                btnStart.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            networkTestBinder = null;
            btnStart.setEnabled(false);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(getString(R.string.key_test_result_path))){
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            msg.obj = intent.getStringExtra(getString(R.string.key_test_result_path));
            msg.sendToTarget();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);
        initUI();
        rgTestModule.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                rbtnChecked = (RadioButton) findViewById(checkedId);
                getSharedPreferences("test_module", MODE_PRIVATE).edit().putString(getString(R.string.key_network_test_module), rbtnChecked.getText().toString()).commit();
                Log.d(TAG, "onCheckedChanged: get the checked module = " + rbtnChecked.getText().toString());
            }
        });
       bindNetworkTestService();
    }

    private void bindNetworkTestService(){
        Intent intent = new Intent(this, NetworkTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void getTestResult(){
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.key_test_result_path))){
            Log.d(TAG, "getResultPath: get the network test result and show it");
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            msg.obj = intent.getStringExtra(getString(R.string.key_test_result_path));
            msg.sendToTarget();
        }else {
            Log.d(TAG, "getResultPath: no network test result need to show");
        }
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, NETWORK_TEST_PARAMS_SAVE_PATH);
        String networkTestTime = properties.getProperty(getString(R.string.key_network_test_time), "1");
        tvServiceState = (TextView) findViewById(R.id.service_state);
        rgTestModule = (RadioGroup) findViewById(R.id.network_test_module);
        String testType = getSharedPreferences("test_module", MODE_PRIVATE).getString(getString(R.string.key_network_test_module), getString(R.string.text_reboot_device));
        Log.d(TAG, "initUI: get the testType = " + testType);
        if (testType.equals(getString(R.string.text_reboot_radio))){
            rbtnChecked = (RadioButton) findViewById(R.id.reboot_radio);
            rbtnChecked.setChecked(true);
        }else if (testType.equals(getString(R.string.text_reboot_device))){
            rbtnChecked = (RadioButton) findViewById(R.id.reboot_device);
            rbtnChecked.setChecked(true);
        }
        etTestTime = (EditText) findViewById(R.id.network_test_time);
        tvTestResult = (TextView) findViewById(R.id.network_test_result);
        btnStart = (Button) findViewById(R.id.network_start_btn);
        if (Integer.parseInt(networkTestTime) == 0){
            etTestTime.setText(getString(R.string.reboot_default_value));
        }else {
            etTestTime.setText(networkTestTime);
        }
        etTestTime.requestFocus();
        etTestTime.setSelection(etTestTime.getText().length());
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_network_test_time), Integer.parseInt(etTestTime.getText().toString()));
        bundle.putString(getString(R.string.key_network_test_module), rbtnChecked.getText().toString());
        return bundle;
    }

    private int saveTestParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + NETWORK_TEST_PARAMS_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveTestParams: Create the NetWork Test Parameter File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create the NetWork Test Parameter File Failed");
                e.printStackTrace();
            }
        }
        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();
            Log.d(TAG, "saveTestParams: etTestTime = " + etTestTime.getText().toString());
            if (TextUtils.isEmpty(etTestTime.getText().toString())){
                Toast.makeText(this, getString(R.string.text_test_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }else {
                if (Integer.parseInt(etTestTime.getText().toString()) == 0){
                    Toast.makeText(this, getString(R.string.text_test_time_is_zero), Toast.LENGTH_SHORT).show();
                    return -1;
                }
            }
            properties.setProperty(getString(R.string.key_network_test_time), etTestTime.getText().toString());
            properties.store(outputStream, "NetWorkParameter");
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
            case R.id.network_start_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    if (saveTestParams() == 0){
                        networkTestBinder.startTest(getTestParameter());
                        btnStart.setText(getString(R.string.btn_stop_test));
                    }else {
                        Toast.makeText(this, getString(R.string.text_params_error), Toast.LENGTH_SHORT).show();
                    }

                }else {
                    networkTestBinder.stopTest();
                    btnStart.setText(getString(R.string.btn_start_test));
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null){
            unbindService(connection);
        }
    }
}
