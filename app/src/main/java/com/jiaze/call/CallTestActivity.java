package com.jiaze.call;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import com.jiaze.autotestapp.R;
import com.jiaze.common.AutoTestActivity;
import com.jiaze.common.Constant;
import com.jiaze.common.TableContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.call
 * Create by jz-pf
 * on 2019/6/20
 * =========================================
 */

public class CallTestActivity extends AutoTestActivity implements View.OnClickListener {

    public static final String CALL_TEST_PARAMS_SAVE_PATH = "CallTestParams";
    private static final String TAG = "CallTestActivity";
    private static final String PARAM_DESC = "CallParameter";

    private EditText etTestTime;
    private EditText etPhone;
    private EditText etWaitTime;
    private EditText etDurationTime;

    @Override
    protected void initUI() {
        //todo 从文件中读取测试所要用的参数，通过Property进行存储

        mTable.addParamsInput(getString(R.string.param_phone), true, getString(R.string.key_phone),
                "10086", Constant.EDIT_ID_PHONE, Constant.EnumDataType.DATA_TYPE_PHONE.getType());
        mTable.addParamsInput(getString(R.string.param_test_times), true, getString(R.string.key_test_times),
                "5", Constant.EDIT_ID_TEST_TIME, Constant.EnumDataType.DATA_TYPE_INT.getType());
        mTable.addParamsInput(getString(R.string.param_wait_time), true, getString(R.string.key_wait_time),
                "20", Constant.EDIT_ID_WAIT_TIME, Constant.EnumDataType.DATA_TYPE_INT.getType());
        mTable.addParamsInput(getString(R.string.param_duration_time), true, getString(R.string.key_duration_time),
                "10", Constant.EDIT_ID_DURATION_TIME, Constant.EnumDataType.DATA_TYPE_INT.getType());

        TableRow btnRow = mTable.createTableRow();
        btnStart = (Button) mTable.createButton(getString(R.string.btn_start_test), Constant.BUTTON_START_ID, getString(R.string.key_btn_start), btnRow);
        btnStart.setOnClickListener(this);
        //此处在服务连接后会将btnStart的setEnable属性设置为true，在服务为来接前不能点击，防止bug
        btnStart.setEnabled(false);
        mTable.getmTableLayout().addView(btnRow);

        tvResult = (TextView) findViewById(R.id.tv_result);

        etPhone = (EditText) mTable.getmTableLayout().findViewById(Constant.EDIT_ID_PHONE);
        etTestTime = (EditText) mTable.getmTableLayout().findViewById(Constant.EDIT_ID_TEST_TIME);
        etWaitTime = (EditText) mTable.getmTableLayout().findViewById(Constant.EDIT_ID_WAIT_TIME);
        etDurationTime = (EditText) mTable.getmTableLayout().findViewById(Constant.EDIT_ID_DURATION_TIME);
    }

    @Override
    protected void saveTestParams() throws IOException {
        String filePath = getFilesDir().getAbsolutePath();
        File callParamsFile = new File(filePath + "/" + CALL_TEST_PARAMS_SAVE_PATH);
        if (!callParamsFile.exists()){
            callParamsFile.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(callParamsFile);
        Properties properties = new Properties();
        properties.setProperty(getString(R.string.key_phone), etPhone.getText().toString());
        properties.setProperty(getString(R.string.key_test_times), etTestTime.getText().toString());
        properties.setProperty(getString(R.string.key_wait_time), etWaitTime.getText().toString());
        properties.setProperty(getString(R.string.key_duration_time), etDurationTime.getText().toString());

        properties.store(fileOutputStream, PARAM_DESC);
    }

    @Override
    protected Bundle getTestParams() {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.key_phone), etPhone.getText().toString());
        bundle.putInt(getString(R.string.key_test_times), Integer.parseInt(etTestTime.getText().toString()));
        bundle.putInt(getString(R.string.key_wait_time), Integer.parseInt(etWaitTime.getText().toString()));
        bundle.putInt(getString(R.string.key_duration_time), Integer.parseInt(etDurationTime.getText().toString()));
        return bundle;
    }

    @Override
    protected Class<?> getServiceClass() {
        return CallTestService.class;
    }
}
