package com.ktfield.routermanager;

import android.app.Activity;
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
import java.net.InetAddress;

public class MainActivity extends Activity {
    LinearLayout root;
    TextView output;
    EditText ipInput;
    int green = Color.rgb(21, 105, 78);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,32,28,32); root.setBackgroundColor(Color.rgb(244,249,246));
        scroll.addView(root); setContentView(scroll);
        TextView title = new TextView(this); title.setText("KT공유기 매니저"); title.setTextSize(27); title.setTextColor(green); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        TextView sub = new TextView(this); sub.setText("KT 현장 네트워크 점검 도구"); sub.setTextSize(15); sub.setPadding(0,12,0,28); root.addView(sub);
        addButton("1. 공유기 자동 검색", () -> discover());
        addButton("2. 관리자 페이지 연결", () -> openRouter(getGateway()));
        ipInput = new EditText(this); ipInput.setSingleLine(true); ipInput.setHint("공유기 IP 입력 (예: 192.168.1.1)"); ipInput.setInputType(InputType.TYPE_CLASS_TEXT); root.addView(ipInput);
        addButton("3. 수동 IP 접속", () -> openRouter(ipInput.getText().toString().trim()));
        addButton("4. 네트워크 진단", () -> diagnose());
        addButton("5. Wi-Fi 설정", () -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        output = new TextView(this); output.setText("준비되었습니다. Wi-Fi 연결 후 검색하세요."); output.setTextSize(16); output.setTextColor(Color.DKGRAY); output.setPadding(8,32,8,12); output.setTextIsSelectable(true); root.addView(output);
    }
    void addButton(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextSize(17); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, (int)(64*getResources().getDisplayMetrics().density)); p.bottomMargin=14; root.addView(b,p); b.setOnClickListener(v->action.run());
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
        for(RouteInfo r:p.getRoutes()) if(r.isDefaultRoute() && r.getGateway() instanceof Inet4Address) return r.getGateway().getHostAddress();
        return null;
    }
    void discover() {
        String ip=getGateway(); output.setText(ip==null?"Wi-Fi 연결 또는 IPv4 게이트웨이를 확인할 수 없습니다.":"현재 Wi-Fi 게이트웨이: " + ip + "\n※ 이 주소가 KT 공유기인지 확인하세요.");
        if(ip!=null) ipInput.setText(ip);
    }
    void openRouter(String host) {
        if(host==null || !host.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) { output.setText("유효한 IPv4 주소를 입력하거나 공유기를 검색하세요."); return; }
        String[] parts=host.split("\\."); for(String s:parts) if(Integer.parseInt(s)>255) { output.setText("IP 주소 범위가 잘못되었습니다."); return; }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+host+"/")));
        output.setText("브라우저에서 " + host + " 연결 시도. 관리자 로그인은 공유기에서 직접 진행하세요.");
    }
    void diagnose() {
        LinkProperties p=props(); if(p==null) {output.setText("Wi-Fi에 연결한 뒤 다시 시도하세요.");return;}
        StringBuilder s=new StringBuilder("Wi-Fi 네트워크 진단\n");
        s.append("게이트웨이: ").append(getGateway()).append("\n");
        p.getLinkAddresses().forEach(a -> s.append("기기 IP: ").append(a.getAddress().getHostAddress()).append("\n"));
        for(InetAddress dns:p.getDnsServers()) s.append("DNS: ").append(dns.getHostAddress()).append("\n");
        output.setText(s.toString());
    }
}
