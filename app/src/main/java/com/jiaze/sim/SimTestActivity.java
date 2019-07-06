package com.jiaze.sim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jiaze.autotestapp.R;

public class SimTestActivity extends AppCompatActivity {

    private static final String TAG = "SimTestActivity";
    private static final String SIM_TEST_PARAM_SAVE_PATH = "";

    private TextView tvSimState;
    private EditText etTestTime;
    private Button btnStart;
    private TextView tvTestResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_test);
        initUI();
    }

    private void initUI(){
        tvSimState = (TextView) findViewById(R.id.sim_state);
        etTestTime = (EditText) findViewById(R.id.sim_test_time);
        btnStart = (Button) findViewById(R.id.sim_start_btn);
        tvTestResult = (TextView) findViewById(R.id.sim_test_result);
    }


}
