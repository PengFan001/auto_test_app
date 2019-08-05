package com.jiaze.common;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jiaze.autotestapp.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.common
 * Create by jz-pf
 * on 2019/6/19
 * =========================================
 */
public abstract class AutoTestActivity extends Activity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    protected static final int MSG_ID_TEST_FINISHED = 1;
    protected static final int MSG_ID_GOT_TEST_RESULT = 2;
    private static final String TAG = "AutoTestActivity";

    protected TableContainer mTable;
    protected Button btnStart;
    protected TextView tvResult;
    protected AutoTestService mAutoTestService;

    protected abstract void initUI();
    protected abstract int saveTestParams() throws IOException;
    protected abstract Bundle getTestParams();
    protected abstract Class<?> getServiceClass();

    protected Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MSG_ID_TEST_FINISHED:
                    btnStart.setText(R.string.btn_start_test);
                    final String resultPath = (String) msg.obj;
                    Log.d(TAG, "handleMessage: finish the test, load the testResult from : " + resultPath);
                    final Message getResult = mHandler.obtainMessage();
                    getResult.what = MSG_ID_GOT_TEST_RESULT;
                    if (TextUtils.isEmpty(resultPath)){
                        Log.d(TAG, "handleMessage: the result dir isn't exist : " + resultPath);
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BufferedReader bufferedReader = null;
                            try {
                                FileReader reader = new FileReader(new File(resultPath));
                                bufferedReader = new BufferedReader(reader);
                                StringBuilder builder = new StringBuilder();
                                String line;
                                while ((line = bufferedReader.readLine()) != null){
                                    builder.append(line);
                                    builder.append("\r\n");
                                }
                                getResult.obj = builder.toString();
                                getResult.sendToTarget();
                            } catch (FileNotFoundException e) {
                                Log.d(TAG, "run: read the FileReader Failed");
                                e.printStackTrace();
                            } catch (IOException e) {
                                Log.d(TAG, "run: readLine error");
                                e.printStackTrace();
                            }finally {
                                if (bufferedReader != null){
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e) {
                                        Log.d(TAG, "run: close the buffer Reader Failed");
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }).start();
                    break;
                case MSG_ID_GOT_TEST_RESULT:
                    Log.d(TAG, "handleMessage: testResult: " + msg.obj);
                    tvResult.setText((String)msg.obj);
                    break;
            }
            return false;
        }
    });

    /**
     * 此处重写此方法的作用是以防Activity被系统回收之后，重新启动Activity
     * 或者是测试结束后返回此界面
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(getString(R.string.key_result))){
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ID_TEST_FINISHED;
            //msg.obj存储的是存放了测试结果的文件存储路径，然后发送出去
            msg.obj = intent.getStringExtra(getString(R.string.key_result));
            msg.sendToTarget();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAutoTestService();
        mTable = new TableContainer(this);
        mTable.getmTableLayout().setOrientation(TableLayout.VERTICAL);
        setContentView(mTable.getmContainerLayout());
        initUI();
    }

    private void bindAutoTestService(){
        Intent intent = new Intent(this, getServiceClass());
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    protected ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAutoTestService = (AutoTestService)((AutoTestService.ServiceBinder)service).getService();

//            Message isTestingMsg = mHandler.obtainMessage();
//            isTestingMsg.what = MSG_ID_TEST_FINISHED;

            if (mAutoTestService.isInTesting()){
                btnStart.setText(R.string.btn_stop_test);
            }else {
                btnStart.setText(R.string.btn_start_test);
            }
            btnStart.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAutoTestService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == Constant.BUTTON_START_ID){
            if (((Button)v).getText().equals(getString(R.string.btn_start_test))){
                try {
                    if (saveTestParams() == 0){
                        mAutoTestService.startTest(getTestParams());
                        ((Button) v).setText(R.string.btn_stop_test);
                    }else {

                    }
                } catch (IOException e) {
                    Log.d(TAG, "==========save test params failed=========");
                    e.printStackTrace();
                }

            }else{
                mAutoTestService.stopTest();
                ((Button) v).setText(R.string.btn_start_test);
            }
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }


//    private void setPermissionAboutApp(){
//        Toast.makeText(this, "Please get me the grant", Toast.LENGTH_SHORT).show();
//        Intent grantIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        Uri uri = Uri.fromParts("package", getPackageName(), null);
//        grantIntent.setData(uri);
//        startActivity(grantIntent);
//    }
}
