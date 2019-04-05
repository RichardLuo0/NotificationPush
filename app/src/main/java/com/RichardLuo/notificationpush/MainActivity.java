package com.RichardLuo.notificationpush;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
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

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    boolean isEnabled;
    Switch Swh;
    EditText input;
    TextView DeviceID;
    Button clear;
    public static String inputID;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Swh = findViewById(R.id.switch1);
        input = findViewById(R.id.editText);
        DeviceID = findViewById(R.id.textView);
        clear = findViewById(R.id.clear);
        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    for (NotificationChannel channel : notificationManager.getNotificationChannels()
                    ) {
                        notificationManager.deleteNotificationChannel(channel.getId());
                    }
                }
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
        preferences = getPreferences(MODE_PRIVATE);
        input.setText(preferences.getString("ID", ""));
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
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Intent service = new Intent(this, GetNotification.class);
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isChecked) {
            if (isEnabled) {
                if (!input.getText().toString().trim().isEmpty()) {
                    inputID = input.getText().toString();
                    editor = preferences.edit();
                    editor.putString("ID", inputID);
                    editor.apply();
                    startService(service);
                } else {
                    Toast.makeText(this, "请填写设备ID", Toast.LENGTH_SHORT).show();
                    Swh.setChecked(false);
                }
            } else {
                startActivity(intent);
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
