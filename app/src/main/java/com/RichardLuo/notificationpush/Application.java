package com.RichardLuo.notificationpush;

import android.animation.ValueAnimator;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Application extends AppCompatActivity {
    ListView listView;
    SharedPreferences preferences;
    ProgressBar progressBar;
    boolean filterSystemApp = true;
    String searchText = null;
    BaseAdapter ba;
    Runnable notifyDataSet = new Runnable() {
        @Override
        public void run() {
            ba.notifyDataSetChanged();
        }
    };

    PackageManager packageManager;
    List<ApplicationInfo> packageInfo;
    List<ApplicationInfo> displayItem = new ArrayList<>();
    List<ApplicationInfo> tempItem;
    Info[] displayInfo;

    class ViewHolder {
        TextView text;
        ImageView icon;
        AppCompatAutoCompleteTextView act;
    }

    class Info {
        String text;
        Drawable icon;
        AdapterView.OnItemClickListener onItemClickListener;
        int selection;

        Info(final ApplicationInfo applicationInfo) {
            text = getPackageManager().getApplicationLabel(applicationInfo).toString();
            icon = getPackageManager().getApplicationIcon(applicationInfo);
            selection = preferences.getInt(applicationInfo.packageName, preferences.contains("allOff") ? 2 : 0);

            onItemClickListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0 && !preferences.contains("allOff")) {
                        preferences.edit().remove(applicationInfo.packageName).apply();
                        return;
                    } else if (position == 2 && preferences.contains("allOff"))
                        return;
                    preferences.edit().putInt(applicationInfo.packageName, position).apply();
                }
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("style", R.style.base_DayNight_AppTheme_teal));
        setContentView(R.layout.activity_application);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        listView = findViewById(R.id.listview);
        progressBar = findViewById(R.id.progressBar);
        preferences = getPreferences(MODE_PRIVATE);

        listView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        listView.setFitsSystemWindows(true);
//        final ActionBar actionBar = getSupportActionBar();
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////            listView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
////                boolean lastScrollinTop = true;
////                ValueAnimator animation = ValueAnimator.ofInt(0, 8);
////
////                {
////                    animation.setDuration(400);
////                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
////                        @Override
////                        public void onAnimationUpdate(ValueAnimator animation) {
////                            actionBar.setElevation((int) animation.getAnimatedValue());
////                        }
////                    });
////                }
////
////                @Override
////                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
////                    if (!v.canScrollVertically(-1) && !lastScrollinTop) {
////                        animation.reverse();
////                        lastScrollinTop = true;
////                    } else if (lastScrollinTop) {
////                        animation.start();
////                        lastScrollinTop = false;
////                    }
////                }
////            });
////        } else
////            actionBar.setElevation(8);

        final String[] priorityContent = getApplicationContext().getResources().getStringArray(R.array.priorityContent);
        final ArrayAdapter actAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.dropdown, priorityContent);
        packageManager = getPackageManager();
        packageInfo = packageManager.getInstalledApplications(0);
        new Thread() {
            @Override
            public void run() {
                Collections.sort(packageInfo, new ApplicationInfo.DisplayNameComparator(packageManager));
                super.run();
            }
        }.start();
        displayInfo = new Info[packageInfo.size()];

        ba = new BaseAdapter() {
            private int lastPosition = -1;

            @Override
            public int getCount() {
                return displayItem.size();
            }

            @Override
            public Object getItem(int position) {
                return displayItem.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public void notifyDataSetChanged() {
                displayInfo = new Info[packageInfo.size()];
                lastPosition = -1;
                super.notifyDataSetChanged();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                Info info;
                if (displayInfo[position] == null)
                    displayInfo[position] = (info = new Info(displayItem.get(position)));
                else
                    info = displayInfo[position];
                if (convertView == null) {
                    convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.app_layout, listView, false);
                    holder = new ViewHolder();
                    holder.text = convertView.findViewById(R.id.appName);
                    holder.icon = convertView.findViewById(R.id.imageView);
                    holder.act = convertView.findViewById(R.id.priority);
                    holder.act.setAdapter(actAdapter);
                    holder.act.setKeyListener(null);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                holder.text.setText(info.text);
                holder.icon.setImageDrawable(info.icon);
                holder.act.setOnItemClickListener(info.onItemClickListener);
                holder.act.setText(priorityContent[info.selection], false);
                convertView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top));
                lastPosition = position;
                return convertView;
            }
        };

        refreshListData(new Runnable() {
            @Override
            public void run() {
                listView.setAdapter(ba);
            }
        });
    }

    public void refreshListData(final Runnable update) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread() {
            @Override
            public void run() {
                if (filterSystemApp || (searchText != null && !searchText.equals(""))) {
                    if (displayItem == packageInfo)
                        displayItem = tempItem;
                    displayItem.clear();
                    for (ApplicationInfo applicationInfo : packageInfo) {
                        if (filterSystemApp && !((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0))
                            continue;

                        // 判断search Text
                        if ((searchText != null && !searchText.equals("")) && !packageManager.getApplicationLabel(applicationInfo).toString().contains(searchText))
                            continue;

                        displayItem.add(applicationInfo);
                    }
                } else if (displayItem != packageInfo) {
                    tempItem = displayItem;
                    displayItem = packageInfo;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update.run();
                        progressBar.setVisibility(View.GONE);
                    }
                });
                super.run();
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appmenu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenu = menu.findItem(R.id.search);
        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                if (searchText != null) {
                    searchText = null;
                    refreshListData(notifyDataSet);
                }
                return true;
            }
        });
        SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setSearchableInfo(Objects.requireNonNull(searchManager).getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        if (preferences.contains("allOff"))
            menu.findItem(R.id.reverse).setTitle(getString(R.string.positive));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.filter:
                filterSystemApp = !filterSystemApp;
                refreshListData(notifyDataSet);
                break;
            case R.id.reverse:
                if (!preferences.contains("allOff")) {
                    preferences.edit().clear().putInt("allOff", 0).apply();
                    item.setTitle(getString(R.string.positive));
                } else {
                    preferences.edit().remove("allOff").apply();
                    item.setTitle(getString(R.string.reverse));
                }
                refreshListData(notifyDataSet);
                break;
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchText = intent.getStringExtra(SearchManager.QUERY);
            refreshListData(notifyDataSet);
        }
        super.onNewIntent(intent);
    }
}
