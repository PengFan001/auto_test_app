package com.jiaze.at;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;
import java.util.logging.Handler;

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

   private Context mContext;
   private Handler mHandler;
   private String mSolDeviceName;
   private String mUnsolDeviceName;

   private boolean mUseFakeData = true;
   private LinkedList<Message> mCommandList;

   public AtSender(Context context, Handler handler){
       this.mContext = context;
       this.mHandler = handler;
       mCommandList = new LinkedList<Message>();
       jni_init();
   }

   private void sendATCommand(String command, Message message, boolean clear){
        synchronized (mCommandList){
            if (command == null){
                Log.d(TAG, "sendATCommand: command is null, send the AT command error");
                return;
            }

            message.obj = command;
            if (clear){
                mCommandList.clear();
            }
            mCommandList.add(message);
            int result = jni_send_at_command(command);
            Log.d(TAG, "sendATCommand: SendATCommand Result = " + result);
        }
   }

   private native int jni_init();

    private native int jni_open_at_device(String solDevName, String unsolDevName);

    private native int jni_close_at_device();

    private native int jni_send_at_command(String str);

}
