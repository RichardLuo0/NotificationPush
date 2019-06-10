package com.RichardLuo.notificationpush;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static SharedPreferences preferences;

    private final static String[] QQNames = new String[]{"com.tencent.mobileqq", "com.tencent.tim", "com.tencent.mobileqqi", "com.tencent.qqlite", "com.tencent.minihd.qq"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getPreferences(MODE_PRIVATE);
        setTheme(preferences.getInt("style", R.style.base_AppTheme_teal));
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preference, new Preferences())
                .commit();

        List<ApplicationInfo> packageInfo = this.getPackageManager().getInstalledApplications(0);
        String installedQQ = preferences.getString("installedQQ", "");
        String current = null;
        for (ApplicationInfo info : packageInfo) {
            if (info.packageName.equals(installedQQ)) break;
            for (String QQName : QQNames) {
                if (QQName.equals(info.packageName) && info.enabled) {
                    current = info.packageName;
                    preferences.edit().putString("installedQQ", current).apply();
                    break;
                }
            }
            if (current != null) break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.color:
                final String[] items = {"水鸭青", "姨妈红", "哔哩粉", "基佬紫", "很深蓝", "非常黄", "真的灰"};
                AlertDialog.Builder listDialog = new AlertDialog.Builder(this);
                listDialog.setTitle("选择颜色");
                listDialog.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int click) {
                        switch (click) {
                            case 0:
                                preferences.edit().putInt("style", R.style.base_AppTheme_teal).apply();
                                preferences.edit().putInt("color", R.color.teal).apply();
                                break;
                            case 1:
                                preferences.edit().putInt("style", R.style.base_AppTheme_red).apply();
                                preferences.edit().putInt("color", R.color.red).apply();
                                break;
                            case 2:
                                preferences.edit().putInt("style", R.style.base_AppTheme_pink).apply();
                                preferences.edit().putInt("color", R.color.pink).apply();
                                break;
                            case 3:
                                preferences.edit().putInt("style", R.style.base_AppTheme_purple).apply();
                                preferences.edit().putInt("color", R.color.purple).apply();
                                break;
                            case 4:
                                preferences.edit().putInt("style", R.style.base_AppTheme_blue).apply();
                                preferences.edit().putInt("color", R.color.blue).apply();
                                break;
                            case 5:
                                preferences.edit().putInt("style", R.style.base_AppTheme_yellow).apply();
                                preferences.edit().putInt("color", R.color.yellow).apply();
                                break;
                            case 6:
                                preferences.edit().putInt("style", R.style.base_AppTheme_grey).apply();
                                preferences.edit().putInt("color", R.color.grey).apply();
                                break;
                        }
                        Toast.makeText(getApplicationContext(), "必须重启应用", Toast.LENGTH_SHORT).show();
                    }
                });
                listDialog.show();
                break;
            case R.id.about:
                final AlertDialog.Builder normalDialog = new AlertDialog.Builder(this);
                normalDialog.setTitle("关于");
                normalDialog.setMessage(getResources().getString(R.string.HowToUse));
                normalDialog.setPositiveButton("捐赠", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent().setData(Uri.parse("alipays://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=https://qr.alipay.com/fkx0746746ugqrzxkrle7c0?_s=web-other")).setAction("android.intent.action.VIEW"));
                    }
                });
                normalDialog.setNeutralButton("GITHUB", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent().setData(Uri.parse("https://github.com/CJieLuo/NotificationPush")).setAction("android.intent.action.VIEW"));
                    }
                });
                normalDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
