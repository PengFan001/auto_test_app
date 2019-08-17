package com.jiaze.common;

import android.content.Context;
import android.util.Log;

import com.jiaze.autotestapp.R;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.common
 * Create by jz-pf
 * on 2019/8/16
 * =========================================
 */
public class FTPUtil {

    private static final String FTP_SET_PARAMS = "FTPParams";
    private static final String TAG = "FTPUtil";
    private static final String REMOTE_PATH = "testResult";
    public static String ftpIp = "203.156.236.67";
    public static int ftpPort = 10021;
    public static String ftpUser = "jzlab";
    public static String ftpPassword = "jzlab";

    private String ip;
    private int port;
    private String user;
    private String password;
    private FTPClient ftpClient;

    public FTPUtil(String ip, int port, String user, String password){
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public static boolean uploadFile(Context context, List<File> fileList){
        loadFTPSetParams(context);
        FTPUtil ftpUtil = new FTPUtil(ftpIp, ftpPort, ftpUser, ftpPassword);
        Log.d(TAG, "uploadFile: start connect the ftp server, and upload the file");
        boolean result = ftpUtil.uploadFile(REMOTE_PATH, fileList);
        Log.d(TAG, "uploadFile: upload the file finished, the upload result = " + result);
        return result;
    }

    public boolean uploadFile(String remotePath, List<File> fileList){
        boolean uploaded = true;
        FileInputStream fileInputStream = null;

        /**connect the ftp server**/
        if (connectFTPServer(this.ip, this.port, this.user, this.password)){
            try {
                ftpClient.setBufferSize(1024);
                ftpClient.setControlEncoding("UTF-8");
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                //ftpClient.enterLocalPassiveMode();

                /**upload the file to the ftp server**/
                for (File fileItem : fileList){
                    Log.d(TAG, "uploadFile: get the fileList fileItem = " + fileItem.getName());
                    fileInputStream = new FileInputStream(fileItem);
                    ftpClient.storeFile(fileItem.getName(), fileInputStream);
                }
                fileInputStream.close();
                ftpClient.disconnect();
            } catch (IOException e) {
                Log.d(TAG, "uploadFile: upload file exception");
                e.printStackTrace();
                uploaded = false;
            }
        }

        return uploaded;
    }

    private boolean connectFTPServer(String ip, int port, String user, String password){
        ftpClient = new FTPClient();
        boolean isLogin = false;
        Log.d(TAG, "connectFTPServer: ip = " + ip + "   port = " + port + "   user = " + user + "   password = " + password);

        try {
            ftpClient.connect(ip, port);
            isLogin = ftpClient.login(user, password);
        } catch (IOException e) {
            Log.d(TAG, "connectFTPServer: connect the ftp server exception");
            e.printStackTrace();
        }

        return isLogin;
    }

    public boolean connectFTPServer(){
        ftpClient = new FTPClient();
        boolean isLogin = false;
        Log.d(TAG, "connectFTPServer: ip = " + ip + "   port = " + port + "   user = " + user + "   password = " + password);

        try {
            ftpClient.connect(ip, port);
            isLogin = ftpClient.login(user, password);
        } catch (IOException e) {
            Log.d(TAG, "connectFTPServer: connect the ftp server exception");
            e.printStackTrace();
            isLogin = false;
        }

        return isLogin;
    }

    private static void loadFTPSetParams(Context context){
        Properties properties = Constant.loadTestParameter(context, FTP_SET_PARAMS);
        ftpIp = properties.getProperty(context.getString(R.string.key_ftp_ip), null);
        ftpPort = Integer.parseInt(properties.getProperty(context.getString(R.string.key_ftp_port), "21"));
        ftpUser = properties.getProperty(context.getString(R.string.key_ftp_username), null);
        ftpPassword = properties.getProperty(context.getString(R.string.key_ftp_password), null);
        Log.d(TAG, "loadFTPSetParams: ftpIp = " + ftpIp + "  ftpPort = " + ftpPort + "   ftpUser = " + ftpUser + "   ftpPassword = " + ftpPassword);
}

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }
}
