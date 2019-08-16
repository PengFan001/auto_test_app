package com.jiaze.common;

import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class Constant {

    private static final String TAG = "Constant";

    private static final String LOG_FILE_DIR = "data/local/log";
    private static final String AUTO_TEST_RESULT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTest";
    private static final String ZIP_SUFFIX = ".zip";
    private static final String OPEN_AT_TT_LOG = "AT^TTLOG=1\r\n";
    private static final String CLOSE_AT_TT_LOG = "AT^TTLOG=0\r\n";
    private static final String CLOSE_AT_DLKS = "AT^DLKS=0\r\n";
    private static final String TT_LOG_PORT = "/dev/STTYEMS50";

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
    public static final int SMS_ID_PHONE = 6;
    public static final int SMS_ID_TEST_TIME = 7;
    public static final int SMS_ID_WAIT_TIME = 8;
    public static final int SMS_ID_SMS_STRING = 9;


    public static final String SIM_STATE_ABSENT = "无卡";
    public static final String SIM_STATE_UNKNOWN = "未知状态";
    public static final String SIM_STATE_NETWORK_LOCK = "需要NetworkPIN锁";
    public static final String SIM_STATE_PIN_REQUIRED = "需要PIN解锁";
    public static final String SIM_STATE_PUK_REQUIRED = "需要PUK解锁";
    public static final String SIM_STATE_READY = "良好";

    public static final String PARAMS_NAME = "参数名称";

    public static boolean isRead = false;

    public enum EnumDataType {
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

    public static void  zipLog(String fileName){
        String filePath = AUTO_TEST_RESULT_DIR + "/" + fileName + "/" +"log" + ZIP_SUFFIX;
        setFilePermission(LOG_FILE_DIR);
        ZipUtil.zip(LOG_FILE_DIR, filePath);
        String desFilePath = AUTO_TEST_RESULT_DIR + "/" + fileName + ZIP_SUFFIX;
        String srcFilePath = AUTO_TEST_RESULT_DIR + "/" + fileName;
        setFilePermission(srcFilePath);
        ZipUtil.zip(srcFilePath, desFilePath);
    }

    public static void  zipLogAndUploadftp(String fileName){
        String filePath = AUTO_TEST_RESULT_DIR + "/" + fileName + "/" +"log" + ZIP_SUFFIX;
        setFilePermission(LOG_FILE_DIR);
        ZipUtil.zip(LOG_FILE_DIR, filePath);
        String desFilePath = AUTO_TEST_RESULT_DIR + "/" + fileName + ZIP_SUFFIX;
        String srcFilePath = AUTO_TEST_RESULT_DIR + "/" + fileName;
        setFilePermission(srcFilePath);
        ZipUtil.zip(srcFilePath, desFilePath);
        upload(desFilePath);
    }


    public static void zipLog(String dir, String fileName){
        String filePath = dir + "/" + fileName + ZIP_SUFFIX;
        setFilePermission(LOG_FILE_DIR);
        ZipUtil.zip(LOG_FILE_DIR, filePath);
    }

    public static void zipAndDelLogFile(String zipFileName){
        zipLog(zipFileName);
        delLog();
    }

    public static void delLog(){
        delFileAndDir(LOG_FILE_DIR);
    }

    /**
     * 删除掉data/local/log下的文件，然后重启设备进行测试，这样可以抓取到干净的测试log
     * @param powerManager 控制设备的power管理
     */
    public static void delLog(PowerManager powerManager){
        delFileAndDir(LOG_FILE_DIR);
        if (powerManager != null){
            powerManager.reboot("delete the log, Reboot the device and start test");
        }else {
            Log.d(TAG, "delLog: Please init the powerManager");
        }
    }

    public static void delFileAndDir(String path){
        File file = new File(path);
        if (file.exists()){
            if (file.isDirectory()){
                String[] children = file.list();
                if (children.length == 0){
                    file.delete();
                }else {
                    for (int i = 0; i < children.length; i++){
                        delFileAndDir(LOG_FILE_DIR + "/" + children[i]);
                    }
                }
            }else {
                file.delete();
            }

            Log.d(TAG, "delFileAndDir: delete the log File success");
        }
    }

    /**
     * 设置文件的压缩权限
     * @param devPath 要进行压缩的文件目录
     */
    public static void setFilePermission(String devPath){
        Log.d(TAG, "setFilePermission: devPath = " + devPath);
        if (TextUtils.isEmpty(devPath)){
            Log.d(TAG, "setFilePermission: the devPath is null");
            return;
        }
        BufferedWriter mWriter = null;
        BufferedReader mReader = null;

        try {
            mWriter = new BufferedWriter(new FileWriter("/dev/ttySA"));
            mReader = new BufferedReader(new FileReader("/dev/ttySA"));
            Log.d(TAG, "setFilePermission: init the BufferedWriter and BufferedReader");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String command = "SA+SYSTEM=\"chmod 777 " + devPath + "/*" + "\"\r";
        Log.d(TAG, "setFilePermission: set the dev port permission command = " + command);
        try {
            mWriter.write(command, 0, command.length());
            mWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }finally {
            if (mWriter != null){
                try {
                    mWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        char[] cReadBuffer = new char[255];
        int read_len = 0;
        String result = null;
        try {
            read_len = mReader.read(cReadBuffer, 0, (cReadBuffer.length - 1));
            if (read_len > 0){
                result = String.copyValueOf(cReadBuffer, 0, read_len);
                Log.d(TAG, "setFilePermission: setChmod = " + result);
            }else if (read_len < 0){
                Log.d(TAG, "setFilePermission: set port permission error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
          if (mReader != null){
              try {
                  mReader.close();
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }
        }
    }

    public static String createSaveTestResultPath(String testName){
        String dirName = testName + getNowDate();
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

    public static String getTestResultFileName(String testResultDir){
        String[] dir = testResultDir.split("/");
        return dir[dir.length - 1];
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

    public static void showTestResult(final String resultPath, final Message getResult){
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedReader = null;
                FileReader reader = null;
                try {
                    reader = new FileReader(new File(resultPath));
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
                            Log.d(TAG, "run: close the Reader buffer");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public static String getNowDate(){
        Date currentDate = new Date();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat format2 = new SimpleDateFormat("HH:mm:ss");
        String dateString = format1.format(currentDate);
        String timeString = format2.format(currentDate);
        StringBuilder builder = new StringBuilder();
        builder.append(dateString);
        String[] time = timeString.split(":");
        for (int i = 0; i < time.length; i++){
            builder.append("-" + time[i]);
        }
        return builder.toString();
    }

    public static String createTTLogPath(String saveDir){
        String dirName = saveDir + "/" + "log";
        File logSaveFile = new File(dirName);
        if (!logSaveFile.exists()){
            logSaveFile.mkdir();
            Log.d(TAG, "createModuleLogPath: Create the module log save file success, the path = " + logSaveFile.getAbsolutePath());
        }

        return logSaveFile.getAbsolutePath();
    }

    public static void saveTTLog(String ttLog){
        Log.d(TAG, "saveTTLog: Start save the ttLog");
        File file = new File(LOG_FILE_DIR + "/" + "tt-log");
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveTTLog: ========Create the save TT log file success");
            } catch (IOException e) {
                Log.d(TAG, "saveTTLog: ========Create the save TT log file failed");
                e.printStackTrace();
            }
        }

        BufferedWriter bufferedWriter = null;
        //FileWriter fileWriter = null;

        try {
            //fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            bufferedWriter.write(ttLog);
            Log.d(TAG, "saveTTLog: Save the ttLog Success");
        } catch (IOException e) {
            Log.d(TAG, "saveTTLog: Save the ttLog Failed");
            e.printStackTrace();
        }finally {
            if (bufferedWriter != null){
                try {
                    bufferedWriter.close();
                    //fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveTTLog(String saveDir, String ttLog){
        Log.d(TAG, "saveTTLog: Start save the ttLog log");
        File file = new File(saveDir + "/" + "logcat");
        if (!file.exists()){
            try {
                file.createNewFile();
                Log.d(TAG, "saveTTLog: ========Create the save Module log file success");
            } catch (IOException e) {
                Log.d(TAG, "saveTTLog: ========Create the save Module log file failed");
                e.printStackTrace();
            }
        }

        BufferedWriter bufferedWriter = null;
        //FileWriter fileWriter = null;

        try {
            //fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            bufferedWriter.write(ttLog);
            Log.d(TAG, "saveTTLog: Save the ttLog Success");
        } catch (IOException e) {
            Log.d(TAG, "saveTTLog: Save the ttLog Failed");
            e.printStackTrace();
        }finally {
            if (bufferedWriter != null){
                try {
                    bufferedWriter.close();
                    //fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void openTTLog(){
        String command_close_dlks = CLOSE_AT_DLKS;
        sendAtCommand(command_close_dlks);
        startRead();
        String command_open_tt_log = OPEN_AT_TT_LOG;
        sendAtCommand(command_open_tt_log);
    }

    public static void closeTTLog(){
        String command_close_tt_log = CLOSE_AT_TT_LOG;
        sendAtCommand(command_close_tt_log);
        stopRead();
    }

    public static void sendAtCommand(String command){
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(TT_LOG_PORT));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            bufferedWriter.write(command);
            Log.d(TAG, "sendAtCommand: send command = " + command + " Success");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "sendAtCommand: send command = " + command + " Failed");
        }finally {
            if (bufferedWriter != null){
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void startRead(){
        isRead = true;
    }

    public static void stopRead(){
        isRead = false;
    }

    /**
     * 将读取到的TT log 存储到data/local/log目录下
     */
    public static void readTTLog(final String fileName){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start read the ttLog");
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new FileReader(TT_LOG_PORT));
                    String log;
                    while (((log = bufferedReader.readLine()) != null) && isRead){
                        Log.d(TAG, "run: isRead = " + isRead +" ==== log ==== " + log);
                        saveTTLog(log + "\n");
                    }
                    zipLog(fileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.d(TAG, "readTTLog: Open the BufferReader Failed");
                } catch (IOException e) {
                    Log.d(TAG, "readTTLog: readLine Exception");
                    e.printStackTrace();
                }finally {
                    if (bufferedReader != null){
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 将读到的TT log 存储到你自定义的目录下面
     * @param saveDir 自定义的存储路径
     */
    public static void readTTLog(final String saveDir, final String fileName){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start read the ttLog");
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new FileReader(TT_LOG_PORT));
                    //StringBuilder builder = new StringBuilder();
                    String log;
                    while (((log = bufferedReader.readLine()) != null) && isRead){
                        Log.d(TAG, "run: isRead = " + isRead +" ==== log ==== " + log);
                        //builder.append(log);
                        //builder.append("\r\n");
                        saveTTLog(saveDir, log);
                    }
                    //saveTTLog(saveDir, builder.toString());
                    //Log.d(TAG, "run: save the ttLog Success get the ttLog = " + builder.toString());
                    zipLog(fileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.d(TAG, "readTTLog: Open the BufferReader Failed");
                } catch (IOException e) {
                    Log.d(TAG, "readTTLog: readLine Exception");
                    e.printStackTrace();
                }finally {
                    if (bufferedReader != null){
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public static boolean upload(String path){
        File uploadFile = new File(path);
        if (!uploadFile.exists()){
            uploadFile.setWritable(true);
            uploadFile.mkdirs();
        }
        List<File> fileList = new ArrayList<File>();
        fileList.add(uploadFile);

        /**upload the file**/
        return FTPUtil.uploadFile(fileList);
    }
}
