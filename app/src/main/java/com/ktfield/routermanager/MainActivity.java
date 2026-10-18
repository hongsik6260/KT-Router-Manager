package com.ktfield.routermanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.view.ViewGroup;
import android.view.WindowInsets;
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
    final String KEY_ALIAS = "kt_router_credentials_v9";
    int selectedAccount = 2;
    final int green = Color.rgb(21, 105, 78);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(244,249,246));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this);
        homeScreen = scroll;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,24,28,32);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            int top=insets.getInsets(WindowInsets.Type.statusBars()).top;
            int bottom=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            scroll.setPadding(0,top,0,bottom);
            return insets;
        });
        root.setBackgroundColor(Color.rgb(244,249,246));
        scroll.addView(root);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("KT 공유기 매니저");
        title.setTextSize(22);
        title.setIncludeFontPadding(true);
        title.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
        title.setTextColor(green);
        title.setTypeface(null,Typeface.BOLD);
        root.addView(title);
        label("v14 · GiGA WiFi home ax · KM15-103H\n관리자 로그인 및 Wi-Fi 설정 도우미");
        addButton("1. KT AX 관리자 로그인", () -> openRouter("172.30.1.254",8899));
        LinearLayout accounts = new LinearLayout(this);
        accounts.setOrientation(LinearLayout.HORIZONTAL);
        Button firstAccount = makeGreenButton("초기 관리자", () -> credentialsDialog(1));
        Button secondAccount = makeGreenButton("변경 관리자", () -> credentialsDialog(2));
        LinearLayout.LayoutParams firstP = new LinearLayout.LayoutParams(0,dp(60),1);
        firstP.setMargins(0,0,dp(4),0);
        LinearLayout.LayoutParams secondP = new LinearLayout.LayoutParams(0,dp(60),1);
        secondP.setMargins(dp(4),0,0,0);
        accounts.addView(firstAccount,firstP);
        accounts.addView(secondAccount,secondP);
        LinearLayout.LayoutParams accountRowP=new LinearLayout.LayoutParams(-1,-2);
        accountRowP.bottomMargin=dp(10);
        root.addView(accounts,accountRowP);
        addButton("관리자 계정 선택 / 삭제", () -> chooseOrDeleteAccount());
        addButton("2. 현재 공유기 자동 검색", () -> discover());
        ipInput = new EditText(this);
        ipInput.setSingleLine(true);
        ipInput.setHint("공유기 IP (예: 172.30.1.254)");
        ipInput.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(ipInput);
        addButton("3. 입력한 IP로 관리자 접속", () -> openRouter(ipInput.getText().toString().trim(),8899));
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
    int dp(int n) {return (int)(n*getResources().getDisplayMetrics().density);}
    Button makeGreenButton(String label,Runnable action) {
        Button b=new Button(this);
        b.setText(label);b.setTextSize(15);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        b.setOnClickListener(v->action.run());
        return b;
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
        page.setOnApplyWindowInsetsListener((v,insets)->{
            int top=insets.getInsets(WindowInsets.Type.statusBars()).top;
            int bottom=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            page.setPadding(0,top,0,bottom);
            return insets;
        });
        Button back = new Button(this);
        back.setText("v13 · 앱 홈으로");
        back.setAllCaps(false);
        back.setOnClickListener(v -> closeRouterWebView());
        page.addView(back, new LinearLayout.LayoutParams(-1, (int)(52*getResources().getDisplayMetrics().density)));
        LinearLayout accountRow = new LinearLayout(this);
        accountRow.setOrientation(LinearLayout.HORIZONTAL);
        accountRow.setPadding(8,8,8,8);
        Button initial = new Button(this);
        initial.setText("초기 관리자");
        initial.setAllCaps(false);
        initial.setTextColor(Color.WHITE);
        initial.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        initial.setOnClickListener(v -> { selectedAccount=1; fillCredentials(); });
        Button changed = new Button(this);
        changed.setText("변경 관리자");
        changed.setAllCaps(false);
        changed.setTextColor(Color.WHITE);
        changed.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        changed.setOnClickListener(v -> { selectedAccount=2; fillCredentials(); });
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0,(int)(64*getResources().getDisplayMetrics().density),1);
        left.setMargins(0,0,4,0);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0,(int)(64*getResources().getDisplayMetrics().density),1);
        right.setMargins(4,0,0,0);
        accountRow.addView(initial,left);
        accountRow.addView(changed,right);
        page.addView(accountRow,new LinearLayout.LayoutParams(-1,-2));
        browser = new WebView(this);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if ("http".equals(request.getUrl().getScheme()) || "https".equals(request.getUrl().getScheme())) return false;
                return true;
            }
            @Override public void onPageFinished(WebView view,String url) {
                // User selects which account to fill; never auto-fill the wrong profile.
            }
            @Override public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                if(request.isForMainFrame()) Toast.makeText(MainActivity.this, "관리자 페이지 접속 오류: "+error.getDescription(), Toast.LENGTH_LONG).show();
            }
        });
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
    SecretKey secretKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if(!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
    }
    void saveCredentials(int account,String user,String pass) {
        try {
            JSONObject data = new JSONObject();
            data.put("user",user); data.put("pass",pass);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE,secretKey());
            SharedPreferences p = getSharedPreferences("local_credentials_"+account,MODE_PRIVATE);
            p.edit().putString("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP))
              .putString("cipher",Base64.encodeToString(c.doFinal(data.toString().getBytes("UTF-8")),Base64.NO_WRAP)).apply();
            Toast.makeText(this,"이 기기에 암호화 저장했습니다.",Toast.LENGTH_SHORT).show();
        } catch(Exception e) { Toast.makeText(this,"저장 실패: "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show(); }
    }
    JSONObject readCredentials(int account) {
        try {
            SharedPreferences p=getSharedPreferences("local_credentials_"+account,MODE_PRIVATE);
            String iv=p.getString("iv",null),cipher=p.getString("cipher",null);
            if(iv==null||cipher==null) return null;
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,secretKey(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
            return new JSONObject(new String(c.doFinal(Base64.decode(cipher,Base64.NO_WRAP)),"UTF-8"));
        } catch(Exception e) {Toast.makeText(this,"저장 정보 해독 실패",Toast.LENGTH_LONG).show();return null;}
    }
    void credentialsDialog(int account) {
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32,16,32,0);
        EditText id=new EditText(this);id.setHint("관리자 아이디");box.addView(id);
        EditText pw=new EditText(this);pw.setHint("관리자 비밀번호");
        pw.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(pw);
        new AlertDialog.Builder(this).setTitle(account+"번 관리자 계정 암호화 저장")
          .setMessage("휴대폰 내부에만 저장합니다. 보안문자는 저장하지 않습니다.")
          .setView(box).setNegativeButton("취소",null)
          .setPositiveButton("저장",(d,w)->{
              String u=id.getText().toString().trim(),p=pw.getText().toString();
              if(u.isEmpty()||p.isEmpty()) Toast.makeText(this,"아이디와 비밀번호를 모두 입력하세요.",Toast.LENGTH_LONG).show();
              else saveCredentials(account,u,p);
          }).show();
    }
    void chooseOrDeleteAccount() {
        new AlertDialog.Builder(this).setTitle("삭제할 관리자 계정 선택")
          .setItems(new String[]{"1번 초기 관리자 계정 삭제","2번 수정된 관리자 계정 삭제"},(d,which)->{
              int account=which+1;
              new AlertDialog.Builder(this).setTitle(account+"번 계정 삭제")
                .setMessage("저장된 아이디와 비밀번호를 삭제할까요?")
                .setNegativeButton("취소",null)
                .setPositiveButton("삭제",(dialog,w)->{
                    getSharedPreferences("local_credentials_"+account,MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this,account+"번 계정 삭제 완료",Toast.LENGTH_SHORT).show();
                }).show();
          }).show();
    }
    void fillCredentials() {
        if(browser==null) return;
        Uri uri=Uri.parse(browser.getUrl()==null?"":browser.getUrl());
        if(!"172.30.1.254".equals(uri.getHost()) || uri.getPort()!=8899) {
            Toast.makeText(this,"확인된 KT 공유기 주소에서만 자동 입력할 수 있습니다.",Toast.LENGTH_LONG).show();
            return;
        }
        JSONObject c=readCredentials(selectedAccount);
        if(c==null) {Toast.makeText(this,"홈에서 관리자 계정을 먼저 저장하세요.",Toast.LENGTH_LONG).show();return;}
        String user=c.optString("user"),pass=c.optString("pass");
        String js="(function(){var f=Array.from(document.querySelectorAll('input'));"+
          "var pw=f.find(x=>x.type==='password');"+
          "var id=f.find(x=>x!==pw && (x.type==='text'||x.type==='email') && !/captcha|code|verify/i.test((x.name||'')+' '+(x.id||'')));"+
          "function set(el,val){if(!el)return;var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,val);el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));}"+
          "set(id,"+JSONObject.quote(user)+");set(pw,"+JSONObject.quote(pass)+");return !!(id&&pw);})()";
        browser.evaluateJavascript(js,result->Toast.makeText(this,"true".equals(result)?"계정 입력 완료. 보안문자를 직접 입력하세요.":"입력칸을 찾지 못했습니다. 직접 입력해주세요.",Toast.LENGTH_LONG).show());
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
