package com.jiaze.common;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.common
 * Create by jz-pf
 * on 2019/8/7
 * =========================================
 */
public class ZipUtil {

    public static final String TAG = "ZipUtil";
    private static final int BUFFER = 8192;
    public ZipUtil(){
        
    }
    
    public static void zip(String srcFilePath, String desFilePath){
        if (TextUtils.isEmpty(srcFilePath) || TextUtils.isEmpty(desFilePath)){
            Log.d(TAG, "zip: Zip file failure, file path wrong");
            return;
        }

        File srcFile = new File(srcFilePath);
        if (!srcFile.exists()){
            Log.d(TAG, "zip: src File not Exist");
            return;
        }
        
        File outputFile = new File(desFilePath);
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(outputFile);
            CheckedOutputStream cos = new CheckedOutputStream(fos, new CRC32());
            zos = new ZipOutputStream(cos);
            zipByType(srcFile, zos, "");
            Log.d(TAG, "zip: success zip the File");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (zos != null){
                try {
                    Log.d(TAG, "zip: CLOSE ZipOutputStream");
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (fos != null){
                try {
                    Log.d(TAG, "zip: CLOSE FileOutputStream");
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void zipByType(File file, ZipOutputStream out, String basedir){
        if (file.isDirectory()){
            Log.d(TAG, "zipByType: type is zipDirectory basedir = " + basedir + "   fileName = " + file.getName());
            zipDirectory(file, out, basedir);
        }else {
            Log.d(TAG, "zipByType: type is zipFile basedir = " + basedir + "   fileName = " + file.getName());
            zipFile(file, out, basedir);
        }
    }

    private static void zipDirectory(File dir, ZipOutputStream out, String basedir){
        Log.d(TAG, "zipDirectory: start zipDirectory ");
        if (!dir.exists()){
            Log.d(TAG, "zipDirectory: the file dir is not directory");
            return;
        }

        File[] files = dir.listFiles();
        for (int i=0; i < files.length; i++){
            zipByType(files[i], out, basedir + dir.getName() + "/");
        }
    }

    private static void zipFile(File file, ZipOutputStream out, String basedir){
        Log.d(TAG, "zipFile: start zip the file");
        BufferedInputStream bins = null;
        if (!file.exists()){
            Log.d(TAG, "zipFile: the file is not exist");
            return;
        }

        try {
            bins = new BufferedInputStream(new FileInputStream(file));
            ZipEntry zipEntry = new ZipEntry(basedir + file.getName());
            out.putNextEntry(zipEntry);
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = bins.read(data, 0, BUFFER)) != -1){
                out.write(data, 0, count);
            }
            Log.d(TAG, "zipFile: zip file Succeed");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (bins != null){
                try {
                    bins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
