package com.jiaze.ftp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

import com.jiaze.autotestapp.R;

public class FTPLoginActivity extends Activity {

    private static final String TAG = "FTPLoginActivity";
    private EditText etIp;
    private EditText etPort;
    private EditText etUsername;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftplogin);
    }
}
