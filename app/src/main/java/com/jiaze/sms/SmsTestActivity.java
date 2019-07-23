package com.jiaze.sms;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import com.jiaze.autotestapp.R;
import com.jiaze.common.AutoTestActivity;
import com.jiaze.common.Constant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.sms
 * Create by jz-pf
 * on 2019/7/17
 * =========================================
 */
public class SmsTestActivity extends AutoTestActivity implements View.OnClickListener {

    public static final String SMS_TEST_PARAMS_SAVE_PATH = "SmsTestParams";
    private static final String TAG = "SmsTestActivity";
    private static final String PARAM_DESC = "SmsParameter";

    private EditText etTestTime;
    private EditText etPhone;
    private EditText etWaitResultTime;
    private EditText etSmsStr;

    @Override
    protected void initUI() {
        Properties properties = Constant.loadTestParameter(this, SMS_TEST_PARAMS_SAVE_PATH);
        String phone = properties.getProperty(getString(R.string.key_phone), "10086");
        String testTimes = properties.getProperty(getString(R.string.key_test_times), "3");
        String waitResultTime = properties.getProperty(getString(R.string.key_wait_sms_result_time), "20");
        String smsStr = properties.getProperty(getString(R.string.key_sms_string), "hello world");

        mTable.addParamsInput(getString(R.string.param_phone), true, getString(R.string.key_phone),
                phone, Constant.SMS_ID_PHONE, Constant.EnumDataType.DATA_TYPE_PHONE.getType());
        mTable.addParamsInput(getString(R.string.param_test_times), true, getString(R.string.key_test_times),
                testTimes, Constant.SMS_ID_TEST_TIME, Constant.EnumDataType.DATA_TYPE_INT.getType());
        mTable.addParamsInput(getString(R.string.param_wait_sms_result_time), true, getString(R.string.key_wait_sms_result_time),
                waitResultTime, Constant.SMS_ID_WAIT_TIME, Constant.EnumDataType.DATA_TYPE_INT.getType());
        mTable.addParamsInput(getString(R.string.param_sms_str), true, getString(R.string.key_sms_string),
                smsStr, Constant.SMS_ID_SMS_STRING, Constant.EnumDataType.DATA_TYPE_ALL.getType());

        TableRow btnRow = mTable.createTableRow();
        btnStart = (Button) mTable.createButton(getString(R.string.btn_start_test), Constant.BUTTON_START_ID, getString(R.string.key_btn_start), btnRow);
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);
        mTable.getmTableLayout().addView(btnRow);

        tvResult = (TextView) findViewById(R.id.tv_result);

        etPhone = (EditText) mTable.getmTableLayout().findViewById(Constant.SMS_ID_PHONE);
        etTestTime = (EditText) mTable.getmTableLayout().findViewById(Constant.SMS_ID_TEST_TIME);
        etWaitResultTime = (EditText) mTable.getmContainerLayout().findViewById(Constant.SMS_ID_WAIT_TIME);
        etSmsStr = (EditText) mTable.getmContainerLayout().findViewById(Constant.SMS_ID_SMS_STRING);

    }

    @Override
    protected void saveTestParams() throws IOException {
        String filePath = getFilesDir().getAbsolutePath();
        File smsParamsFile = new File(filePath + "/" + SMS_TEST_PARAMS_SAVE_PATH);
        if (!smsParamsFile.exists()){
            smsParamsFile.createNewFile();
        }

        FileOutputStream fileOutputStream = new FileOutputStream(smsParamsFile);
        Properties properties = new Properties();
        properties.setProperty(getString(R.string.key_phone), etPhone.getText().toString());
        properties.setProperty(getString(R.string.key_test_times), etTestTime.getText().toString());
        properties.setProperty(getString(R.string.key_wait_sms_result_time), etWaitResultTime.getText().toString());
        properties.setProperty(getString(R.string.key_sms_string), etSmsStr.getText().toString());

        properties.store(fileOutputStream, PARAM_DESC);
        if (fileOutputStream != null){
            fileOutputStream.close();
        }
    }

    @Override
    protected Bundle getTestParams() {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.key_phone), etPhone.getText().toString());
        bundle.putInt(getString(R.string.key_test_times), Integer.parseInt(etTestTime.getText().toString()));
        bundle.putInt(getString(R.string.key_wait_sms_result_time), Integer.parseInt(etWaitResultTime.getText().toString()));
        bundle.putString(getString(R.string.key_sms_string), etSmsStr.getText().toString());
        return bundle;
    }

    @Override
    protected Class<?> getServiceClass() {
        return SmsTestService.class;
    }
}