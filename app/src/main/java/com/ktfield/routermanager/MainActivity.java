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
    boolean pendingLoginFill = false;
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
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(17),dp(22),dp(17),dp(22));
        header.setBackgroundColor(Color.rgb(0,117,75));
        TextView logo=new TextView(this);
        logo.setText("kt service남부   │   KT 공유기 매니저");
        logo.setTextColor(Color.WHITE);logo.setTextSize(19);logo.setTypeface(null,Typeface.BOLD);
        header.addView(logo);
        TextView subtitle=new TextView(this);subtitle.setText("GiGA WiFi home");
        subtitle.setTextColor(Color.WHITE);subtitle.setTextSize(14);header.addView(subtitle);
        root.addView(header,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout accounts=new LinearLayout(this);accounts.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams a1=new LinearLayout.LayoutParams(0,-2,1);a1.setMargins(0,dp(12),dp(4),0);
        LinearLayout.LayoutParams a2=new LinearLayout.LayoutParams(0,-2,1);a2.setMargins(dp(4),dp(12),0,0);
        accounts.addView(makeAccountCard(1,"초기 관리자 계정",Color.rgb(15,106,205)),a1);
        accounts.addView(makeAccountCard(2,"변경 관리자 계정",Color.rgb(0,145,80)),a2);
        root.addView(accounts,new LinearLayout.LayoutParams(-1,-2));

        TextView section=new TextView(this);
        section.setText("🌐  공유기 접속 및 설정");
        section.setTextSize(18);section.setTypeface(null,Typeface.BOLD);
        section.setTextColor(Color.BLACK);section.setPadding(dp(4),dp(20),0,dp(10));
        root.addView(section);
        LinearLayout tiles=new LinearLayout(this);tiles.setOrientation(LinearLayout.HORIZONTAL);
        addTile(tiles,"공유기 접속",()->openRouter("172.30.1.254",8899));
        addTile(tiles,"사용자 계정 설정",()->openRouter("172.30.1.254",8899));
        root.addView(tiles);
        LinearLayout tiles2=new LinearLayout(this);tiles2.setOrientation(LinearLayout.HORIZONTAL);
        addTile(tiles2,"페이지 새로고침",()->openRouter("172.30.1.254",8899));
        addTile(tiles2,"로그아웃 / 닫기",()->new AlertDialog.Builder(this).setMessage("관리자 페이지를 닫을까요? 공유기 세션 로그아웃은 관리자 페이지에서 진행하세요.").setPositiveButton("닫기",(d,w)->closeRouterWebView()).setNegativeButton("취소",null).show());
        root.addView(tiles2);
        output=new TextView(this);
        output.setText("KT 공유기 Wi-Fi 연결 후 사용하세요. 보안문자는 직접 입력합니다.");
        output.setTextSize(12);output.setTextColor(Color.DKGRAY);
        output.setPadding(dp(8),dp(18),dp(8),dp(12));root.addView(output);
        TextView footer=new TextView(this);
        footer.setText("KT 공유기 매니저  |  Ver. 20\nkt service남부\n충청본부 대전지점 대전지사 CS4팀(금산) 김홍식");
        footer.setTextSize(12);footer.setTextColor(Color.DKGRAY);
        footer.setPadding(dp(8),dp(12),dp(8),dp(22));root.addView(footer);
    }
    void addTile(LinearLayout row,String text,Runnable action) {
        Button b=new Button(this);b.setText(text);b.setTextSize(13);b.setAllCaps(false);
        b.setTextColor(Color.rgb(10,65,58));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(223,243,235)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(62),1);
        p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(b,p);
        b.setOnClickListener(v->action.run());
    }
    LinearLayout makeAccountCard(int n,String name,int color) {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(7),dp(9),dp(7),dp(10));
        card.setBackgroundColor(n==1?Color.rgb(234,244,255):Color.rgb(231,249,239));
        TextView title=new TextView(this);title.setText(name);title.setTextColor(color);
        title.setTextSize(16);title.setTypeface(null,Typeface.BOLD);card.addView(title);
        TextView sub=new TextView(this);sub.setText(n==1?"공유기 초기 설정":"변경한 관리자 계정");
        sub.setTextSize(11);sub.setTextColor(Color.DKGRAY);card.addView(sub);
        EditText id=new EditText(this);id.setSingleLine(true);id.setTextSize(14);
        id.setHint("아이디");card.addView(id,new LinearLayout.LayoutParams(-1,dp(50)));
        EditText pw=new EditText(this);pw.setSingleLine(true);pw.setTextSize(14);
        pw.setHint("비밀번호");pw.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(pw,new LinearLayout.LayoutParams(-1,dp(50)));
        JSONObject saved=readCredentials(n);
        if(saved!=null){id.setText(saved.optString("user"));pw.setText(saved.optString("pass"));}
        Button save=makeGreenButton("계정 저장",()->{
            String u=id.getText().toString().trim(),p=pw.getText().toString();
            if(u.isEmpty()||p.isEmpty())Toast.makeText(this,"아이디와 비밀번호를 입력하세요.",Toast.LENGTH_LONG).show();
            else saveCredentials(n,u,p);
        });
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        save.setTextSize(12);card.addView(save,new LinearLayout.LayoutParams(-1,dp(49)));
        Button fill=makeGreenButton("자동 입력",()->{
            String u=id.getText().toString().trim(),p=pw.getText().toString();
            if(!u.isEmpty()&&!p.isEmpty())saveCredentials(n,u,p);
            selectedAccount=n;
            pendingLoginFill=true;
            if(browser==null)showRouterWebView("http://172.30.1.254:8899/");
            else {browser.loadUrl("http://172.30.1.254:8899/");}
        });
        fill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        fill.setTextSize(12);card.addView(fill,new LinearLayout.LayoutParams(-1,dp(49)));
        return card;
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
        back.setText("v20 · 앱 홈으로");
        back.setAllCaps(false);
        back.setOnClickListener(v -> closeRouterWebView());
        page.addView(back, new LinearLayout.LayoutParams(-1, (int)(52*getResources().getDisplayMetrics().density)));
        LinearLayout accountRow=new LinearLayout(this);
        accountRow.setOrientation(LinearLayout.HORIZONTAL);
        Button initial=makeGreenButton("초기 관리자",()->{selectedAccount=1;fillCredentials();});
        Button changed=makeGreenButton("변경 관리자",()->{selectedAccount=2;fillCredentials();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(55),1);
        accountRow.addView(initial,new LinearLayout.LayoutParams(0,dp(55),1));
        accountRow.addView(changed,new LinearLayout.LayoutParams(0,dp(55),1));
        page.addView(accountRow,new LinearLayout.LayoutParams(-1,-2));
        Button changeFill=makeGreenButton("계정 변경 화면 5개 항목 자동 입력",()->fillChangeForm());
        page.addView(changeFill,new LinearLayout.LayoutParams(-1,dp(54)));
        browser = new WebView(this);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if ("http".equals(request.getUrl().getScheme()) || "https".equals(request.getUrl().getScheme())) return false;
                return true;
            }
            @Override public void onPageFinished(WebView view,String url) {
                if(pendingLoginFill) {
                    pendingLoginFill=false;
                    view.postDelayed(()->fillCredentials(),500);
                }
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
    void fillChangeForm() {
        if(browser==null) return;
        Uri uri=Uri.parse(browser.getUrl()==null?"":browser.getUrl());
        if(!"172.30.1.254".equals(uri.getHost()) || uri.getPort()!=8899) {
            Toast.makeText(this,"확인된 KT 공유기 관리 주소에서만 입력할 수 있습니다.",Toast.LENGTH_LONG).show();return;
        }
        JSONObject oldAccount=readCredentials(1),newAccount=readCredentials(2);
        if(oldAccount==null || newAccount==null) {
            Toast.makeText(this,"홈에서 초기 관리자와 변경 관리자 계정을 모두 저장해주세요.",Toast.LENGTH_LONG).show();return;
        }
        String[] values={oldAccount.optString("user"),newAccount.optString("user"),oldAccount.optString("pass"),newAccount.optString("pass"),newAccount.optString("pass")};
        for(String value:values) if(value.isEmpty()) {
            Toast.makeText(this,"저장된 계정 정보가 비어 있습니다.",Toast.LENGTH_LONG).show();return;
        }
        String data=new org.json.JSONArray(java.util.Arrays.asList(values)).toString();
        String js="(function(){"+
          "var text=(document.body&&document.body.innerText)||'';"+
          "if(!text.includes('사용자 계정 설정')||!text.includes('변경할 비밀번호'))return 'wrong_page';"+
          "var inputs=Array.from(document.querySelectorAll('input')).filter(x=>"+
          "['text','password'].includes((x.type||'').toLowerCase())&&!x.disabled&&!x.readOnly&&x.getClientRects().length>0);"+
          "if(inputs.length!==5)return 'fields_'+inputs.length;"+
          "var vals="+data+";"+
          "for(var i=0;i<5;i++){var el=inputs[i];var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,vals[i]);el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));}"+
          "return 'ok';})()";
        browser.evaluateJavascript(js,result->{
            if("\"ok\"".equals(result)) Toast.makeText(this,"5개 항목 입력 완료. 확인 후 적용을 직접 눌러주세요.",Toast.LENGTH_LONG).show();
            else Toast.makeText(this,"자동 입력하지 않았습니다. 계정 변경 화면과 입력칸을 확인해주세요. ("+result+")",Toast.LENGTH_LONG).show();
        });
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
