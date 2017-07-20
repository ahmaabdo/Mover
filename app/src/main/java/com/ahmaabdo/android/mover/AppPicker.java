package com.ahmaabdo.android.mover;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Ahmad on Jul 19, 2017.
 */

public class AppPicker extends AsyncTask<Void, Void, Void> {

    List<Drawable> icons;
    List<ApplicationInfo> apps;
    PackageManager pm;
    private ProgressDialog progress;
    final MainActivity mainActivity;


    public AppPicker(final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


    @Override
    protected void onPreExecute() {
        pm = mainActivity.getPackageManager();
        progress = ProgressDialog.show(mainActivity, "", "Loading apps", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancel(true);
                progress.dismiss();
            }
        });
    }


    @Override
    protected void onPostExecute(Void a) {
        try {
            progress.cancel();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG) Logger.log(e);
        }
        if (apps == null || apps.isEmpty()) {
            mainActivity.showErrorDialog("Error while loading apps!");
        } else {
            ListView listView = (ListView) mainActivity.findViewById(R.id.apps);
            listView.setAdapter(new EfficientAdapter(mainActivity, this));
            listView.setOnItemClickListener(new AppClickListener(this));
        }
    }


    @Override
    protected Void doInBackground(Void... params) {
        //Load all apps and their icons, sort them alphabetical
        apps = pm.getInstalledApplications(0);
        if (!MainActivity.SHOW_SYSTEM_APPS) {
            Iterator<ApplicationInfo> it = apps.iterator();
            ApplicationInfo app;
            while (it.hasNext()) {
                app = it.next();
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                    it.remove();
            }
        }

        try {
            Collections.sort(apps, new Comparator<ApplicationInfo>() {
                @Override
                public int compare(ApplicationInfo app1, ApplicationInfo app2) {
                    try {
                        return app1.loadLabel(pm).toString().toLowerCase()
                                .compareTo(app2.loadLabel(pm).toString().toLowerCase());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                }
            });
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        icons = new ArrayList<Drawable>(apps.size());
        try {
            for (int i = 0; i < apps.size(); i++) {
                icons.add(apps.get(i).loadIcon(pm));

            }
        } catch (OutOfMemoryError oome) {
            oome.printStackTrace();
        }
        return null;
    }
}
