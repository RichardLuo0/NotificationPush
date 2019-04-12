package com.RichardLuo.notificationpush;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.List;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    static String inputID;
    static SharedPreferences preferences;
    boolean isEnabled;
    Switch Swh;
    EditText input;
    TextView DeviceID;
    Button clear;
    Button colors;
    Button about;

    final static String[] QQNames = new String[]{"com.tencent.mobileqq", "com.tencent.tim", "com.tencent.mobileqqi", "com.tencent.qqlite", "com.tencent.minihd.qq"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getPreferences(MODE_PRIVATE);
        setTheme(preferences.getInt("style", R.style.base_AppTheme_teal));
        setContentView(R.layout.activity_main);
        Swh = findViewById(R.id.switch1);
        input = findViewById(R.id.editText);
        DeviceID = findViewById(R.id.textView);
        clear = findViewById(R.id.clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    final NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
                        notificationManager.deleteNotificationChannel(channel.getId());
                    }
                }
            }
        });
        colors = findViewById(R.id.colors);
        colors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = {"水鸭青", "姨妈红", "哔哩粉", "基佬紫", "很深蓝", "非常黄", "真的灰"};
                AlertDialog.Builder listDialog = new AlertDialog.Builder(MainActivity.this);
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
                        Toast.makeText(MainActivity.this, "必须重启应用", Toast.LENGTH_SHORT).show();
                    }
                });
                listDialog.show();
            }
        });
        about = findViewById(R.id.about);
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
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
            }
        });
        Swh.setOnCheckedChangeListener(this);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            DeviceID.setText("fail");
                            return;
                        }
                        DeviceID.setText(task.getResult().getToken());
                    }
                });
        startService(new Intent(this, FCMReceiver.class));
        input.setText(preferences.getString("ID", ""));

        if (preferences.getString("installedQQ", null) == null) {
            List<PackageInfo> info = this.getPackageManager().getInstalledPackages(0);
            for (int i = 0; i < info.size(); i++) {
                String ipackage = info.get(i).packageName;
                for (String QQName : QQNames) {
                    if (QQName.equals(ipackage)) {
                        preferences.edit().putString("installedQQ", QQName).apply();
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isNotificationListenersEnabled()) {
            isEnabled = true;
            Swh.setChecked(true);
        } else {
            isEnabled = false;
        }
        preferences.edit().putString("ID", inputID).apply();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Intent service = new Intent(this, GetNotification.class);
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isChecked) {
            if (!input.getText().toString().trim().isEmpty()) {
                if (isEnabled) {
                    inputID = input.getText().toString();
                    preferences.edit().putString("ID", inputID).apply();
                    startService(service);
                } else {
                    startActivity(intent);
                    Swh.setChecked(false);
                }
            } else {
                Toast.makeText(this, "请填写设备ID", Toast.LENGTH_SHORT).show();
                Swh.setChecked(false);
            }
        } else {
            if (!isEnabled) {
                startActivity(intent);
                stopService(service);
            }
            startActivity(intent);
        }
    }

    public boolean isNotificationListenersEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
