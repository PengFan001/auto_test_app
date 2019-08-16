package com.jiaze.autotestapp;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;

import com.jiaze.airmode.AirModeTestActivity;
import com.jiaze.call.CallTestActivity;
import com.jiaze.combination.CombinationTestActivity;
import com.jiaze.ftp.FTPLoginActivity;
import com.jiaze.network.NetworkTestActivity;
import com.jiaze.ps.PsTestActivity;
import com.jiaze.reboot.ModuleRebootActivity;
import com.jiaze.reboot.RebootTestActivity;
import com.jiaze.sim.SimTestActivity;
import com.jiaze.sms.SmsTestActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private GridView mGridView;
    private List<Map<String, Object>> gridItems;

    private int[] mAppIcons = {
            R.mipmap.icon_kaiguanji, R.mipmap.con_sim, R.mipmap.icon_ruwang, R.mipmap.icon_yuyingtonghua,
            R.mipmap.icon_feixingmoshi, R.mipmap.icon_message, R.mipmap.icon_psyewu, R.mipmap.icon_ceshi, R.mipmap.icon_tongyong, R.mipmap.icon_ftp
    };

    private String[] mAppNames = new String[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGridView = (GridView) findViewById(R.id.gridView);
        initGridViewData();
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, gridItems, R.layout.gridview_item,
                new String[]{getString(R.string.key_icon), getString(R.string.key_name)},
                new int[]{R.id.icon_image, R.id.name_tv});

        mGridView.setAdapter(simpleAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                if (mAppNames[position].equals(getString(R.string.title_boot_and_shutdown))){
                    intent.setClass(parent.getContext(), RebootTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_sim))) {
                    intent.setClass(parent.getContext(), SimTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_net))) {
                    intent.setClass(parent.getContext(), NetworkTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_call))) {
                    intent.setClass(parent.getContext(), CallTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_air_mode))) {
                    intent.setClass(parent.getContext(), AirModeTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_sms))) {
                    intent.setClass(parent.getContext(), SmsTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_pps))) {
                    intent.setClass(parent.getContext(), PsTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_module_reboot))){
                    intent.setClass(parent.getContext(), ModuleRebootActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_combination_test))){
                    intent.setClass(parent.getContext(), CombinationTestActivity.class);
                }else if (mAppNames[position].equals(getString(R.string.title_login_ftp))){
                    intent.setClass(parent.getContext(), FTPLoginActivity.class);
                }

                if (intent != null){
                    Log.d(TAG, "onItemClick: ========start a new activity============");
                    startActivity(intent);
                }else {
                    Log.d(TAG, "onItemClick: the intent init is exception");
                }
            }
        });

        Configuration configuration = this.getResources().getConfiguration();
        int ori = configuration.orientation;
        if (ori == configuration.ORIENTATION_LANDSCAPE){
            Log.d(TAG, "onCreate: the device is ORIENTATION_LANDSCAPE");
        }else if (ori == configuration.ORIENTATION_PORTRAIT){
            Log.d(TAG, "onCreate: the device is ORIENTATION_PORTRAIT");
        }

    }

    private void initGridViewData(){
        gridItems = new ArrayList<Map<String, Object>>();

        mAppNames[0] = getString(R.string.title_boot_and_shutdown);
        mAppNames[1] = getString(R.string.title_sim);
        mAppNames[2] = getString(R.string.title_net);
        mAppNames[3] = getString(R.string.title_call);
        mAppNames[4] = getString(R.string.title_air_mode);
        mAppNames[5] = getString(R.string.title_sms);
        mAppNames[6] = getString(R.string.title_pps);
        mAppNames[7] = getString(R.string.title_module_reboot);
        mAppNames[8] = getString(R.string.title_combination_test);
        mAppNames[9] = getString(R.string.title_login_ftp);

        for (int i = 0; i < mAppIcons.length; i++){
            Map<String, Object> gridItem = new HashMap<String, Object>();
            gridItem.put(getString(R.string.key_icon), mAppIcons[i]);
            gridItem.put(getString(R.string.key_name), mAppNames[i]);
            gridItems.add(gridItem);
        }
    }
}
