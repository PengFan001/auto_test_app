package com.jiaze.at;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.at
 * Create by jz-pf
 * on 2019/7/25
 * =========================================
 */
public class AtSender {

   private static final String TAG = "AtSender";

   private static final String AT_DEVICE_NAME_PREFIX = "dev/STTYEMS";
   private static final String DEFAULT_DEVICE_NUMBER = "42";
   private static final int MESSAGE_ATSENDER_START = 1000;
   private static final int MESSAGE_WAIT_INIT = MESSAGE_ATSENDER_START;
   private static final int MESSAGE_RECEIVED_RESPONSE = MESSAGE_ATSENDER_START + 1;
   private static final int MESSAGE_RECEIVED_UNSOL = MESSAGE_ATSENDER_START + 2;
   private static final int MESSAGE_DISABLE_UNSOL_COMPLETE = MESSAGE_ATSENDER_START + 3;
   public static final int MSG_UNSOL_SENDSMS_COMPLETED = 206;

   private Context mContext;
   private Handler mHandler;
   private String mSolDeviceName;
   private String mUnsolDeviceName;
   private int mInitWaitTimes = 0;
   private boolean mIsPhoneReady = false;
   private static final int INIT_WAIT_MAX_TIMES = 20;

   private boolean mUseFakeData = true;
   private LinkedList<Message> mCommandList;

    static {
        try {
            System.loadLibrary("survive");
        } catch (UnsatisfiedLinkError e) {
            Log.e("survive", "survive library not found!");
        }
    }

   public AtSender(Context context, Handler handler){
       this.mContext = context;
       this.mHandler = handler;
       mCommandList = new LinkedList<Message>();
       getAtDeviceName();
       jni_init();
       openAtDevice();
   }

   public int sendATCommand(String command, Message message, boolean clear){
        synchronized (mCommandList){
            if (command == null){
                Log.d(TAG, "sendATCommand: command is null, send the AT command error");
                return -1;
            }

            message.obj = command;
            if (clear){
                mCommandList.clear();
            }
            mCommandList.add(message);
            int result = jni_send_at_command(command);
            Log.d(TAG, "sendATCommand: command = " + command);
            Log.d(TAG, "sendATCommand: SendATCommand Result = " + result);
            return result;
        }
   }

   private void getAtDeviceName(){
       String prop = SystemProperties.get("persist.sys.ttynum.survive", "null");
       Log.d(TAG, "getAtDeviceName: prop = " + prop);
       if (prop.equals("null")) {
           prop = DEFAULT_DEVICE_NUMBER;
       }
       mSolDeviceName = AT_DEVICE_NAME_PREFIX;
       mSolDeviceName += prop;

       mUnsolDeviceName = AT_DEVICE_NAME_PREFIX;
       mUnsolDeviceName += (Integer.valueOf(prop) + 1);
       Log.d(TAG, "getAtDeviceName: sol:" + mSolDeviceName + " unsol:"
               + mUnsolDeviceName);
   }

    public void destory() {
        Log.d(TAG, "destory");
        closeAtDevice();
        mIsPhoneReady = false;
    }

    public void closeAtDevice() {
        Log.d(TAG, "closeAtDevice: ");
        jni_close_at_device();
    }

   private void processResponse(String response){
        synchronized (mCommandList){
            Log.d(TAG, "processResponse: " + response);
            String[] lines = response.split("\\|");
            if (!mCommandList.isEmpty()){
                Message message = mCommandList.removeFirst();
                Log.d(TAG, "processResponse: message.what = " + message.what);
                if (message == null){
                    Log.d(TAG, "processResponse: error, message = null");
                    return;
                }
                message.arg1 = 0;
                for (int i = 0; i < lines.length; i++){
                    if ("OK".equals(lines[i])){
                        message.arg1 = 1;
                        break;
                    }
                }

                Log.d(TAG, "processResponse: message: command:" + (String) message.obj
                            + " arg1=" + message.arg1 + ". lines:" + lines);
                
                if (message.obj == null){
                    Log.d(TAG, "processResponse: error, message.obj = null");
                    return;
                }
                
                message.obj = lines;
                message.sendToTarget();

            }else {
                Log.d(TAG, "processResponse: This is an unsol command, we ignore it");
            }
        }
   }

    private void processUnsol(String unsolString) {
        Log.d(TAG, "processUnsol: " + unsolString);
        if (unsolString.startsWith("+EMGSMS")) {
            String result = unsolString.substring(unsolString.length() - 1,
                    unsolString.length());
            sendMessage(MSG_UNSOL_SENDSMS_COMPLETED, result);
        }
    }

    public void handleMessage(Message msg) {
        Log.d(TAG, "handleMessage: get the message. msg.what = " + msg.what);
        switch (msg.what) {
            case MESSAGE_WAIT_INIT:
                Log.d(TAG, "handleMessage: MESSAGE_WAIT_INIT");
                openAtDevice();
                break;
            case MESSAGE_RECEIVED_RESPONSE:
                Log.d(TAG, "handleMessage: MESSAGE_RECEIVED_RESPONSE");
                processResponse((String) msg.obj);
                break;

            case MESSAGE_RECEIVED_UNSOL:
                Log.d(TAG, "handleMessage: MESSAGE_RECEIVED_UNSOL");
                processUnsol((String) msg.obj);
                break;

            case MESSAGE_DISABLE_UNSOL_COMPLETE:
                Log.d(TAG, "handleMessage: [disable unsol] complete result:" + msg.arg1);
                break;

            default:
                break;
        }
    }

    public void jniReceivedOneRespPacket(byte[] data, int datalen) {

        Log.d(TAG, "jniReceivedOneRespPacket: datalen = " + datalen + "data[0]" + data[0]);
        try {
            String isoString = new String(data, 0, datalen, "ISO-8859-1");
            Log.d(TAG, "jniReceivedOneRespPacket: String:" + isoString);
            sendMessage(MESSAGE_RECEIVED_RESPONSE, isoString);
        } catch (IOException e) {
            Log.d(TAG, "jniReceivedOneRespPacket: isoString:error!!!");
        }
    }

    public void jniReceivedOneUnsolPacket(byte[] data, int datalen) {

        try {
            String isoString = new String(data, 0, datalen, "ISO-8859-1");
            Log.d(TAG, "jniReceivedOneUnsolPacket: String:" + isoString);
            sendMessage(MESSAGE_RECEIVED_UNSOL, isoString);
        } catch (IOException e) {
            Log.d(TAG, "jniReceivedOneUnsolPacket: isoString:error!!!");
        }
    }

    private void disableUnsolCommands() {
        Log.d(TAG, "disableUnsolCommands: ");
        sendATCommand("AT^DLKS=0",
                mHandler.obtainMessage(MESSAGE_DISABLE_UNSOL_COMPLETE), false);
    }

    private void sendMessage(int msgId, String string) {
        Message msg = Message.obtain();
        msg.what = msgId;
        msg.obj = string;
        mHandler.sendMessageDelayed(msg, 0);
    }

    public void openAtDevice() {
        if (jni_open_at_device(mSolDeviceName, mUnsolDeviceName) < 0) {
            if (mInitWaitTimes < INIT_WAIT_MAX_TIMES) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_WAIT_INIT, 1000);
            } else {
                mInitWaitTimes = 0;
                Log.d(TAG, "openAtDevice: Init at device failed:" + mSolDeviceName + " unsol:"
                        + mUnsolDeviceName);
                return;
            }
        } else {
            Log.d(TAG, "openAtDevice: Open device success!");
            mIsPhoneReady = true;
            disableUnsolCommands();
        }
        mInitWaitTimes++;
    }

    private native int jni_init();

    private native int jni_open_at_device(String solDevName, String unsolDevName);

    private native int jni_close_at_device();

    private native int jni_send_at_command(String str);

}
