package com.ktfield.routermanager;

import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.view.ViewGroup;
import android.os.Bundle;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import java.net.Inet4Address;

public class MainActivity extends Activity {
    LinearLayout root;
    TextView output;
    EditText ipInput;
    WebView browser;
    ScrollView homeScreen;
    final int green = Color.rgb(21, 105, 78);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        homeScreen = scroll;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,32,28,32);
        root.setBackgroundColor(Color.rgb(244,249,246));
        scroll.addView(root);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("KT 공유기 매니저");
        title.setTextSize(23);
        title.setTextColor(green);
        title.setTypeface(null,Typeface.BOLD);
        root.addView(title);
        label("GiGA WiFi home ax · KM15-103H\n관리자 로그인 및 Wi-Fi 설정 도우미");
        addButton("1. KT AX 관리자 로그인", () -> openRouter("172.30.1.254",8899));
        addButton("2. 현재 공유기 자동 검색", () -> discover());
        ipInput = new EditText(this);
        ipInput.setSingleLine(true);
        ipInput.setHint("공유기 IP (예: 172.30.1.254)");
        ipInput.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(ipInput);
        addButton("3. 입력한 IP로 관리자 접속", () -> openRouter(ipInput.getText().toString().trim(),8899));
        label("Wi-Fi 설정 항목");
        addButton("2.4GHz 설정 안내", () -> showBand("2.4GHz"));
        addButton("5GHz 설정 안내", () -> showBand("5GHz"));
        addButton("2.4GHz + 5GHz 설정 안내", () -> showBand("2.4GHz와 5GHz"));
        addButton("4. 네트워크 진단", () -> diagnose());
        addButton("5. 휴대폰 Wi-Fi 설정", () -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        output = new TextView(this);
        output.setText("KT AX 공유기에 Wi-Fi로 연결한 후 관리자 로그인하세요.\n관리자 페이지에서 보안문자를 직접 입력해야 합니다.");
        output.setTextSize(16);
        output.setTextColor(Color.DKGRAY);
        output.setPadding(8,32,8,12);
        output.setTextIsSelectable(true);
        root.addView(output);
    }
    void label(String value) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(16); t.setTextColor(Color.DKGRAY);
        t.setPadding(0,16,0,20); root.addView(t);
    }
    void addButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(16); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,(int)(60*getResources().getDisplayMetrics().density));
        p.bottomMargin=10;
        root.addView(b,p);
        b.setOnClickListener(v->action.run());
    }
    LinkProperties props() {
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        Network n=cm.getActiveNetwork(); if(n==null) return null;
        NetworkCapabilities caps=cm.getNetworkCapabilities(n);
        if(caps==null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null;
        return cm.getLinkProperties(n);
    }
    String getGateway() {
        LinkProperties p=props(); if(p==null) return null;
        for(RouteInfo r:p.getRoutes())
            if(r.isDefaultRoute() && r.getGateway() instanceof Inet4Address)
                return r.getGateway().getHostAddress();
        return null;
    }
    void discover() {
        String ip=getGateway();
        output.setText(ip==null?"Wi-Fi 연결 또는 IPv4 게이트웨이를 확인할 수 없습니다.":"현재 Wi-Fi 게이트웨이: "+ip+"\nKT 공유기인지 확인 후 접속하세요.");
        if(ip!=null) ipInput.setText(ip);
    }
    void openRouter(String host,int port) {
        if(host==null || !host.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) {
            output.setText("유효한 IPv4 주소를 입력하세요."); return;
        }
        for(String s:host.split("\\."))
            if(Integer.parseInt(s)>255) {output.setText("IP 주소 범위가 잘못되었습니다.");return;}
        showRouterWebView("http://"+host+":"+port+"/");
    }
    void showRouterWebView(String url) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);
        Button back = new Button(this);
        back.setText("← 앱 홈으로");
        back.setAllCaps(false);
        back.setOnClickListener(v -> closeRouterWebView());
        page.addView(back, new LinearLayout.LayoutParams(-1, (int)(52*getResources().getDisplayMetrics().density)));
        browser = new WebView(this);
        browser.setWebViewClient(new WebViewClient());
        browser.setWebChromeClient(new WebChromeClient());
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        page.addView(browser, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);
        browser.loadUrl(url);
    }
    void closeRouterWebView() {
        if(browser != null) {
            browser.stopLoading();
            browser.destroy();
            browser = null;
        }
        setContentView(homeScreen);
    }
    @Override public void onBackPressed() {
        if(browser != null) {
            if(browser.canGoBack()) browser.goBack();
            else closeRouterWebView();
        } else super.onBackPressed();
    }
    void showBand(String band) {
        output.setText(band+" 설정 안내\n1. 관리자 로그인(보안문자 포함)\n2. 무선랜 설정에서 해당 주파수 선택\n3. 활성/비활성, Wi-Fi 이름, 비밀번호 및 WPA 모드 설정\n4. 적용 후 Wi-Fi 재연결 및 필요시 관리자 재로그인\n\n주의: 이 버전은 설정 화면으로 안내하며 공유기 값을 직접 변경하지 않습니다.");
    }
    void diagnose() {
        LinkProperties p=props();
        if(p==null) {output.setText("Wi-Fi에 연결한 뒤 다시 시도하세요.");return;}
        StringBuilder s=new StringBuilder("Wi-Fi 네트워크 진단\n");
        s.append("게이트웨이: ").append(getGateway()).append("\n");
        p.getLinkAddresses().forEach(a->s.append("기기 IP: ").append(a.getAddress().getHostAddress()).append("\n"));
        p.getDnsServers().forEach(d->s.append("DNS: ").append(d.getHostAddress()).append("\n"));
        output.setText(s.toString());
    }
}
