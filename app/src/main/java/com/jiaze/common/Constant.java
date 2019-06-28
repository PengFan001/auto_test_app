package com.jiaze.common;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Constant {

    private static final String TAG = "Constant";

    public static final String PRE_KEY_BOOT_OR_SHUTDOWN = "boot_or_shutdown";
    public static final String PRE_KEY_CALL = "call";
    public static final String PRE_KEY_SIM = "sim";
    public static final String PRE_KEY_NET = "net";
    public static final String PRE_KEY_SMS = "sms";
    public static final String PRE_KEY_AIR_MODE = "airMode";
    public static final String PRE_KEY_PPS = "pps";

    public static final int EDIT_ID_PHONE = 0;
    public static final int EDIT_ID_TEST_TIME = 1;
    public static final int EDIT_ID_WAIT_TIME = 2;
    public static final int EDIT_ID_DURATION_TIME = 3;
    public static final int BUTTON_START_ID = 4;
    public static final int TEXTVIEW_RESULT_ID = 5;

    public static final String PARAMS_NAME = "参数名称";

    public static enum EnumDataType {
        DATA_TYPE_INT(1, "int"),
        DATA_TYPE_STRING(2, "String"),
        DATA_TYPE_FLOAT(3, "float"),
        DATA_TYPE_DATE(4, "Date"),
        DATA_TYPE_ALL(5, "allType"),
        DATA_TYPE_PHONE(6, "phone"),
        ;

        private int code;
        private String type;

        EnumDataType (int code, String type){
            this.code = code;
            this.type = type;
        }

        public int getCode() {
            return code;
        }

        public String getType() {
            return type;
        }
    }


    public static String createSaveTestResultPath(String testName){
        String dirName = testName + System.currentTimeMillis();
        File testResultPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTest");
        if (!testResultPath.exists()){
            testResultPath.mkdir();
            Log.d(TAG, "createSaveTestResultPath: create the testResultPath dir" + testResultPath + "\t" +testResultPath.exists());
        }
        File resultDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTest" + "/" + dirName);
        if (!resultDir.exists()){
            resultDir.mkdir();
            Log.d(TAG, "createSaveTestResultPath: create the resultDir" + resultDir + "\t" + resultDir.exists());
        }

        return resultDir.getAbsolutePath();
    }

    public static Properties loadTestParameter(Context context, String fileName){
        Properties properties = new Properties();
        if (context == null || TextUtils.isEmpty(fileName)){
            Log.d(TAG, "loadTestParameter: the fileName or the context is Error: " + context + "\t" + fileName);
            return properties;
        }
        String testDir = context.getFilesDir().getAbsolutePath() +  "/" + fileName;
        File file = new File(testDir);
        if (!file.exists()){
            Log.d(TAG, "loadTestParameter: the " + fileName + "is not exist");
            return properties;
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "loadTestParameter: create FileInputStream Failed");
            e.printStackTrace();
        }

        try {
            properties.load(inputStream);
        } catch (IOException e) {
            Log.d(TAG, "loadTestParameter: Failed to load the properties from the File");
            e.printStackTrace();
        }
        if (inputStream != null){
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "loadTestParameter: close the inputStream failed");
                e.printStackTrace();
            }
        }
        return properties;
    }

}