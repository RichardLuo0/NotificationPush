package com.RichardLuo.notificationpush;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.util.List;

public class QQInstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();
        for (String qqName : Const.QQ_NAMES) {
            if (qqName.equals(packageName)) {
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED))
                    context.getSharedPreferences("MainActivity", MODE_PRIVATE).edit().putString("installedQQ", packageName).apply();
                else
                    context.getSharedPreferences("MainActivity", MODE_PRIVATE).edit().putString("installedQQ", null).apply();
                break;
            }
        }
    }

    static void findQQPackage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("MainActivity", MODE_PRIVATE);
        List<ApplicationInfo> packageInfo = context.getPackageManager().getInstalledApplications(0);
        String installedQQ = preferences.getString("installedQQ", "");
        String current = null;
        for (ApplicationInfo info : packageInfo) {
            if (info.packageName.equals(installedQQ)) break;
            for (String QQName : Const.QQ_NAMES) {
                if (QQName.equals(info.packageName) && info.enabled) {
                    current = info.packageName;
                    preferences.edit().putString("installedQQ", current).apply();
                    break;
                }
            }
            if (current != null) break;
        }
    }
}

