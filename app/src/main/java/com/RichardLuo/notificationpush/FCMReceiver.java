package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.Person;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class FCMReceiver extends FirebaseMessagingService {
    static Map<String, PendingIntent> Package_Intent = new HashMap<>();
    static String installedQQ = null;
    final static String[] QQNames = new String[]{"com.tencent.mobileqq", "com.tencent.tim", "com.tencent.mobileqqi", "com.tencent.qqlite", "com.tencent.minihd.qq"};
    int color = 0;
    NotificationManagerCompat notificationManagerCompat;

    @Override
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            color = getColor(R.color.colorPrimary);
        else
            color = getResources().getColor(R.color.colorPrimary);
        notificationManagerCompat = NotificationManagerCompat.from(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
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
                if (installedQQ == null) {
                    if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getSystemService(NotificationManager.class).getNotificationChannels().contains(packageName))) {
                        List<PackageInfo> info = this.getPackageManager().getInstalledPackages(0);
                        for (int i = 0; i < info.size(); i++) {
                            String ipackage = info.get(i).packageName;
                            for (String QQName : QQNames) {
                                if (QQName.equals(ipackage)) {
                                    installedQQ = QQName;
                                    break;
                                }
                            }
                            if (installedQQ != null)
                                break;
                        }
                    } else
                        installedQQ = packageName;
                }
                packageName = installedQQ;
                forQQ(packageName, title, body, getIntent(packageName), notificationManagerCompat);
                return;
        }

        setChannel(packageName);
        PendingIntent intent = getIntent(packageName);
        Notification summary = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(packageName))
                .setGroup(packageName)
                .setGroupSummary(true)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build();
        notificationManagerCompat.notify(packageName, 0, summary);
        Notification notification = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(packageName)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build();
        notificationManagerCompat.notify(packageName, id, notification);
    }

    private boolean isAppInstalled(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        List<PackageInfo> info = getPackageManager().getInstalledPackages(0);
        if (info == null || info.isEmpty())
            return false;
        for (int i = 0; i < info.size(); i++) {
            if (packageName.equals(info.get(i).packageName)) {
                return true;
            }
        }
        return false;
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

    private void setChannel(String packageName) {
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getSystemService(NotificationManager.class).getNotificationChannels().contains(packageName)) {
            mChannel = new NotificationChannel(packageName, packageName, IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(mChannel);
        }
    }

    private PendingIntent getIntent(String packageName) {
        PendingIntent intent = null;
        if (Package_Intent.containsKey(packageName)) {
            intent = Package_Intent.get(packageName);
            return intent;
        }
        if (packageName != null && !packageName.equals("com.android.systemui") && packageName.split("\\.")[0].equals("com") && isAppInstalled(packageName))
            intent = PendingIntent.getActivity(this, 200, getPackageManager().getLaunchIntentForPackage(packageName), FLAG_UPDATE_CURRENT);
        Package_Intent.put(packageName, intent);
        return intent;
    }

    private Notification getCurrentNotification(String packageName, int id) {
        StatusBarNotification[] sbns;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            sbns = getSystemService(NotificationManager.class).getActiveNotifications();
        else
            return null;
        for (StatusBarNotification sbn : sbns) {
            if (sbn.getTag().equals(packageName) && sbn.getId() == id) {
                return sbn.getNotification();
            }
        }
        return null;
    }

    private void forQQ(String packageName, String title, String body, PendingIntent
            intent, NotificationManagerCompat notificationManagerCompat) {
        setChannel(packageName);
        Notification notification;
        if (!(body.contains("联系人给你") || title.contains("QQ空间") || body.contains("你收到了"))) {
            int TitleID = StringToA(title.split("\\s\\(")[0]);
            String[] bodySplit = body.split(":");
            Person sender;
            String message;
            if (bodySplit.length == 1) {
                sender = new Person.Builder()
                        .setName(title.split("\\s\\(")[0])
                        .build();
                message = body;
            } else {
                sender = new Person.Builder()
                        .setName(bodySplit[0])
                        .build();
                message = bodySplit[1];
            }
            NotificationCompat.MessagingStyle style;
            Notification current;
            if (!((current = getCurrentNotification(packageName, TitleID)) == null)) {
                style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(current);
                style.addMessage(message, new Date().getTime(), sender);
            } else {
                style = new NotificationCompat.MessagingStyle(sender)
                        .setConversationTitle(title)
                        .addMessage(message, new Date().getTime(), sender);
            }
            notification = new NotificationCompat.Builder(this, packageName)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(color)
                    .setContentTitle(packageName)
                    .setStyle(style)
                    .setGroup(packageName)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build();
            notificationManagerCompat.notify(packageName, TitleID, notification);
        } else {
            notification = new NotificationCompat.Builder(this, packageName)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(color)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setGroup(packageName)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build();
            notificationManagerCompat.notify(packageName, 0, notification);
        }
    }
}
