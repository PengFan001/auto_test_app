package com.jiaze.network;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

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
    private NetworkTestService.NetworkTestBinder networkTestBinder;
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message, msg.what = " + msg.what);
            switch (msg.what){
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(getString(R.string.btn_start_test));
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
        bindNetworkTestService();
    }

    private void bindNetworkTestService(){
        Intent intent = new Intent(this, NetworkTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, NETWORK_TEST_PARAMS_SAVE_PATH);
        String networkTestTime = properties.getProperty(getString(R.string.key_network_test_time), "1");
        tvServiceState = (TextView) findViewById(R.id.service_state);
        etTestTime = (EditText) findViewById(R.id.network_test_time);
        tvTestResult = (TextView) findViewById(R.id.network_test_result);
        btnStart = (Button) findViewById(R.id.network_start_btn);
        etTestTime.setText(networkTestTime);
        etTestTime.requestFocus();
        etTestTime.setSelection(etTestTime.getText().length());
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_network_test_time), Integer.parseInt(etTestTime.getText().toString()));
        return bundle;
    }

    private void saveTestParams(){
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
            properties.setProperty(getString(R.string.key_sim_test_time), etTestTime.getText().toString());
            properties.store(outputStream, "NetWorkParameter");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveTestParams: Failed to Create FileOutputStream Create");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "saveTestParams: Failed to store the properties to the File");
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.network_start_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    saveTestParams();
                    networkTestBinder.startTest(getTestParameter());
                    btnStart.setText(getString(R.string.btn_stop_test));
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