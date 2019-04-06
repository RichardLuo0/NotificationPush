package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.Person;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class FCMReceiver extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        int id = Integer.valueOf(remoteMessage.getData().get("id"));
        String packageName = remoteMessage.getData().get("package");
        //此处对单个应用进行单独定义
        switch (packageName) {
            case "com.tencent.minihd.qq":
            case "com.tencent.mobileqqi":
            case "com.tencent.qqlite":
            case "com.tencent.tim":
            case "com.tencent.mobileqq":
                packageName = "com.tencent.mobileqq";
                forQQ(packageName, title, body, getIntent(packageName), id, notificationManagerCompat);
                return;
        }
        CheckChannel(packageName);
        PendingIntent intent = getIntent(packageName);
        Notification summary = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getColor(R.color.colorPrimary))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(packageName))
                .setGroup(packageName)
                .setGroupSummary(true)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build();
        notificationManagerCompat.notify(StringToA(packageName), summary);
        Notification notification = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getColor(R.color.colorPrimary))
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(packageName)
                .setContentIntent(intent)
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

    public void CheckChannel(String packageName) {
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            mChannel = new NotificationChannel(packageName, packageName, IMPORTANCE_DEFAULT);
            if (!notificationManager.getNotificationChannels().contains(mChannel)) {
                notificationManager.createNotificationChannel(mChannel);
            }
        }
    }

    public PendingIntent getIntent(String packageName) {
        PendingIntent intent = null;
        if (isAppInstalled(packageName) && packageName != null && !packageName.equals("com.android.systemui"))
            intent = PendingIntent.getActivity(this, 200, getPackageManager().getLaunchIntentForPackage(packageName), FLAG_UPDATE_CURRENT);
        return intent;
    }

    public Notification getCurrentNotification(int id) {
        StatusBarNotification[] sbns = getSystemService(NotificationManager.class).getActiveNotifications();
        for (StatusBarNotification sbn : sbns) {
            if (sbn.getId() == id) {
                return sbn.getNotification();
            }
        }
        return null;
    }

    public void forQQ(String packageName, String title, String body, PendingIntent intent, int id, NotificationManagerCompat notificationManagerCompat) {
        CheckChannel(packageName);
        Person sender = new Person.Builder()
                .setName(body.split(":")[0])
                .build();
        Notification notification;
        if (!(body.contains("联系人给你") || body.contains("你收到了"))) {
            NotificationCompat.MessagingStyle style;
            Notification current = getCurrentNotification(id);
            if (!(current == null)) {
                style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(current);
                style.addMessage(body.split(":")[1], new Date().getTime(), sender);
            } else {
                style = new NotificationCompat.MessagingStyle(sender)
                        .setConversationTitle(title)
                        .addMessage(body.split(":")[1], new Date().getTime(), sender);
            }
            notification = new NotificationCompat.Builder(this, packageName)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(getColor(R.color.colorPrimary))
                    .setContentTitle(packageName)
                    .setStyle(style)
                    .setGroup(packageName)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(this, packageName)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(getColor(R.color.colorPrimary))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setGroup(packageName)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .build();
            notificationManagerCompat.notify(StringToA(body), notification);
        }
        notificationManagerCompat.notify(id, notification);
    }
}
