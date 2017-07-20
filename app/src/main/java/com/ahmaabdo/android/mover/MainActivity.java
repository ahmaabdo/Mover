package com.ahmaabdo.android.mover;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.stericson.RootTools.RootTools;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public final static String SYSTEM_FOLDER_1 = "/system/priv-app/";
    public final static String SYSTEM_FOLDER_2 = "/system/app/";

    public final static String SYSTEM_DIR_TARGET =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                    SYSTEM_FOLDER_1 : SYSTEM_FOLDER_2;

    public static boolean SHOW_SYSTEM_APPS = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RootTools.debugMode = false;
        checkForRoot();

    }


    /**
     * shows an error dialog with the specified text
     *
     * @param text the error text
     */
    void showErrorDialog(final String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error).setMessage(text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).create().show();
    }


    /**
     * Show the initial warning dialog
     */
    void showWarningDialog() {
        final AppCompatDialog appCompatDialog = new AppCompatDialog(this);
        appCompatDialog.setTitle(getString(R.string.warning_text));
        appCompatDialog.setCancelable(false);
        appCompatDialog.setContentView(R.layout.warning_dialog);

        final CheckBox checkBox = (CheckBox) appCompatDialog.findViewById(R.id.warning_checkbox);
        final Button button = (Button) appCompatDialog.findViewById(R.id.button);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                button.setText(isChecked ? android.R.string.ok : android.R.string.cancel);
            }
        });
        button.setText(android.R.string.cancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkBox.isChecked()) {
                    getSharedPreferences("settings", MODE_PRIVATE).edit()
                            .putBoolean("warningRead", true).commit();
                    appCompatDialog.dismiss();
                } else {
                    appCompatDialog.dismiss();
                    finish();
                }
            }
        });
        appCompatDialog.show();

    }


    /**
     * Uses the RootTools library to check for root and BusyBox
     */
    private void checkForRoot() {
        final ProgressDialog progressDialog =
                ProgressDialog.show(this, "", getString(R.string.waiting_for_root_access), true);
        progressDialog.show();
        final TextView error = (TextView) findViewById(R.id.error);
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean systemLessRoot = new File("/su").exists();
                if (!systemLessRoot && !RootTools.isRootAvailable()) {
                    if (progressDialog == null || !progressDialog.isShowing())
                        return;
                    progressDialog.cancel();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            error.setText(
                                    R.string.not_rooted_text);
                            //Ask user to delete app on non-rooted devices
                            error.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:com.ahmaabdo.android.mover")));
                                }
                            });

                        }
                    });
                    return;
                }
                final boolean root = systemLessRoot || RootTools.isAccessGiven();
                if (progressDialog == null || !progressDialog.isShowing())
                    return;
                progressDialog.cancel();
                handler.post(new Runnable() {
                                 @Override
                                 public void run() {
                                     if (root) {
                                         ((CheckBox) findViewById(R.id.root)).setChecked(true);
                                     } else {
                                         error.setText(R.string.recheck_root_access);
                                         error.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View v) {
                                                 checkForRoot();
                                             }
                                         });
                                         return;
                                     }
                                     if (new File("/su/xbin/busybox").exists() ||
                                             RootTools.isBusyboxAvailable()) {
                                         CheckBox busyBox = (CheckBox) findViewById(R.id.busybox);
                                         busyBox.setChecked(true);
                                         busyBox.setText("BusyBox: " + RootTools.getBusyBoxVersion());
                                     } else {
                                         error.setText(R.string.busybox_not_found);
                                         error.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View v) {
                                                 try {
                                                     RootTools.offerBusyBox(MainActivity.this);
                                                 } catch (ActivityNotFoundException anfe) {
                                                     MainActivity.this.startActivity(
                                                             new Intent(Intent.ACTION_VIEW, Uri.parse(
                                                                     getString(R.string.busy_box_link_playstore))));
                                                 }
                                                 finish();
                                             }
                                         });
                                     }
                                     if (root) {
                                         new AppPicker(MainActivity.this).execute();
                                         if (!getSharedPreferences("settings", MODE_PRIVATE)
                                                 .getBoolean("warningRead", false)) {
                                             showWarningDialog();
                                         }
                                         error.setText(
                                                 R.string.warning_use_risk);
                                         final CheckBox showSystem = (CheckBox) findViewById(R.id.showsystem);
                                         showSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                             @Override
                                             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                 SHOW_SYSTEM_APPS = isChecked;
                                                 new AppPicker(MainActivity.this).execute();

                                             }
                                         });
                                     }

                                 }
                             }

                );
            }
        }

        ).start();
    }

}
