package com.RichardLuo.notificationpush;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Application extends AppCompatActivity {
    ListView listView;
    SharedPreferences preferences;
    ProgressBar progressBar;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("style", R.style.base_AppTheme_teal));
        setContentView(R.layout.activity_application);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        preferences = getPreferences(MODE_PRIVATE);
        listView = findViewById(R.id.listview);
        progressBar = findViewById(R.id.progressBar);
        final PackageManager packageManager = getPackageManager();
        final List<ApplicationInfo> packageInfo = packageManager.getInstalledApplications(0);

        new Thread() {
            @Override
            public void run() {
                super.run();
                final List<info> packageView = new ArrayList<>();
                for (final ApplicationInfo applicationInfo : packageInfo) {
                    final String name = packageManager.getApplicationLabel(applicationInfo).toString();
                    Spinner.OnItemSelectedListener onItemSelectedListener = new Spinner.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position == 0) {
                                preferences.edit().remove(applicationInfo.packageName).apply();
                                return;
                            }
                            preferences.edit().putInt(applicationInfo.packageName, position).apply();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    };
                    packageView.add(new info(name, packageManager.getApplicationIcon(applicationInfo), onItemSelectedListener, preferences.getInt(applicationInfo.packageName, 0)));
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return packageView.size();
                            }

                            @Override
                            public Object getItem(int position) {
                                return packageView.get(position);
                            }

                            @Override
                            public long getItemId(int position) {
                                return position;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                ViewHolder holder;
                                info info = packageView.get(position);
                                if (convertView == null) {
                                    convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.app_layout, listView, false);
                                    holder = new ViewHolder();
                                    holder.text = convertView.findViewById(R.id.appName);
                                    holder.icon = convertView.findViewById(R.id.imageView);
                                    holder.spinner = convertView.findViewById(R.id.spinner);
                                    convertView.setTag(holder);
                                } else {
                                    holder = (ViewHolder) convertView.getTag();
                                }
                                holder.text.setText(info.text);
                                holder.icon.setImageDrawable(info.icon);
                                holder.spinner.setSelection(info.selection);
                                holder.spinner.setOnItemSelectedListener(info.onItemSelectedListener);
                                return convertView;
                            }
                        });
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            class ViewHolder {
                TextView text;
                ImageView icon;
                Spinner spinner;
            }

            class info {
                String text;
                Drawable icon;
                Spinner.OnItemSelectedListener onItemSelectedListener;
                int selection;

                info(String text, Drawable icon, Spinner.OnItemSelectedListener onItemSelectedListener, int selection) {
                    this.text = text;
                    this.icon = icon;
                    this.onItemSelectedListener = onItemSelectedListener;
                    this.selection = selection;
                }
            }
        }.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
