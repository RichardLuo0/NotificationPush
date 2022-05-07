package com.RichardLuo.notificationpush;

import android.content.Context;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class ThemeProvider {
    private static class Theme {
        String name;
        int style;
        int color;

        public Theme(String name, int style, int color) {
            this.name = name;
            this.style = style;
            this.color = color;
        }
    }

    final static private ArrayList<Theme> themeList = new ArrayList<>();

    static {
        themeList.add(new Theme("水鸭青", R.style.base_DayNight_AppTheme_teal, R.color.teal));
        themeList.add(new Theme("姨妈红", R.style.base_DayNight_AppTheme_red, R.color.red));
        themeList.add(new Theme("哔哩粉", R.style.base_DayNight_AppTheme_pink, R.color.pink));
        themeList.add(new Theme("基佬紫", R.style.base_DayNight_AppTheme_purple, R.color.purple));
        themeList.add(new Theme("很深蓝", R.style.base_DayNight_AppTheme_blue, R.color.blue));
        themeList.add(new Theme("非常黄", R.style.base_DayNight_AppTheme_yellow, R.color.yellow));
        themeList.add(new Theme("真的灰", R.style.base_DayNight_AppTheme_grey, R.color.grey));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            themeList.add(new Theme("Android12+", R.style.base_DayNight_AppTheme_android12plus, R.color.android12plus));
    }

    static Theme selected = null;

    static String[] getThemeNameList() {
        String[] themeNameList = new String[themeList.size()];
        for (int i = 0; i < themeList.size(); i++) {
            themeNameList[i] = themeList.get(i).name;
        }
        return themeNameList;
    }

    static void setTheme(Context context, int i) {
        context.getSharedPreferences("theme", Context.MODE_PRIVATE).edit().putString("selected", themeList.get(i).name).apply();
        selected = themeList.get(i);
    }

    static Theme getCurrentTheme(Context context) {
        if (selected == null) {
            String themeName = context.getSharedPreferences("theme", Context.MODE_PRIVATE).getString("selected", themeList.get(0).name);
            for (int i = 0; i < themeList.size(); i++) {
                Theme theme = themeList.get(i);
                if (theme.name.equals(themeName)) {
                    selected = theme;
                    break;
                }
            }
        }
        return selected;
    }

    static int getCurrentStyle(Context context) {
        return getCurrentTheme(context).style;
    }

    static int getCurrentColor(Context context) {
        return ContextCompat.getColor(context, getCurrentTheme(context).color);
    }
}
