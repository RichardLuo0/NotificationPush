package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.content.Intent;
import android.os.StrictMode;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetNotification extends NotificationListenerService {
    public String inputID;

    @Override
    public void onCreate() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        inputID = MainActivity.inputID.trim();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("com.RichardLuo.notificationpush") || inputID == null) {
            return;
        }
        Notification oneNotification = sbn.getNotification();
        HttpURLConnection connection;
        try {
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setRequestProperty("Authorization","key=AIzaSyDJ_--RTncjiM-aqJzTRx8cqgla77yv7Ag");
            connection.setRequestProperty("Sender","id=657560725798");
            connection.connect();
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            JSONObject obj = new JSONObject();
            JSONObject content = new JSONObject();
            content.put("title",oneNotification.extras.getString(Notification.EXTRA_TITLE, "无标题") + " ・ " + sbn.getPackageName());
            content.put("body",oneNotification.extras.getString(Notification.EXTRA_TEXT, "无内容"));
            obj.put("to",inputID);
            obj.put("notification",content);
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
}
