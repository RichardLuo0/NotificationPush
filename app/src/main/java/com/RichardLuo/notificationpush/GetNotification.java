package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.content.Intent;
import android.os.StrictMode;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetNotification extends NotificationListenerService {
    protected final String Authorization = "";
    protected final String Sender = "";
    public String inputID;

    @Override
    public void onCreate() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        inputID = MainActivity.inputID.trim();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Notification oneNotification = sbn.getNotification();
        String title = oneNotification.extras.getString(Notification.EXTRA_TITLE, "无标题");
        String body = oneNotification.extras.getString(Notification.EXTRA_TEXT, "无内容");
        int ID = sbn.getId();
        String senderName = null;

        if (title.contains("正在运行") || title.contains("running")) return;
        if (inputID == null) return;

        //此处对单个应用进行单独定义
        switch (packageName) {
            case "com.RichardLuo.notificationpush":
                return;
            case "com.tencent.minihd.qq":
                if (getSharedPreferences("MainActivity", MODE_PRIVATE).getString("isNotfInstalled", "").equals("true"))
                    return;
            case "com.tencent.mobileqqi":
            case "com.tencent.qqlite":
            case "com.tencent.tim":
            case "com.tencent.mobileqq":
            case "com.jinhaihan.qqnotfandshare":
                if (!(body.contains("联系人给你") || title.contains("QQ空间") || body.contains("你收到了"))) {
                    ID = StringToA(title.split("\\s\\(")[0]);
                    String[] bodySplit = body.split(":");
                    if (bodySplit.length == 1 || body.split("\\s")[0].equals(""))
                        senderName = title.split("\\s\\(")[0];
                    else {
                        senderName = bodySplit[0];
                        body = bodySplit[1];
                    }
                }
        }

        HttpURLConnection connection;
        try {
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setConnectTimeout(1000);
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
            content.put("id", ID);
            if (senderName != null)
                content.put("senderName", senderName);
            obj.put("to", inputID);
            obj.put("data", content);
            String json = obj.toString();
            out.write(json.getBytes());
            out.flush();
            out.close();
            connection.getResponseCode();
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
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
