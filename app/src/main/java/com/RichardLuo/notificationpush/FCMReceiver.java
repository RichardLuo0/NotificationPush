package com.RichardLuo.notificationpush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.ApplicationInfo;
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
import java.util.List;
import java.util.Map;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

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
				String packageName = data.get("package");
				String AppName = data.get("name");
				int id = Integer.valueOf(data.get("id"));
				String senderName = null;
				if (data.containsKey("senderName"))
						senderName = data.get("senderName");
				PendingIntent intent;
				boolean hide = getDefaultSharedPreferences(this).getBoolean("hide", false);
				if (hide && ForegroundMonitor.packageName.equals(packageName))
						return;

				setChannel(AppName);

				switch (packageName) {
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
								if (getDefaultSharedPreferences(this).getBoolean("sendQQ", false) && this.getDatabasePath("friends.db").exists()) {
										String encodeSendername = senderName.replace(" ", "%20").replace("/", "%2f");
										File file = new File(this.getCacheDir().getPath() + "/" + encodeSendername);
										try {
												if (!file.exists()) {
														SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(this.getDatabasePath("friends.db"), null);
														Cursor cursor;
														if (senderName.equals(title))
																cursor = db.query("friends", new String[]{"uin"}, "name ='" + senderName + "'", null, null, null, null);
														else {
																db.execSQL("create table if not exists '" + title + "'(uin interger primary key,name varchar)");
																cursor = db.query("'" + title + "'", new String[]{"uin"}, "name ='" + senderName + "'", null, null, null, null);
														}
														if (cursor.getCount() != 0)
																if (cursor.moveToFirst()) {
																		String QQnumber = cursor.getString(0);
																		cursor.close();
																		db.close();
																		HttpURLConnection connection = (HttpURLConnection) new URL("https://qlogo3.store.qq.com/qzone/" + QQnumber + "/" + QQnumber + "/100").openConnection();
																		connection.setDoInput(true);
																		connection.setConnectTimeout(1000);
																		connection.setReadTimeout(1000);
																		connection.connect();
																		if (connection.getResponseCode() == 200) {
																				InputStream inputStream = connection.getInputStream();
																				FileOutputStream out = new FileOutputStream(new File(getCacheDir(), encodeSendername));
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
																		}
																		connection.disconnect();
																		icon = IconCompat.createWithBitmap(BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + encodeSendername));
																}
														cursor.close();
														db.close();
												} else
														icon = IconCompat.createWithBitmap(BitmapFactory.decodeFile(this.getCacheDir().getPath() + "/" + encodeSendername));
										} catch (IOException e) {
												e.printStackTrace();
										}
								}
								setSummary(packageName, AppName, intent);
								MessagingStyle(packageName, AppName, title, senderName, body, intent, notificationManagerCompat, id, icon);
								return;
						default:
								intent = getIntent(packageName);
								setSummary(packageName, AppName, intent);
				}

				Notification notification = new NotificationCompat.Builder(this, AppName)
						.setSmallIcon(R.drawable.ic_notification)
						.setColor(color)
						.setStyle(new NotificationCompat.BigTextStyle()
								.setSummaryText(AppName))
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

		public void setChannel(String AppName) {
				NotificationChannel mChannel;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getSharedPreferences("Channels", MODE_PRIVATE).contains(AppName)) {
						mChannel = new NotificationChannel(AppName, AppName, IMPORTANCE_DEFAULT);
						getSystemService(NotificationManager.class).createNotificationChannel(mChannel);
						getSharedPreferences("Channels", MODE_PRIVATE).edit().putBoolean(AppName, true).apply();
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

		private void MessagingStyle(String packageName, String AppName, String title, String senderName, String message, PendingIntent intent, NotificationManagerCompat notificationManagerCompat, int ID, IconCompat icon) {
				Person.Builder personBuilder = new Person.Builder()
						.setName(senderName);
				if (icon != null)
						personBuilder.setIcon(icon);
				Person sender = personBuilder.build();

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

				Notification notification = new NotificationCompat.Builder(this, AppName)
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
