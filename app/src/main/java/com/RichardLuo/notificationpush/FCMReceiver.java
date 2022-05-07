package com.RichardLuo.notificationpush;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.PatternMatcher;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FCMReceiver extends FirebaseMessagingService {
    static Map<String, PendingIntent> package_Intent = new HashMap<>();

    int color = 0;
    Boolean ringForEach;
    NotificationManagerCompat notificationManagerCompat;

    private final BroadcastReceiver br = new QQInstallReceiver();

    @Override
    public void onCreate() {
        notificationManagerCompat = NotificationManagerCompat.from(this);
        super.onCreate();

        QQInstallReceiver.findQQPackage(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        filter.addDataSchemeSpecificPart("com.tencent", PatternMatcher.PATTERN_LITERAL);
        registerReceiver(br, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(br);
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
        color = ThemeProvider.getCurrentColor(this);
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

        boolean isQQ = false;
        for (String qqName : Const.QQ_NAMES) {
            if (qqName.equals(packageName)) {
                isQQ = true;
                break;
            }
        }

        if (isQQ && senderName != null) {
            String className = ForegroundMonitor.packageName;
            String qqPackageName = getSharedPreferences("MainActivity", MODE_PRIVATE).getString("installedQQ", null);
            intent = getIntent(qqPackageName);
            if (hide && qqPackageName != null && className.contains(qqPackageName))
                return;
            if (senderName.equals(""))
                senderName = "  ";
            Bitmap icon = null;
            Bitmap largeIcon = null;
            if (getDefaultSharedPreferences(this).getBoolean("sendQQ", false) && this.getDatabasePath("friends.db").exists()) {
                boolean isfriend = senderName.equals(title);
                String encodeSendername = "member_" + Utils.md5(senderName);
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
                    icon = BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + encodeSendername);
                } catch (IOException e) {
                    Log.e("", e.getMessage(), e);
                }
                // Large icon
                if (isfriend) {
                    if (icon != null)
                        largeIcon = icon;
                } else if (title != null) {
                    String encodeTitle = "group_" + Utils.md5(title);
                    if (new File(this.getCacheDir().getPath() + "/" + encodeTitle).exists())
                        largeIcon = BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + encodeTitle);
                    else {
                        String groupNumber = getSharedPreferences("groupsNumber", MODE_PRIVATE).getString(title, null);
                        if (groupNumber != null) {
                            try {
                                largeIcon = downloadIcon("https://p.qlogo.cn/gh/" + groupNumber + "/" + groupNumber + "/100", encodeTitle);
                            } catch (IOException e) {
                                Log.e(Const.TAG, e.getMessage(), e);
                            }
                        }
                    }
                }
            }
            setSummary(packageName, AppName, intent);
            MessagingStyle(packageName, AppName, title, senderName, body, intent, id, icon == null ? null : IconCompat.createWithBitmap(icon), largeIcon);
            return;
        } else {
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
        if (package_Intent.containsKey(packageName)) {
            intent = package_Intent.get(packageName);
            return intent;
        }
        if (packageName != null)
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
                package_Intent.put(packageName, null);
                return null;
            }
        package_Intent.put(packageName, intent);
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
        if (largeIcon != null)
            notification.setLargeIcon(largeIcon);
        notification.setGroup(packageName)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(!ringForEach);
        notificationManagerCompat.notify(packageName, ID, notification.build());
    }
}
