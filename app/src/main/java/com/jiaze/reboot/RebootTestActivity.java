package com.jiaze.reboot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
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
    private EditText etRebootTimes;
    private Button btnTest;
    private TextView tvRebootResult;
    private RebootTestService.RebootTestBinder rebootTestBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rebootTestBinder = (RebootTestService.RebootTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the RebootTestService Succeed");
            btnTest.setText(getString(R.string.btn_start_test));
            btnTest.setEnabled(true);
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
        bindRebootTestService();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.reboot_btn:
                if (btnTest.getText().equals(getString(R.string.btn_start_test))){
                    saveTestParams();
                    rebootTestBinder.startTest(getTestParameter());
                    btnTest.setText(R.string.btn_stop_test);
                }else {
                    rebootTestBinder.stopTest();
                    btnTest.setText(R.string.btn_start_test);
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

}
