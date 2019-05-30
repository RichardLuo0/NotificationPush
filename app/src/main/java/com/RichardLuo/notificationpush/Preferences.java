package com.RichardLuo.notificationpush;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class Preferences extends PreferenceFragmentCompat {
		private SharedPreferences preferences;
		private SwitchPreference start;
		private EditTextPreference input;
		private SwitchPreference hide;
		private String token;

		@Override
		public void onCreatePreferences(Bundle bundle, String s) {
				setPreferencesFromResource(R.xml.preference, null);
				preferences = getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));
				FirebaseInstanceId.getInstance().getInstanceId()
						.addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
								@Override
								public void onComplete(@NonNull Task<InstanceIdResult> task) {
										if (!task.isSuccessful()) {
												token = "fail";
												Toast.makeText(getActivity(), "获取token失败", Toast.LENGTH_SHORT).show();
												return;
										}
										token = Objects.requireNonNull(task.getResult()).getToken();
								}
						});

				start = (SwitchPreference) findPreference("start");
				start.setOnPreferenceClickListener(new SwitchPreference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
								Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								if (!Objects.requireNonNull(preferences.getString("input", "")).isEmpty()) {
										startActivity(intent);
								} else {
										Toast.makeText(getActivity(), "请填写设备ID", Toast.LENGTH_SHORT).show();
										start.setChecked(false);
								}
								return false;
						}
				});

				input = (EditTextPreference) findPreference("input");

				hide = (SwitchPreference) findPreference("hide");
				hide.setOnPreferenceClickListener(new SwitchPreference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
								Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
								return false;
						}
				});

				Preference tokenPreference = findPreference("token");
				tokenPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
								final AlertDialog.Builder normalDialog = new AlertDialog.Builder(Objects.requireNonNull(getContext()));
								normalDialog.setTitle("Token");
								normalDialog.setMessage(token);
								normalDialog.setPositiveButton("复制", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
												((ClipboardManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("token", token));
												Toast.makeText(getActivity(), "复制成功", Toast.LENGTH_SHORT).show();
										}
								});
								normalDialog.setNeutralButton("取消", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
										}
								});
								normalDialog.show();
								return false;
						}
				});

				Preference clear = findPreference("clear");
				clear.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
										final NotificationManager notificationManager = Objects.requireNonNull(getContext()).getSystemService(NotificationManager.class);
										for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
												notificationManager.deleteNotificationChannel(channel.getId());
										}
								}
								Objects.requireNonNull(getContext()).getSharedPreferences("Channels", MODE_PRIVATE).edit().clear().apply();
								Toast.makeText(getActivity(), "已删除通知渠道", Toast.LENGTH_SHORT).show();
								return true;
						}
				});
		}

		@Override
		public void onStart() {
				super.onStart();
				if (isNotificationListenersEnabled()) {
						input.setEnabled(false);
						start.setChecked(true);
				} else {
						input.setEnabled(true);
						start.setChecked(false);
				}
				if (isAccessibilitySettingsOn(Objects.requireNonNull(getActivity()))) {
						hide.setChecked(true);
				} else {
						hide.setChecked(false);
				}
		}

		private boolean isNotificationListenersEnabled() {
				String pkgName = Objects.requireNonNull(getActivity()).getPackageName();
				final String flat = Settings.Secure.getString(getActivity().getContentResolver(), "enabled_notification_listeners");
				if (!TextUtils.isEmpty(flat)) {
						final String[] names = flat.split(":");
						for (String name : names) {
								final ComponentName cn = ComponentName.unflattenFromString(name);
								if (cn != null) {
										if (TextUtils.equals(pkgName, cn.getPackageName())) {
												return true;
										}
								}
						}
				}
				return false;
		}


		private boolean isAccessibilitySettingsOn(Context mContext) {
				int accessibilityEnabled;
				final String service = Objects.requireNonNull(getActivity()).getPackageName() + "/" + ForegroundMonitor.class.getCanonicalName();
				try {
						accessibilityEnabled = Settings.Secure.getInt(
								mContext.getApplicationContext().getContentResolver(),
								android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
				} catch (Settings.SettingNotFoundException e) {
						return false;
				}
				TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

				if (accessibilityEnabled == 1) {
						String settingValue = Settings.Secure.getString(
								mContext.getApplicationContext().getContentResolver(),
								Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
						if (settingValue != null) {
								mStringColonSplitter.setString(settingValue);
								while (mStringColonSplitter.hasNext()) {
										String accessibilityService = mStringColonSplitter.next();
										if (accessibilityService.equalsIgnoreCase(service)) {
												return true;
										}
								}
						}
				}
				return false;
		}
}
