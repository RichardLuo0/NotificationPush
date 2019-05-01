package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.ApplicationInfo;
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

    int color = 0;
    NotificationManagerCompat notificationManagerCompat;

    @Override
    public void onCreate() {
        notificationManagerCompat = NotificationManagerCompat.from(this);
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        color = getResources().getColor(getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("color", R.color.teal));
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");
        int id = Integer.valueOf(data.get("id"));
        String senderName = null;
        if (data.containsKey("senderName"))
            senderName = data.get("senderName");
        String packageName = remoteMessage.getData().get("package");
        PendingIntent intent;
        if (getSharedPreferences("MainActivity", MODE_PRIVATE).getBoolean("hide", false) && ForegroundMonitor.packageName.equals(packageName))
            return;

        setChannel(packageName);

        switch (packageName) {
            case "com.tencent.minihd.qq":
            case "com.tencent.mobileqqi":
            case "com.tencent.qqlite":
            case "com.tencent.tim":
            case "com.tencent.mobileqq":
            case "com.jinhaihan.qqnotfandshare":
                String className = ForegroundMonitor.packageName;
                if (getSharedPreferences("MainActivity", MODE_PRIVATE).getBoolean("hide", false) && (className.contains("com.tencent.") && (className.contains("qq") || className.contains("tim"))))
                    return;
                String QQpackageName = getSharedPreferences("MainActivity", MODE_PRIVATE).getString("installedQQ", null);
                intent = getIntent(QQpackageName);
                if (senderName == null)
                    break;
                if (senderName.equals(""))
                    senderName = "  ";
                setSummary(packageName, intent);
                MessagingStyle(packageName, title, senderName, body, intent, notificationManagerCompat, id);
                return;
            default:
                intent = getIntent(packageName);
                setSummary(packageName, intent);
        }

        Notification notification = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(packageName)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        notificationManagerCompat.notify(packageName, id, notification);
    }

    private boolean isAppInstalled(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        List<ApplicationInfo> applicationInfo = getPackageManager().getInstalledApplications(0);
        if (applicationInfo == null || applicationInfo.isEmpty())
            return false;
        for (ApplicationInfo info : applicationInfo) {
            if (packageName.equals(info.packageName) && info.enabled) {
                return true;
            }
        }
        return false;
    }

    private boolean isChannelExist(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<NotificationChannel> channels = getSystemService(NotificationManager.class).getNotificationChannels();
            for (NotificationChannel channel : channels) {
                if (channel.getId().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void setChannel(String packageName) {
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isChannelExist(packageName)) {
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
        if (packageName != null && !packageName.contains("android") && packageName.split("\\.", 2)[0].equals("com") && isAppInstalled(packageName))
            try {
                intent = PendingIntent.getActivity(this, 200, getPackageManager().getLaunchIntentForPackage(packageName), FLAG_UPDATE_CURRENT);
            } catch (Exception e) {
                Package_Intent.put(packageName, null);
                return null;
            }
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

    public void setSummary(String packageName, PendingIntent intent) {
        Notification summary = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(packageName))
                .setGroup(packageName)
                .setContentIntent(intent)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        notificationManagerCompat.notify(packageName, 0, summary);
    }

    private void MessagingStyle(String packageName, String title, String senderName, String message, PendingIntent intent, NotificationManagerCompat notificationManagerCompat, int ID) {
        Person sender = new Person.Builder()
                .setName(senderName)
                .build();

        Notification current;
        NotificationCompat.MessagingStyle style;
        if (!((current = getCurrentNotification(packageName, ID)) == null)) {
            style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(current);
            style.addMessage(message, new Date().getTime(), sender);
        } else {
            style = new NotificationCompat.MessagingStyle(sender)
                    .setConversationTitle(title)
                    .setGroupConversation(true)
                    .addMessage(message, new Date().getTime(), sender);
        }

        Notification notification = new NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setContentTitle(packageName)
                .setStyle(style)
                .setGroup(packageName)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        notificationManagerCompat.notify(packageName, ID, notification);
    }

    /*private void forQQ(String packageName, String title, String body, PendingIntent intent, NotificationManagerCompat notificationManagerCompat) {
        setChannel(packageName);
        Notification notification;
        if (!(body.contains("联系人给你") || title.contains("QQ空间") || body.contains("你收到了"))) {
            int TitleID = StringToA(title.split("\\s\\(")[0]);
            String[] bodySplit = body.split(":");
            Person sender;
            String message;
            if (bodySplit.length == 1 || body.split("\\s")[0].equals("")) {
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
    }*/
}
