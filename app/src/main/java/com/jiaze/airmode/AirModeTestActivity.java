package com.jiaze.airmode;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class AirModeTestActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "AirModeTestActivity";
    private static final String AIR_MODE_TEST_PARAM_SAVE_PATH = "AirModeTestParams";
    private static final int UPDATE_AIR_MODE_STATE = 1;
    private static final int AIR_MODE_STATE_CHANGE = 4;
    private static final int MSG_ID_TEST_FINISHED = 2;
    private static final int MSG_ID_GOT_TEST_RESULT = 3;

    private TextView tvAirModeState;
    private EditText etTestTimes;
    private Button btnStart;
    private TextView tvTestResult;
    private AirModeTestService.AirModeTestBinder airModeTestBinder;
    private IntentFilter intentFilter;
    private AirModeChangeBroadcastReceiver airModeChangeBroadcastReceiver;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message msg.what = " + msg.what);
            switch (msg.what){
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(getString(R.string.btn_start_test));
                    tvAirModeState.setText(airModeTestBinder.getAirModeState());
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
                case AIR_MODE_STATE_CHANGE:
                    Log.d(TAG, "handleMessage: get the air mode state : " + msg.obj.toString());
                    tvAirModeState.setText(msg.obj.toString());
                    break;
            }
            return false;
        }
    });

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            airModeTestBinder = (AirModeTestService.AirModeTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the AirModeTestService");
            btnStart.setEnabled(true);
            tvAirModeState.setText(airModeTestBinder.getAirModeState());
            if (airModeTestBinder.isInTesting()){
                btnStart.setText(getString(R.string.btn_stop_test));
            }else {
                btnStart.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            airModeTestBinder = null;
            btnStart.setEnabled(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_mode_test);
        initUI();
        bindAirModeTestService();
        intentFilter = new IntentFilter("com.jiaze.action.AIR_MODE_STATE_CHANGED");
        airModeChangeBroadcastReceiver = new AirModeChangeBroadcastReceiver();
        registerReceiver(airModeChangeBroadcastReceiver, intentFilter);
        Log.d(TAG, "onCreate: register the AirModeChangeBroadcastReceiver");
    }

    class AirModeChangeBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: Receiver the Air Mode Changed broadcast");
            if (intent.hasExtra("state")){
                Message msg = mHandler.obtainMessage();
                msg.what = AIR_MODE_STATE_CHANGE;
                msg.obj = intent.getStringExtra("state");
                Log.d(TAG, "onReceive: get the air mode state : " + msg.obj.toString());
                msg.sendToTarget();
            }
        }
    }

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

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, AIR_MODE_TEST_PARAM_SAVE_PATH);
        String airModeTestTime = properties.getProperty(getString(R.string.key_air_mode_test_time), "1");
        tvAirModeState = (TextView) findViewById(R.id.air_mode_state);
        etTestTimes = (EditText) findViewById(R.id.air_mode_test_time);
        btnStart = (Button) findViewById(R.id.air_start_btn);
        tvTestResult = (TextView) findViewById(R.id.air_test_result);
        etTestTimes.setText(airModeTestTime);
        etTestTimes.requestFocus();
        etTestTimes.setSelection(etTestTimes.getText().length());
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private void bindAirModeTestService(){
        Intent intent = new Intent(this, AirModeTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private Bundle getTestParameter(){
        Bundle bundle =  new Bundle();
        bundle.putInt(getString(R.string.key_air_mode_test_time), Integer.parseInt(etTestTimes.getText().toString()));
        return bundle;
    }

    private int saveTestParams(){
        String filePath =  getFilesDir().getAbsolutePath() + "/" + AIR_MODE_TEST_PARAM_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveTestParams: Create the AirMode Test Params Save File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create the AirMode Test Params Save File Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();
            if (TextUtils.isEmpty(etTestTimes.getText().toString())){
                Toast.makeText(this, getString(R.string.text_test_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }
            properties.setProperty(getString(R.string.key_air_mode_test_time), etTestTimes.getText().toString());
            properties.store(outputStream, "AirModeParameter");
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
            case R.id.air_start_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    if (saveTestParams() == 0){
                        saveTestParams();
                        airModeTestBinder.startTest(getTestParameter());
                        Log.d(TAG, "onClick: send the update UI message");
                        btnStart.setText(getString(R.string.btn_stop_test));
                    }else {

                    }
                }else {
                    airModeTestBinder.stopTest();
                    btnStart.setText(getString(R.string.btn_start_test));
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (airModeChangeBroadcastReceiver != null){
            unregisterReceiver(airModeChangeBroadcastReceiver);
        }
        if (connection != null){
            unbindService(connection);
        }
    }
}
