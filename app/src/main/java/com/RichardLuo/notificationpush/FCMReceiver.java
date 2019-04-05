package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class FCMReceiver extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String packageName = remoteMessage.getData().get("package");
        /*此处定义服务端的app与客户端如何对应（例如qq一类无法同时登陆同一账号）
        switch (packageName) {
            case "com.tencent.minihd.qq":
                packageName = "com.tencent.mobileqq";
                break;
        }*/
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            mChannel = new NotificationChannel(packageName, packageName, IMPORTANCE_DEFAULT);
            if (!notificationManager.getNotificationChannels().contains(mChannel)) {
                notificationManager.createNotificationChannel(mChannel);
            }
        }
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        int id = Integer.valueOf(remoteMessage.getData().get("id"));

        PendingIntent intent = null;
        if (isAppInstalled(packageName))
            intent = PendingIntent.getActivity(this, 200, getPackageManager().getLaunchIntentForPackage(packageName), FLAG_UPDATE_CURRENT);
        Notification summary = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getColor(R.color.colorPrimary))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(packageName))
                .setGroup(packageName)
                .setGroupSummary(true)
                .build();
        notificationManagerCompat.notify(StringToA(packageName), summary);
        Notification notification = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getColor(R.color.colorPrimary))
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(intent)
                .setGroup(packageName)
                .setAutoCancel(true)
                .build();
        notificationManagerCompat.notify(id, notification);
    }

    public boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int StringToA(String content) {
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
