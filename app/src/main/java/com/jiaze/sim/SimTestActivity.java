package com.jiaze.sim;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

public class SimTestActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SimTestActivity";
    private static final String SIM_TEST_PARAM_SAVE_PATH = "SimTestParam";
    private static final int UPDATE_SIM_STATE = 9;
    private static final int MSG_ID_TEST_FINISHED = 10;
    private static final int MSG_ID_GOT_TEST_RESULT = 11;

    private TextView tvSimState;
    private EditText etTestTime;
    private Button btnStart;
    private TextView tvTestResult;
    private SimTestService.SimTestBinder simTestBinder;
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the msg.what = " + msg.what);
            switch (msg.what){
                case UPDATE_SIM_STATE:
                    tvSimState.setText(simTestBinder.getSimState());
                    Log.d(TAG, "handleMessage: get sim State : " + simTestBinder.getSimState());
                    break;
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(getString(R.string.btn_start_test));
                    tvSimState.setText(simTestBinder.getSimState());
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
            simTestBinder = (SimTestService.SimTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the SimTestService Succeed");
            tvSimState.setText(simTestBinder.getSimState());
            btnStart.setEnabled(true);
            getResultPath();
            if (simTestBinder.isInTesting()){
                btnStart.setText(getString(R.string.btn_stop_test));
            }else {
                btnStart.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            simTestBinder = null;
            btnStart.setEnabled(false);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: ============onNewIntent=============");
        if (intent.hasExtra(getString(R.string.key_result))){
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            msg.obj = intent.getStringExtra(getString(R.string.key_result));
            msg.sendToTarget();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ===========onCreate=============");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_test);
        initUI();
        bindSimTestService();
    }

    private void getResultPath(){
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.key_result))){
            Log.d(TAG, "getResultPath: get the sim test result and show it");
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            msg.obj = intent.getStringExtra(getString(R.string.key_result));
            msg.sendToTarget();
        }else {
            Log.d(TAG, "getResultPath: no sim test result need to show");
        }
    }

    private void bindSimTestService(){
        Intent intent = new Intent(this, SimTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, SIM_TEST_PARAM_SAVE_PATH);
        String simTestTime = properties.getProperty(getString(R.string.key_sim_test_time), "1");
        tvSimState = (TextView) findViewById(R.id.sim_state);
        etTestTime = (EditText) findViewById(R.id.sim_test_time);
        btnStart = (Button) findViewById(R.id.sim_start_btn);
        tvTestResult = (TextView) findViewById(R.id.sim_test_result);
        if (Integer.parseInt(simTestTime) <= 0){
            etTestTime.setText(getString(R.string.reboot_default_value));
        }else {
            etTestTime.setText(simTestTime);
        }
        etTestTime.requestFocus();
        etTestTime.setSelection(etTestTime.getText().length());
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_sim_test_time), Integer.parseInt(etTestTime.getText().toString()));
        return bundle;
    }

    private int saveTestParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + SIM_TEST_PARAM_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveTestParams: Create the Sim Test Parameter File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create the Sim Test Parameter File Failed");
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
            properties.setProperty(getString(R.string.key_sim_test_time), etTestTime.getText().toString());
            properties.store(outputStream, "SimParameter");
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
            case R.id.sim_start_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    if (saveTestParams() == 0){
                        mHandler.sendEmptyMessage(UPDATE_SIM_STATE);
                        btnStart.setText(getString(R.string.btn_stop_test));
                        simTestBinder.startTest(getTestParameter());
                        Log.d(TAG, "onClick: send the update Ui message");
                    }else {

                    }
                }else {
                    btnStart.setText(getString(R.string.btn_start_test));
                    simTestBinder.stopTest();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ===============onDestroy============");
        if (connection != null){
            unbindService(connection);
        }
    }
}
