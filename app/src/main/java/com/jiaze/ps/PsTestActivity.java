package com.jiaze.ps;

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

public class PsTestActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "PsTestActivity";
    private static final String PS_TEST_PARAMS_SAVE_PATH = "PsTestParams";
    private static final int MSG_ID_TEST_FINISHED = 2;
    private static final int MSG_ID_GOT_TEST_RESULT = 3;

    private TextView tvPsState;
    private EditText etTestTime;
    private TextView tvTestResult;
    private Button btnStart;
    private PsTestService.PsTestBinder psTestBinder;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message msg.what = " + msg.what);
            switch (msg.what){
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(getString(R.string.btn_start_test));
                    tvPsState.setText(psTestBinder.getPsState());
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
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            psTestBinder = (PsTestService.PsTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the PsTestService");
            btnStart.setEnabled(true);
            tvPsState.setText(psTestBinder.getPsState());
            if (psTestBinder.isInTesting()){
                btnStart.setText(getString(R.string.btn_stop_test));
            }else {
                btnStart.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            psTestBinder = null;
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
        setContentView(R.layout.activity_ps_test);
        initUI();
        bindPsTestService();
    }

    private void bindPsTestService(){
        Intent intent = new Intent(this, PsTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, PS_TEST_PARAMS_SAVE_PATH);
        String psTestTime = properties.getProperty(getString(R.string.key_ps_test_time), "1");
        tvPsState = (TextView) findViewById(R.id.ps_state);
        tvTestResult = (TextView) findViewById(R.id.ps_test_result);
        btnStart = (Button) findViewById(R.id.ps_start_btn);
        etTestTime = (EditText) findViewById(R.id.ps_test_time);
        etTestTime.setText(psTestTime);
        etTestTime.requestFocus();
        etTestTime.setSelection(etTestTime.getText().length());
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_ps_test_time), Integer.parseInt(etTestTime.getText().toString()));
        return bundle;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ps_start_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    if (saveTestParams() == 0){
                        saveTestParams();
                        psTestBinder.startTest(getTestParameter());
                        btnStart.setText(getString(R.string.btn_stop_test));
                    }else {

                    }
                }else {
                    psTestBinder.stopTest();
                    btnStart.setText(getString(R.string.btn_start_test));
                }
        }
    }

    private int saveTestParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + PS_TEST_PARAMS_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveTestParams: Create PS Test Parameter File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create PS Test Parameter File Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();
            if (TextUtils.isEmpty(etTestTime.getText().toString())){
                Toast.makeText(this, getString(R.string.text_test_not_null),Toast.LENGTH_SHORT).show();
                return -1;
            }
            properties.setProperty(getString(R.string.key_ps_test_time), etTestTime.getText().toString());
            properties.store(outputStream, "PsParameter");
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
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null){
            unbindService(connection);
        }
    }

}
