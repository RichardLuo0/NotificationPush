package com.RichardLuo.notificationpush;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQLogin extends AppCompatActivity {
    WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("style", R.style.base_DayNight_AppTheme_teal));
        setContentView(R.layout.qq_login);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        web = findViewById(R.id.web);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        web.getSettings().setJavaScriptEnabled(true);
        if (CookieManager.getInstance().getCookie("https://qun.qq.com") != null)
            web.loadUrl("https://qun.qq.com/member.html");
        else
            web.loadUrl("https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_disable_pwd=0&appid=715030901&daid=73&hide_close_icon=1&pt_no_auth=1&s_url=https%3A%2F%2Fqun.qq.com%2Fmember.html%23gid%3D8292362");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_cookies:
                CookieManager.getInstance().removeAllCookies(null);
                Toast.makeText(this, "清除完成", Toast.LENGTH_SHORT).show();
                break;
            case R.id.refresh:
                web.reload();
                break;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        String cookies = CookieManager.getInstance().getCookie("https://qun.qq.com") + ";";
        Matcher matcher_pskey = Pattern.compile("p_skey=(.*?);").matcher(cookies);
        Matcher matcher_skey = Pattern.compile("skey=(.*?);").matcher(cookies);
        Matcher matcher_uin = Pattern.compile("p_uin=(.*?);").matcher(cookies);
        Matcher matcher_token = Pattern.compile("pt4_token=(.*?);").matcher(cookies);
        if (matcher_pskey.find() && matcher_skey.find() && matcher_uin.find() && matcher_token.find()) {
            intent.putExtra("pskey", matcher_pskey.group(1));
            intent.putExtra("skey", matcher_skey.group(1));
            intent.putExtra("uin", matcher_uin.group(1));
            intent.putExtra("token", matcher_token.group(1));
            setResult(RESULT_OK, intent);
        } else
            setResult(0);
        super.finish();
    }
}
