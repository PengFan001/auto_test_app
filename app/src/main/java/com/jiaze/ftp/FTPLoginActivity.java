package com.jiaze.ftp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jiaze.autotestapp.R;
import com.jiaze.common.Constant;
import com.jiaze.common.FTPUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPLoginActivity extends Activity {

    private static final String TAG = "FTPLoginActivity";
    private static final String FTP_SET_PARAMS = "FTPParams";
    private static final int IS_LOGIN_IN = 1;
    private static final int IS_LOGIN_FAILED = 2;

    private EditText etIp;
    private EditText etPort;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private String ip;
    private String port;
    private String username;
    private String password;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what){
                case IS_LOGIN_IN:
                    Toast.makeText(getApplicationContext(), getString(R.string.text_connect_ftp_success), Toast.LENGTH_SHORT).show();
                    break;

                case IS_LOGIN_FAILED:
                    Toast.makeText(getApplicationContext(), getString(R.string.text_connect_ftp_failed), Toast.LENGTH_SHORT).show();
                    break;
            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftplogin);
        initUI();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip = etIp.getText().toString();
                port = etPort.getText().toString();
                username = etUsername.getText().toString();
                password = etPassword.getText().toString();

                if (saveFTPParams() == 0){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            FTPUtil ftpUtil = new FTPUtil(ip, Integer.parseInt(port), username, password);
                            if (ftpUtil.connectFTPServer()){
                                mHandler.sendEmptyMessage(IS_LOGIN_IN);
                            }else {
                                mHandler.sendEmptyMessage(IS_LOGIN_FAILED);
                            }
                        }
                    }).start();
                }
            }
        });
    }

    private void initUI(){
        Properties properties = Constant.loadTestParameter(this, FTP_SET_PARAMS);
        String ftpIp = properties.getProperty(getString(R.string.key_ftp_ip), null);
        String ftpPort = properties.getProperty(getString(R.string.key_ftp_port), "21");
        String ftpUsername = properties.getProperty(getString(R.string.key_ftp_username), null);
        String ftpPassword = properties.getProperty(getString(R.string.key_ftp_password));

        etIp = (EditText) findViewById(R.id.ip);
        etPort = (EditText) findViewById(R.id.port);
        etUsername = (EditText) findViewById(R.id.username);
        etPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btn_login);
        setEditTextValue(etIp, ftpIp);
        setEditTextValue(etPort, ftpPort);
        setEditTextValue(etUsername, ftpUsername);
        setEditTextValue(etPassword, ftpPassword);

    }

    private void setEditTextValue(EditText editText, String value){
        editText.setText(value);
        editText.requestFocus();
        editText.setSelection(editText.getText().length());
    }

    private int saveFTPParams(){
        String filePath = getFilesDir().getAbsolutePath() + "/" + FTP_SET_PARAMS;
        File paramFile = new File(filePath);
        if (!paramFile.exists()){
            try {
                paramFile.createNewFile();
                Log.d(TAG, "saveFTPParams: Create PS Test Parameter File Success");
            } catch (IOException e) {
                Log.d(TAG, "saveFTPParams: Create PS Test Parameter File Failed");
                e.printStackTrace();
            }
        }

        try {
            OutputStream outputStream = new FileOutputStream(paramFile);
            Properties properties = new Properties();
            if (TextUtils.isEmpty(etIp.getText().toString())){
                Toast.makeText(this, getString(R.string.text_ip_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }

            if (TextUtils.isEmpty(etPort.getText().toString())){
                Toast.makeText(this, getString(R.string.text_port_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }

            if (TextUtils.isEmpty(etUsername.getText().toString())){
                Toast.makeText(this, getString(R.string.text_username_not_null), Toast.LENGTH_SHORT).show();
                return -1;
            }

            Log.d(TAG, "saveFTPParams: etIP = " + etIp.getText().toString());
            if (isInputValid(etIp.getText().toString().trim(), Constant.IP_FORMAT)){
                Toast.makeText(this, getString(R.string.text_ip_address_format_error), Toast.LENGTH_SHORT).show();
                return -1;
            }

            properties.setProperty(getString(R.string.key_ftp_ip), etIp.getText().toString().trim());
            properties.setProperty(getString(R.string.key_ftp_port), etPort.getText().toString().trim());
            properties.setProperty(getString(R.string.key_ftp_username), etUsername.getText().toString().trim());
            properties.setProperty(getString(R.string.key_ftp_password), etPassword.getText().toString().trim());
            properties.store(outputStream, "FTPParameter");
            if (outputStream != null){
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "saveFTPParams: Failed to Create FileOutputStream Create");
            e.printStackTrace();
            return -2;
        } catch (IOException e) {
            Log.d(TAG, "saveFTPParams: Failed to store the properties to the File");
            e.printStackTrace();
            return -2;
        }

        return 0;
    }

    private boolean isInputValid(String input, String validFormat){
        boolean isValid;
        String patternFormat = validFormat;
        Pattern pattern = Pattern.compile(patternFormat);
        Matcher matcher = pattern.matcher(input);
        isValid = matcher.matches();
        return isValid;
    }

}
