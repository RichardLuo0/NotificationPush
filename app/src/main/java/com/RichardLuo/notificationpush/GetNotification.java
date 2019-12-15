package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class GetNotification extends NotificationListenerService {
    protected final String Authorization = "";
    protected final String Sender = "";
    public String inputID;
    PackageManager pm;

    @Override
    public void onCreate() {
        inputID = getDefaultSharedPreferences(this).getString("input", "");
        pm = getPackageManager();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        if (getDefaultSharedPreferences(getApplicationContext()).getBoolean("startForeground", false)) {
            NotificationChannel mChannel;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mChannel = new NotificationChannel("Foreground", "前台服务", IMPORTANCE_DEFAULT);
                Objects.requireNonNull(getSystemService(NotificationManager.class)).createNotificationChannel(mChannel);
            }
            Notification foregroundNotice = new NotificationCompat.Builder(this, "Foreground")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(getResources().getColor(getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("color", R.color.teal)))
                    .setContentTitle("后台转发通知中")
                    .setContentText("转发中")
                    .build();
            startForeground(1, foregroundNotice);
        }
    }

    @Override
    public void onDestroy() {
        if (getDefaultSharedPreferences(getApplicationContext()).getBoolean("startForeground", false)) {
            stopForeground(true);
        }
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        new Thread() {
            @Override
            public void run() {
                String packageName = sbn.getPackageName();
                Notification oneNotification = sbn.getNotification();
                String title = oneNotification.extras.getString(Notification.EXTRA_TITLE, "无标题");
                String body = oneNotification.extras.getCharSequence(Notification.EXTRA_TEXT, "无内容").toString();
                String AppName;
                SharedPreferences sharedPreferences = getSharedPreferences("AppName", MODE_PRIVATE);
                if ((AppName = sharedPreferences.getString(packageName, null)) == null) {
                    try {
                        AppName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        AppName = " ";
                    }
                    sharedPreferences.edit().putString(packageName, AppName).apply();
                }
                int ID = sbn.getId();
                String senderName = null;
                String priority = "normal";

                if (title.contains("正在运行") || title.contains("running") || inputID == null) return;
                if (getDefaultSharedPreferences(getApplicationContext()).getBoolean("hide_no_content", false) && title.equals("无标题") && body.equals("无内容"))
                    return;

                SharedPreferences appPreference = getSharedPreferences("Application", MODE_PRIVATE);
                switch (appPreference.getInt(packageName, appPreference.contains("allOff") ? 2 : 0)) {
                    case 0:
                        priority = "normal";
                        break;
                    case 1:
                        priority = "high";
                        break;
                    case 2:
                        return;
                }

                //此处对单个应用进行单独定义
                switch (packageName) {
                    case "com.RichardLuo.notificationpush":
                        return;
                    case "com.tencent.minihd.qq":
                    case "com.tencent.mobileqqi":
                    case "com.tencent.qqlite":
                    case "com.tencent.tim":
                    case "com.tencent.mobileqq":
                        if (!(title.contains("QQ空间"))) {
                            if (oneNotification.tickerText != null) {
                                String tickerText = oneNotification.tickerText.toString().replace("\n", "");
                                Matcher matcher = Pattern.compile("^(.*?)\\((((?![()]).)*?)\\):(.*?)$").matcher(tickerText);
                                if (matcher.find()) {
                                    senderName = matcher.group(1);
                                    title = matcher.group(2);
                                    body = Objects.requireNonNull(matcher.group(4)).trim();
                                } else {
                                    String[] single = tickerText.split(":", 2);
                                    senderName = single[0];
                                    title = single[0];
                                    if (single.length > 1)
                                        body = single[1].trim();
                                }
                                if (title != null)
                                    ID = StringToA(title);
                            } else
                                return;
                        }
                }


                HttpURLConnection connection;
                try {
                    URL url = new URL("https://fcm.googleapis.com/fcm/send");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", "key=" + Authorization);
                    connection.setRequestProperty("Sender", "id=" + Sender);
                    connection.connect();
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    JSONObject obj = new JSONObject();
                    JSONObject content = new JSONObject();
                    content.put("title", title);
                    content.put("body", body);
                    content.put("package", packageName);
                    content.put("name", AppName);
                    content.put("id", ID);
                    if (senderName != null)
                        content.put("senderName", senderName);
                    obj.put("to", inputID);
                    obj.put("priority", priority);
                    obj.put("data", content);
                    String json = obj.toString();
                    out.write(json.getBytes());
                    out.flush();
                    out.close();
                    connection.getResponseCode();
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e("error:", "Can't send " + packageName + " " + title);
                }
                super.run();
            }
        }.start();
    }

    private static int StringToA(String content) {
        int result = 0;
        int max = content.length();
        for (int i = 0; i < max; i++) {
            char c = content.charAt(i);
            int b = (int) c;
            result = result + b;
        }
        return result;
    }
}
