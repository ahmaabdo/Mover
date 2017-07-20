package com.ahmaabdo.android.mover;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by Ahmad on Jul 19, 2017.
 */

public class Logger {

    private static FileWriter fw;
    private static Date date = new Date();
    private final static String APP = "SystemAppMover";

    public static void log(Throwable ex) {
        log(ex.getMessage());
        for (StackTraceElement ste : ex.getStackTrace()) {
            log(ste.toString());
        }
    }

    @SuppressWarnings("deprecation")

    public static void log(String msg) {
        if (!BuildConfig.DEBUG)
            return;
        Log.d(APP, msg);
        try {
            if (fw == null) {
                fw = new FileWriter(new File(Environment.getExternalStorageDirectory().toString()));

            }
            date.setTime(System.currentTimeMillis());
            fw.write(date.toLocaleString() + " - " + msg + "\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (fw != null)
                fw.close();
        } finally {
            super.finalize();
        }
    }
}
