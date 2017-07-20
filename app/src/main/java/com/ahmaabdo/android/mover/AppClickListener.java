package com.ahmaabdo.android.mover;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;

import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.List;

/**
 * Created by Ahmad on Jul 19, 2017.
 */

public class AppClickListener implements AdapterView.OnItemClickListener {

    private final AppPicker ap;


    public AppClickListener(final AppPicker ap) {
        this.ap = ap;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view,
                            int position, long id) {
        if (position >= ap.apps.size())
            return;

        if ("MOVED".equals(view.getTag())) {
            ap.mainActivity.showErrorDialog(view.getResources().getString(R.string.require_reboot_text));
            return;
        }

        ApplicationInfo tmp = ap.apps.get(position);
        boolean tmpAlreadySys = (tmp.flags & ApplicationInfo.FLAG_SYSTEM) == 1;

        //Update necessary?
        if ((tmpAlreadySys && tmp.sourceDir.contains("/data/app/"))
                || (!tmpAlreadySys && tmp.sourceDir.contains("/system/"))) {
            try {
                tmp = ap.pm.getApplicationInfo(tmp.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                ap.mainActivity.showErrorDialog(view.getResources().getString(R.string.app_not_found));
                if (BuildConfig.DEBUG)
                    Logger.log(e);
                return;
            }
        }

        final ApplicationInfo app = tmp;
        final String appName = (String) app.loadLabel(ap.pm);
        final boolean alreadySys = (app.flags & ApplicationInfo.FLAG_SYSTEM) == 1;

        if (BuildConfig.DEBUG) Logger.log("Trying to move " + appName + " - " + app.packageName);
        if (app.packageName.equals(ap.mainActivity.getPackageName())) {
            ap.mainActivity.showErrorDialog(view.getResources().getString(R.string.cant_move_me));
            if (BuildConfig.DEBUG) Logger.log(view.getResources().getString(R.string.cant_move_me));
            return;
        }
        if (alreadySys && app.sourceDir.contains("/data/app/")) {
            if (BuildConfig.DEBUG) Logger.log("Need to remove updates first");
            AlertDialog.Builder builder = new AlertDialog.Builder(ap.mainActivity);
            builder.setTitle("Error")
                    .setMessage(view.getResources().getString(R.string.cant_move)  + appName + view.getResources().getString(R.string.remove_installed_first))
                    .setPositiveButton("Remove updates", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int id) {
                            try {
                                ap.mainActivity.startActivity(new Intent(Intent.ACTION_DELETE,
                                        Uri.parse("package:" + app.packageName)));
                                dialog.dismiss();
                            } catch (Exception e) {
                            }
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, int id) {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                    }
                }
            });
            builder.create().show();
            return;
        } else if (!alreadySys && tmp.sourceDir.contains("/system/")) {
            ap.mainActivity.showErrorDialog(view.getResources().getString(R.string.cant_move) + appName +
                    ": Undefined app status. You might need to reboot once.");
            if (BuildConfig.DEBUG) Logger.log(
                    "Undefined app status: IsSystem = " + alreadySys + " path = " + tmp.sourceDir);
            return;
        }

        String warning = null;

        if (!alreadySys && app.sourceDir.endsWith("pkg.apk")) {
            if (app.sourceDir.contains("asec")) {
                if (BuildConfig.DEBUG) Logger.log("Encrypted app? Path = " + app.sourceDir);
                warning = appName +
                        view.getResources().getString(R.string.encrypted_app);
            } else {
                if (BuildConfig.DEBUG) Logger.log("SD card? " + app.sourceDir);
                ap.mainActivity.showErrorDialog(appName +
                        " is currently installed on SD card. Please move to internal memory before moving to /system/app/");
                return;
            }
        }

        AlertDialog.Builder b = new AlertDialog.Builder(ap.mainActivity);
        b.setMessage("Convert " + appName + " to " + (alreadySys ? "normal" : "system") + " app?" +
                (warning != null ? "\n\nWarning: " + warning : ""));
        b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (RootTools.remount("/system", "rw")) {
                            try {
                                if (BuildConfig.DEBUG)
                                    Logger.log("process name: " + app.processName);

                                ActivityManager activityManager = (ActivityManager) ap.mainActivity
                                        .getSystemService(Context.ACTIVITY_SERVICE);
                                List<ActivityManager.RunningAppProcessInfo> runningProcInfo =
                                        activityManager.getRunningAppProcesses();
                                String[] pkgList;
                                for (ActivityManager.RunningAppProcessInfo p : runningProcInfo) {
                                    pkgList = p.pkgList;
                                    for (String pkg : pkgList) {
                                        if (pkg.equals(app.processName)) {
                                            if (BuildConfig.DEBUG)
                                                Logger.log("killing: " + p.processName);
                                            RootTools.killProcess(p.processName);
                                            break;
                                        }
                                    }
                                }

                                if (BuildConfig.DEBUG) Logger.log("source: " + app.sourceDir);
                                if (!new File(app.sourceDir).exists()) {
                                    if (BuildConfig.DEBUG) Logger.log("source does not exist?!?");
                                    ap.mainActivity.showErrorDialog("Can not access source file");
                                    return;
                                }

                                String fallbackFilename = appName.replaceAll("[^a-zA-Z0-9]+", "");

                                String newFile;
                                List<String> output;
                                if (!alreadySys) {
                                    if (app.sourceDir.endsWith("/pkg.apk") ||
                                            app.sourceDir.endsWith("/base.apk")) {
                                        newFile =
                                                MainActivity.SYSTEM_DIR_TARGET + fallbackFilename +
                                                        ".apk";
                                    } else {
                                        newFile = app.sourceDir.replace("/data/app/",
                                                MainActivity.SYSTEM_DIR_TARGET);
                                    }
                                } else {
                                    if (app.sourceDir.endsWith("/pkg.apk")) {
                                        newFile = "/data/app/" + app.packageName + ".apk";
                                    } else {
                                        if (app.sourceDir.contains(MainActivity.SYSTEM_FOLDER_1)) {
                                            newFile = app.sourceDir
                                                    .replace(MainActivity.SYSTEM_FOLDER_1,
                                                            "/data/app/");
                                        } else {
                                            newFile = app.sourceDir
                                                    .replace(MainActivity.SYSTEM_FOLDER_2,
                                                            "/data/app/");
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            String filename =
                                                    newFile.substring(newFile.lastIndexOf("/") + 1);
                                            if (BuildConfig.DEBUG)
                                                Logger.log("filename: " + filename);
                                            if (filename.equals("pkg.apk") ||
                                                    filename.equals("base.apk")) {
                                                filename = fallbackFilename;
                                            }
                                            newFile = "/data/app/" + filename;
                                        }
                                    }
                                }
                                String oldFile = app.sourceDir;
                                String cpcmd = "busybox cp " + oldFile + " " + newFile;
                                if (BuildConfig.DEBUG) Logger.log("command: " + cpcmd);

                                output = RootTools.sendShell(cpcmd, 10000);

                                if (output.size() > 1) {
                                    String error = "Error: ";
                                    for (String str : output) {
                                        if (str.length() > 1) {
                                            error += "\n" + str;
                                        }
                                    }
                                    if (BuildConfig.DEBUG) Logger.log(error);
                                    ap.mainActivity.showErrorDialog(error);
                                } else {
                                    File f = new File(newFile);

                                    for (int i = 0; f.length() < 1 && i < 20; i++) {
                                        Thread.sleep(100);
                                    }

                                    if (BuildConfig.DEBUG) Logger.log(
                                            "file " + f.getAbsolutePath() + " size: " + f.length());

                                    if (f.length() > 1) {

                                        if (BuildConfig.DEBUG) Logger.log("changing chmod");
                                        output = RootTools
                                                .sendShell("busybox chmod 644 " + newFile, 5000);
                                        if (BuildConfig.DEBUG) {
                                            for (String str : output) {
                                                Logger.log(str);
                                            }
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            if (BuildConfig.DEBUG)
                                                Logger.log("changing chown and chgrp");
                                            output = RootTools.sendShell(
                                                    new String[]{"busybox chown system " + newFile,
                                                            "busybox chgrp system " + newFile}, 500,
                                                    5000);
                                            if (BuildConfig.DEBUG) {
                                                for (String str : output) {
                                                    Logger.log(str);
                                                }
                                            }
                                        }

                                        view.setVisibility(View.GONE);
                                        view.setTag("MOVED");
                                        AlertDialog.Builder b2 =
                                                new AlertDialog.Builder(ap.mainActivity);
                                        b2.setMessage(appName +
                                                " successfully moved, you need to reboot your device.\nReboot now?");
                                        if (BuildConfig.DEBUG) Logger.log("successfully moved");
                                        b2.setPositiveButton(android.R.string.yes,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            final DialogInterface dialog,
                                                            int which) {
                                                        if (BuildConfig.DEBUG)
                                                            Logger.log("reboot now");
                                                        ap.mainActivity.sendBroadcast(new Intent(
                                                                "de.j4velin.ACTION_SHUTDOWN"));
                                                        try {
                                                            dialog.dismiss();
                                                        } catch (Exception e) {
                                                        }
                                                        try {
                                                            RootTools.sendShell(
                                                                    "am broadcast -a android.intent.action.ACTION_SHUTDOWN",
                                                                    5000);
                                                            try {
                                                                Thread.sleep(1000);
                                                            } catch (InterruptedException e) {
                                                            }
                                                            RootTools.sendShell("reboot", 5000);
                                                        } catch (Exception e) {
                                                        }
                                                    }
                                                });
                                        b2.setNegativeButton(android.R.string.no,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            final DialogInterface dialog,
                                                            int which) {
                                                        if (BuildConfig.DEBUG)
                                                            Logger.log("no reboot");
                                                        try {
                                                            dialog.dismiss();
                                                        } catch (Exception e) {
                                                        }
                                                    }
                                                });
                                        b2.create().show();
                                        String deletecmd = "busybox rm " + oldFile;
                                        if (BuildConfig.DEBUG) Logger.log("command: " + deletecmd);
                                        RootTools.sendShell(deletecmd, 10000);
                                    } else {
                                        ap.mainActivity
                                                .showErrorDialog(appName + " could not be moved");
                                    }
                                }
                            } catch (Exception e) {
                                ap.mainActivity.showErrorDialog(
                                        e.getClass().getName() + " " + e.getMessage());
                                e.printStackTrace();
                                if (BuildConfig.DEBUG) Logger.log(e);
                            } finally {
                                RootTools.remount("/system", "ro");
                                RootTools.remount("/mnt", "ro");
                            }
                        } else {
                            if (BuildConfig.DEBUG) Logger.log("can not remount target partition");
                            ap.mainActivity.showErrorDialog("Could not remount /system");
                        }
                    }
                }

        );
        b.setNegativeButton(android.R.string.no, new

                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                        } catch (Exception e) {
                        }
                    }
                }

        );
        b.create().

                show();
    }
}