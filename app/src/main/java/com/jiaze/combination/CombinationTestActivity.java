package com.jiaze.combination;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class CombinationTestActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "CombinationTestActivity";
    private static final String COMBINATION_TEST_PARAMS_SAVE_PATH = "CombinationTestParams";
    private static final int MSG_ID_TEST_FINISHED = 2;
    private static final int MSG_ID_GOT_TEST_RESULT = 3;

    private EditText etTestTime;
    private CheckBox checkBox_reboot;
    private CheckBox checkBox_sim;
    private CheckBox checkBox_network;
    private CheckBox checkBox_call;
    private CheckBox checkBox_air_mode;
    private CheckBox checkBox_sms;
    private CheckBox checkBox_ps;
    private CheckBox checkBox_module_reboot;
    private EditText etCallPhone;
    private EditText etWaitTime;
    private EditText etDurationTime;
    private EditText etSmsPhone;
    private EditText etWaitResultTime;
    private EditText etSmsBody;
    private RadioGroup rgTestModule;
    private RadioButton rbtnChecked;
    private TextView tvTestResult;
    private Button btnStart;
    private LinearLayout llNetworkTest;
    private LinearLayout llCallTest;
    private LinearLayout llSmsTest;
    private boolean isCheck = false;
    private int inputLength = 0;
    private CombinationTestService.CombinationTestBinder combinationTestBinder;


    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: get the message msg.what = " + msg.what);
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

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            combinationTestBinder = (CombinationTestService.CombinationTestBinder) service;
            Log.d(TAG, "onServiceConnected: Bind the CombinationTestService");
            btnStart.setEnabled(true);
            getTestResult();
            if (combinationTestBinder.isInTesting()){
                btnStart.setText(getString(R.string.btn_stop_test));
            }else {
                btnStart.setText(getString(R.string.btn_start_test));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            combinationTestBinder = null;
            btnStart.setEnabled(false);
        }
    };

    private void bindCombinationTestService(){
        Intent intent = new Intent(this, CombinationTestService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combination_test);
        initUI();
        setRadioButtonListener();
        setCheckBoxListener();
        bindCombinationTestService();
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, COMBINATION_TEST_PARAMS_SAVE_PATH);
        String testTime = properties.getProperty(getString(R.string.key_test_times), "1");
        String callPhone = properties.getProperty(getString(R.string.key_call_phone), "10086");
        String waitTime = properties.getProperty(getString(R.string.key_wait_time), "20");
        String durationTime = properties.getProperty(getString(R.string.key_duration_time), "20");
        String smsPhone = properties.getProperty(getString(R.string.key_sms_phone), "10086");
        String waitResultTime = properties.getProperty(getString(R.string.key_wait_sms_result_time), "15");
        String smsBody = properties.getProperty(getString(R.string.key_sms_string), "Hello World");
        tvTestResult = (TextView) findViewById(R.id.combination_test_result);
        btnStart = (Button) findViewById(R.id.combination_btn);
        etTestTime = (EditText) findViewById(R.id.combination_times);
        checkBox_reboot = (CheckBox) findViewById(R.id.checkbox_reboot);
        checkBox_sim = (CheckBox) findViewById(R.id.checkbox_sim);
        checkBox_network = (CheckBox) findViewById(R.id.checkbox_network);
        checkBox_call = (CheckBox) findViewById(R.id.checkbox_call);
        checkBox_air_mode = (CheckBox) findViewById(R.id.checkbox_air_mode);
        checkBox_sms = (CheckBox) findViewById(R.id.checkbox_sms);
        checkBox_ps = (CheckBox) findViewById(R.id.checkbox_ps);
        checkBox_module_reboot = (CheckBox) findViewById(R.id.checkbox_module_reboot);
        etCallPhone = (EditText) findViewById(R.id.combination_call_phone);
        etWaitTime = (EditText) findViewById(R.id.combination_wait_time);
        etDurationTime = (EditText) findViewById(R.id.combination_duration_time);
        etSmsPhone = (EditText) findViewById(R.id.combination_sms_phone);
        etWaitResultTime = (EditText) findViewById(R.id.combination_wait_result_time);
        etSmsBody = (EditText) findViewById(R.id.combination_sms_str);
        rgTestModule = (RadioGroup) findViewById(R.id.combination_network_test_module);
        llCallTest = (LinearLayout) findViewById(R.id.combination_call_test);
        llNetworkTest = (LinearLayout) findViewById(R.id.combination_network_test_type);
        llSmsTest = (LinearLayout) findViewById(R.id.combination_sms_test);
        initChecked();
        if (Integer.parseInt(testTime) == 0){
            etTestTime.setText(getString(R.string.reboot_default_value));
        }else {
            etTestTime.setText(testTime);
        }
        etTestTime.requestFocus();
        etTestTime.setSelection(etTestTime.getText().length());
        setEditTextValue(etCallPhone, callPhone);
        setEditTextValue(etWaitTime, waitTime);
        setEditTextValue(etDurationTime, durationTime);
        setEditTextValue(etSmsPhone, smsPhone);
        setEditTextValue(etWaitResultTime, waitResultTime);
        setEditTextValue(etSmsBody, smsBody);

        etSmsBody.addTextChangedListener(mTextWatcher);

        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
    }

    private void setCheckBoxListener(){
        checkBox_reboot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_reboot_is_checked), true).commit();
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_reboot_is_checked), false).commit();
                }
            }
        });

        checkBox_sim.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_sim_is_checked), true).commit();
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_sim_is_checked), false).commit();
                }
            }
        });

        checkBox_network.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_network_is_checked), true).commit();
                    llNetworkTest.setVisibility(View.VISIBLE);
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_network_is_checked), false).commit();
                    llNetworkTest.setVisibility(View.GONE);
                }
            }
        });

        checkBox_call.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_call_is_checked), true).commit();
                    llCallTest.setVisibility(View.VISIBLE);
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_call_is_checked), false).commit();
                    llCallTest.setVisibility(View.GONE);
                }
            }
        });

        checkBox_air_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_air_mode_is_checked), true).commit();
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_air_mode_is_checked), false).commit();
                }
            }
        });

        checkBox_sms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_sms_is_checked), true).commit();
                    llSmsTest.setVisibility(View.VISIBLE);
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_sms_is_checked), false).commit();
                    llSmsTest.setVisibility(View.GONE);
                }
            }
        });

        checkBox_ps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_ps_is_checked), true).commit();
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_ps_is_checked), false).commit();
                }
            }
        });

        checkBox_module_reboot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_module_reboot_is_checked), true).commit();
                }else {
                    getSharedPreferences("combination", MODE_PRIVATE).edit().putBoolean(getString(R.string.key_module_reboot_is_checked), false).commit();
                }
            }
        });
    }

    private void setRadioButtonListener(){
        rgTestModule.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                rbtnChecked = (RadioButton) findViewById(checkedId);
                getSharedPreferences("combination", MODE_PRIVATE).edit().putString(getString(R.string.key_network_test_module), rbtnChecked.getText().toString()).commit();
                Log.d(TAG, "onCheckedChanged: get the checked module = " + rbtnChecked.getText().toString());
            }
        });
    }

    private void initChecked(){
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_reboot_is_checked), false);
        checkBox_reboot.setChecked(isCheck);
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_sim_is_checked), false);
        checkBox_sim.setChecked(isCheck);
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_network_is_checked), false);
        checkBox_network.setChecked(isCheck);
        if (isCheck){
            llNetworkTest.setVisibility(View.VISIBLE);
        }else {
            llNetworkTest.setVisibility(View.GONE);
        }
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_call_is_checked), false);
        checkBox_call.setChecked(isCheck);
        if (isCheck){
            llCallTest.setVisibility(View.VISIBLE);
        }else {
            llCallTest.setVisibility(View.GONE);
        }
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_air_mode_is_checked), false);
        checkBox_air_mode.setChecked(isCheck);
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_sms_is_checked), false);
        checkBox_sms.setChecked(isCheck);
        if (isCheck){
            llSmsTest.setVisibility(View.VISIBLE);
        }else {
            llSmsTest.setVisibility(View.GONE);
        }
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_ps_is_checked), false);
        checkBox_ps.setChecked(isCheck);
        isCheck = getSharedPreferences("combination", MODE_PRIVATE).getBoolean(getString(R.string.key_module_reboot_is_checked), false);
        checkBox_module_reboot.setChecked(isCheck);
        String testType = getSharedPreferences("combination", MODE_PRIVATE).getString(getString(R.string.key_network_test_module), getString(R.string.text_reboot_device));
        Log.d(TAG, "initChecked: testType = " + testType);
        if (testType.equals(getString(R.string.text_reboot_radio))){
            rbtnChecked = (RadioButton) findViewById(R.id.combination_reboot_radio);
            rbtnChecked.setChecked(true);
        }else if (testType.equals(getString(R.string.text_reboot_device))){
            rbtnChecked = (RadioButton) findViewById(R.id.combination_reboot_device);
            rbtnChecked.setChecked(true);
        }
    }

    private void setEditTextValue(EditText editText, String value){
        editText.setText(value);
        editText.requestFocus();
        editText.setSelection(editText.getText().length());
    }

    private void getTestResult(){
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.key_test_result_path))){
            Log.d(TAG, "getResultPath: get the combination test result and show it");
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            msg.obj = intent.getStringExtra(getString(R.string.key_test_result_path));
            msg.sendToTarget();
        }else {
            Log.d(TAG, "getResultPath: no combination test result need to show");
        }
    }

    private Bundle getTestParameter(){
        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.key_test_times), Integer.parseInt(etTestTime.getText().toString()));
        bundle.putBoolean(getString(R.string.key_reboot_is_checked), checkBox_reboot.isChecked());
        bundle.putBoolean(getString(R.string.key_sim_is_checked), checkBox_sim.isChecked());
        bundle.putBoolean(getString(R.string.key_network_is_checked), checkBox_network.isChecked());
        bundle.putBoolean(getString(R.string.key_call_is_checked), checkBox_call.isChecked());
        bundle.putBoolean(getString(R.string.key_air_mode_is_checked), checkBox_air_mode.isChecked());
        bundle.putBoolean(getString(R.string.key_sms_is_checked), checkBox_sms.isChecked());
        bundle.putBoolean(getString(R.string.key_ps_is_checked), checkBox_ps.isChecked());
        bundle.putBoolean(getString(R.string.key_module_reboot_is_checked), checkBox_module_reboot.isChecked());
        if (checkBox_network.isChecked()){
            bundle.putString(getString(R.string.key_network_test_module), rbtnChecked.getText().toString());
        }
        if (checkBox_call.isChecked()){
            bundle.putString(getString(R.string.key_call_phone), etCallPhone.getText().toString());
            bundle.putInt(getString(R.string.key_wait_time), Integer.parseInt(etWaitTime.getText().toString()));
            bundle.putInt(getString(R.string.key_duration_time), Integer.parseInt(etDurationTime.getText().toString()));
        }
        if (checkBox_sms.isChecked()){
            bundle.putString(getString(R.string.key_sms_phone), etSmsPhone.getText().toString());
            bundle.putInt(getString(R.string.key_wait_sms_result_time), Integer.parseInt(etWaitResultTime.getText().toString()));
            bundle.putString(getString(R.string.key_sms_string), etSmsBody.getText().toString());
        }
        return bundle;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.combination_btn:
                if (btnStart.getText().equals(getString(R.string.btn_start_test))){
                    if (saveTestParams() == 0){
                        combinationTestBinder.startTest(getTestParameter());
                        btnStart.setText(getString(R.string.btn_stop_test));
                    }else {

                    }
                }else {
                    combinationTestBinder.stopTest();
                    btnStart.setText(getString(R.string.btn_start_test));
                }
                break;
        }
    }

    private int saveTestParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + COMBINATION_TEST_PARAMS_SAVE_PATH;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveTestParams: Create Combination Test Parameter File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveTestParams: Create Combination Test Parameter File Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();

            if (TextUtils.isEmpty(etTestTime.getText().toString())){
                Toast.makeText(this, getString(R.string.text_test_not_null),Toast.LENGTH_SHORT).show();
                return -1;
            }else {
                if (Integer.parseInt(etTestTime.getText().toString()) == 0){
                    Toast.makeText(this, getString(R.string.text_test_time_is_zero), Toast.LENGTH_SHORT).show();
                    return -1;
                }
            }

            if (Integer.parseInt(etWaitResultTime.getText().toString().trim()) < 15){
                Toast.makeText(this, getString(R.string.text_min_wait_result_time), Toast.LENGTH_SHORT).show();
                return -1;
            }

            if (Integer.parseInt(etWaitTime.getText().toString()) <= 0){
                Toast.makeText(this, getString(R.string.text_wait_time_must_large_zero), Toast.LENGTH_SHORT).show();
                return -1;
            }

            properties.setProperty(getString(R.string.key_test_times), etTestTime.getText().toString());
            properties.setProperty(getString(R.string.key_call_phone), etCallPhone.getText().toString().trim());
            properties.setProperty(getString(R.string.key_wait_time), etWaitTime.getText().toString().trim());
            properties.setProperty(getString(R.string.key_duration_time), etDurationTime.getText().toString().trim());
            properties.setProperty(getString(R.string.key_sms_phone), etSmsPhone.getText().toString().trim());
            properties.setProperty(getString(R.string.key_wait_sms_result_time), etWaitResultTime.getText().toString().trim());
            properties.setProperty(getString(R.string.key_sms_string), etSmsBody.getText().toString().trim());
            properties.store(outputStream, "CombinationParameter");
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

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (delayRun != null && mHandler!= null){
                mHandler.removeCallbacks(delayRun);
            }
            String s = editable.toString();
            Log.d(TAG, "afterTextChanged: the byte if s = " + s.getBytes().length);
            inputLength = s.getBytes().length;
            mHandler.postDelayed(delayRun, 1600);
        }
    };

    Runnable delayRun = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: the sms input is finished");
            if (inputLength > 140){
                Toast.makeText(getApplicationContext(), getString(R.string.text_long_sms), Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(), getString(R.string.text_short_sms), Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null){
            unbindService(connection);
        }
    }
}
