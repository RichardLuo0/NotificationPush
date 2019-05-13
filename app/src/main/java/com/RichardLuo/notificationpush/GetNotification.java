package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class GetNotification extends NotificationListenerService {
    protected final String Authorization = "";
    protected final String Sender = "";
    public String inputID;
    static boolean QQcount = true;//前一次是否有多个联系人，避免重复通知
    PackageManager pm;

    @Override
    public void onCreate() {
        inputID = getDefaultSharedPreferences(this).getString("input", "");
        pm = getPackageManager();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
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
        if (getDefaultSharedPreferences(this).getBoolean("hide_no_content", false) && title.equals("无标题") && body.equals("无内容"))
            return;

        switch (getSharedPreferences("Application", MODE_PRIVATE).getInt(packageName, 0)) {
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
                    if (body.contains("联系人给你") || body.contains("你收到了")) {
                        QQcount = true;
                        String tickerText = oneNotification.tickerText.toString().replace("\n", "");
                        Matcher matcher = Pattern.compile("^(.*?)\\((((?![()]).)*?)\\):(.*?)$").matcher(tickerText);
                        if (matcher.find()) {
                            senderName = matcher.group(1);
                            title = matcher.group(2);
                            body = matcher.group(4);
                        } else {
                            String[] single = tickerText.split(":", 2);
                            senderName = single[0];
                            title = single[0];
                            body = single[1];
                        }
                    } else {
                        if (QQcount) {
                            QQcount = false;
                            return;
                        }
                        QQcount = false;
                        title = title.split("\\s\\(", 2)[0];
                        String[] bodySplit = body.split(":\\s", 2);
                        if (bodySplit.length == 1 || body.split("\\s", 2)[0].equals(""))
                            senderName = title.split("\\s\\(", 2)[0];
                        else {
                            senderName = bodySplit[0];
                            body = bodySplit[1];
                        }
                    }
                    ID = StringToA(title);
                }
        }

        HttpURLConnection connection;
        try {
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
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
            e.printStackTrace();
        }
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
