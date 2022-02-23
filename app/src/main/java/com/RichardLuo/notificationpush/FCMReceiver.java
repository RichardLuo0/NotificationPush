package com.RichardLuo.notificationpush;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FCMReceiver extends FirebaseMessagingService {
    static Map<String, PendingIntent> Package_Intent = new HashMap<>();

    int color = 0;
    Boolean ringForEach;
    NotificationManagerCompat notificationManagerCompat;

    @Override
    public void onCreate() {
        notificationManagerCompat = NotificationManagerCompat.from(this);
        super.onCreate();
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        String channelName = "Token变更";
        setChannel(channelName);
        Notification notification = new NotificationCompat.Builder(this, channelName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(channelName))
                .setContentTitle(channelName)
                .setContentText("Token发生变更，请更换服务端token")
                .setAutoCancel(true)
                .setOnlyAlertOnce(!ringForEach)
                .build();
        notificationManagerCompat.notify(channelName, 0, notification);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        ringForEach = getDefaultSharedPreferences(this).getBoolean("ringForEach", false);
        color = ContextCompat.getColor(this, getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("color", R.color.teal));
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");
        String packageName = data.get("package");
        String AppName = data.get("name");
        int id = Integer.parseInt(Objects.requireNonNull(data.get("id")));
        String senderName = null;
        if (data.containsKey("senderName"))
            senderName = data.get("senderName");
        PendingIntent intent;
        boolean hide = getDefaultSharedPreferences(this).getBoolean("hide", false);
        if (hide && ForegroundMonitor.packageName.equals(packageName))
            return;

        setChannel(AppName);

        switch (Objects.requireNonNull(packageName)) {
            case "com.tencent.minihd.qq":
            case "com.tencent.mobileqqi":
            case "com.tencent.qqlite":
            case "com.tencent.tim":
            case "com.tencent.mobileqq":
            case "com.jinhaihan.qqnotfandshare":
                String className = ForegroundMonitor.packageName;
                if (hide && (className.contains("com.tencent.") && (className.contains("qq") || className.contains("tim"))))
                    return;
                String QQpackageName = getSharedPreferences("MainActivity", MODE_PRIVATE).getString("installedQQ", null);
                intent = getIntent(QQpackageName);
                if (senderName == null)
                    break;
                if (senderName.equals(""))
                    senderName = "  ";
                IconCompat icon = null;
                Bitmap largeIcon = null;
                if (getDefaultSharedPreferences(this).getBoolean("sendQQ", false) && this.getDatabasePath("friends.db").exists()) {
                    boolean isfriend = senderName.equals(title);
                    String encodeSendername = md5(senderName);
                    File file = new File(this.getCacheDir().getPath() + "/" + encodeSendername);
                    out:
                    try {
                        if (!file.exists()) {
                            SQLiteDatabase db;
                            Cursor cursor;
                            if (isfriend) {
                                db = SQLiteDatabase.openOrCreateDatabase(this.getDatabasePath("friends.db"), null);
                                if (getSharedPreferences("groups", MODE_PRIVATE).contains("sync_friends"))
                                    cursor = db.query("friends", new String[]{"uin"}, "name ='" + senderName + "'", null, null, null, null);
                                else
                                    break out;
                            } else if (getSharedPreferences("groups", MODE_PRIVATE).contains(title)) {
                                db = SQLiteDatabase.openOrCreateDatabase(this.getDatabasePath("friends.db"), null);
                                Cursor cursorTemp = db.query("'" + title + "'", new String[]{"uin"}, "name ='" + senderName + "'", null, null, null, null);
                                if (cursorTemp.getCount() == 0) {
                                    cursorTemp.close();
                                    cursor = db.query("friends", new String[]{"uin"}, "name ='" + senderName + "'", null, null, null, null);
                                } else cursor = cursorTemp;
                            } else
                                break out;
                            if (cursor.getCount() != 0) {
                                if (cursor.moveToFirst()) {
                                    String QQnumber = cursor.getString(0);
                                    cursor.close();
                                    db.close();
                                    downloadIcon("https://q4.qlogo.cn/g?b=qq&s=140&nk=" + QQnumber, encodeSendername);
                                }
                            } else {
                                cursor.close();
                                db.close();
                                break out;
                            }
                        }
                        icon = IconCompat.createWithBitmap(BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + encodeSendername));
                    } catch (IOException e) {
                        Log.e("", e.getMessage(), e);
                    }
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P)
                        if (isfriend) {
                            if (icon != null)
                                largeIcon = BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + encodeSendername);
                        } else if (new File(this.getCacheDir().getPath() + "/" + title).exists())
                            largeIcon = BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + title);
                        else {
                            String groupNumber = getSharedPreferences("groupsNumber", MODE_PRIVATE).getString(title, null);
                            if (groupNumber != null) {
                                try {
                                    largeIcon = downloadIcon("https://p.qlogo.cn/gh/" + groupNumber + "/" + groupNumber + "/100", title);
                                } catch (IOException e) {
                                    Log.e(Const.TAG, e.getMessage(), e);
                                }
                            }
                        }
                }
                setSummary(packageName, AppName, intent);
                MessagingStyle(packageName, AppName, title, senderName, body, intent, id, icon, largeIcon);
                return;
            default:
                intent = getIntent(packageName);
                setSummary(packageName, AppName, intent);
        }

        Notification notification = new NotificationCompat.Builder(this, AppName == null ? "" : AppName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(AppName))
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(packageName)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(!ringForEach)
                .build();
        notificationManagerCompat.notify(packageName, id, notification);
    }

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & aMessageDigest));
                while (h.length() < 2)
                    h.insert(0, "0");
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(Const.TAG, e.getMessage(), e);
        }
        return "unknown";
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Bitmap downloadIcon(String url, String name) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        connection.connect();
        if (connection.getResponseCode() == 200 || connection.getResponseCode() == 304) {
            InputStream inputStream = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(new File(getCacheDir(), name));
            Bitmap avatar = BitmapFactory.decodeStream(inputStream);
            int width = avatar.getWidth();
            Bitmap output = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Rect rect = new Rect(0, 0, width, width);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(0xff424242);
            canvas.drawCircle(width / 2f, width / 2f, width / 2f, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(avatar, rect, rect, paint);
            output.compress(Bitmap.CompressFormat.PNG, 100, out);
            return output;
        }
        connection.disconnect();
        return null;
    }

    public void setChannel(String AppName) {
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getSharedPreferences("Channels", MODE_PRIVATE).contains(AppName)) {
            mChannel = new NotificationChannel(AppName, AppName, IMPORTANCE_DEFAULT);
            Objects.requireNonNull(getSystemService(NotificationManager.class)).createNotificationChannel(mChannel);
            getSharedPreferences("Channels", MODE_PRIVATE).edit().putBoolean(AppName, true).apply();
        }
    }

    private PendingIntent getIntent(String packageName) {
        PendingIntent intent = null;
        if (Package_Intent.containsKey(packageName)) {
            intent = Package_Intent.get(packageName);
            return intent;
        }
        if (packageName != null && !packageName.contains("android"))
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent == null)
                    return null;
                int flags;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    flags = PendingIntent.FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT;
                else
                    flags = FLAG_UPDATE_CURRENT;
                intent = PendingIntent.getActivity(this, 200, launchIntent, flags);
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
            sbns = Objects.requireNonNull(getSystemService(NotificationManager.class)).getActiveNotifications();
        else
            return null;
        for (StatusBarNotification sbn : sbns) {
            if (sbn.getTag().equals(packageName) && sbn.getId() == id) {
                return sbn.getNotification();
            }
        }
        return null;
    }

    public void setSummary(String packageName, String AppName, PendingIntent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Notification summary = new NotificationCompat.Builder(this, AppName)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(color)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setSummaryText(AppName))
                    .setGroup(packageName)
                    .setContentIntent(intent)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build();
            notificationManagerCompat.notify(packageName, 0, summary);
        }
    }

    private void MessagingStyle(String packageName, String AppName, String title, String senderName, String message, PendingIntent intent, int ID, IconCompat icon, Bitmap largeIcon) {
        Person.Builder personBuilder = new Person.Builder()
                .setName(senderName);
        if (icon != null)
            personBuilder.setIcon(icon);
        Person sender = personBuilder.build();

        Notification current;
        NotificationCompat.MessagingStyle style = null;
        if (!((current = getCurrentNotification(packageName, ID)) == null))
            style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(current);
        if (style == null) {
            style = new NotificationCompat.MessagingStyle(sender);
            if (title.equals(senderName))
                style.setGroupConversation(false);
            else
                style.setConversationTitle(title).setGroupConversation(true);
        }
        style.addMessage(message, new Date().getTime(), sender);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, AppName)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setContentTitle(packageName)
                .setStyle(style);
        if (largeIcon != null && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            notification.setLargeIcon(largeIcon);
        notification.setGroup(packageName)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(!ringForEach);
        notificationManagerCompat.notify(packageName, ID, notification.build());
    }
}
