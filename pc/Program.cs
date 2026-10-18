using System;using System.IO;using System.Text.Json;using System.Drawing;using System.Windows.Forms;using System.Security.Cryptography;using Microsoft.Web.WebView2.WinForms;
namespace KTRouterManager;
internal static class Program{[STAThread]static void Main(){ApplicationConfiguration.Initialize();Application.Run(new RouterForm());}}
internal sealed class RouterForm:Form{
 readonly WebView2 web=new(){Dock=DockStyle.Fill}; readonly string dir=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"KTRouterManagerPC");
 const string Router="http://172.30.1.254:8899/";
 int pendingLoginAccount=0;
 record Account(string User,string Pass);
 readonly Panel toolbar=new(){Dock=DockStyle.Top,Height=58,BackColor=Color.FromArgb(0,116,73),Visible=false};
 bool dashboard=true;
 public RouterForm(){
  Text="KT 공유기 매니저 PC";Width=1280;Height=850;MinimumSize=new Size(980,690);
  StartPosition=FormStartPosition.CenterScreen;BackColor=Color.White;
  var home=new Button{Text="⌂  앱 홈으로",Width=155,Height=44,Left=10,Top=7,BackColor=Color.White,FlatStyle=FlatStyle.Flat,Font=new Font("Malgun Gothic",11,FontStyle.Bold)};
  home.Click+=(_,_)=>ShowHome();
  var initial=new Button{Text="초기 관리자",Width=145,Height=44,Left=175,Top=7,BackColor=Color.White};
  initial.Click+=(_,_)=>Fill(1);
  var changed=new Button{Text="변경 관리자",Width=145,Height=44,Left=330,Top=7,BackColor=Color.White};
  changed.Click+=(_,_)=>Fill(2);
  var change=new Button{Text="계정 변경 화면 5개 항목 자동 입력",Width=300,Height=44,Left=485,Top=7,BackColor=Color.White};
  change.Click+=(_,_)=>FillChange();
  toolbar.Controls.AddRange(new Control[]{home,initial,changed,change});
  Controls.Add(web);Controls.Add(toolbar);
  Shown+=async(_,_)=>{
   try{
    await web.EnsureCoreWebView2Async();
    web.CoreWebView2.Settings.IsWebMessageEnabled=true;
    web.CoreWebView2.WebMessageReceived+=async(_,args)=>await HandleMessage(args.WebMessageAsJson);
    web.CoreWebView2.NavigationCompleted+=async(_,args)=>{
     if(pendingLoginAccount!=0){int n=pendingLoginAccount;pendingLoginAccount=0;await System.Threading.Tasks.Task.Delay(550);Fill(n);}
    };
    ShowHome();
   }catch(Exception ex){MessageBox.Show("Microsoft Edge WebView2 Runtime이 필요합니다. "+ex.Message);}
  };
 }
 void ShowHome(){
  if(web.CoreWebView2==null)return;
  dashboard=true;toolbar.Visible=false;
  var a=Read(1);var b=Read(2);
  string json=JsonSerializer.Serialize(new{first=a?.User??"",firstPass=a?.Pass??"",second=b?.User??"",secondPass=b?.Pass??""});
  string html=DashboardHtml.Replace("__DATA__",json);
  web.NavigateToString(html);
 }
 void OpenRouter(int account=0){
  if(web.CoreWebView2==null)return;
  dashboard=false;toolbar.Visible=true;pendingLoginAccount=account;
  web.CoreWebView2.Navigate(Router);
 }
 async System.Threading.Tasks.Task HandleMessage(string message){
  if(!dashboard)return;
  try{
   using var doc=JsonDocument.Parse(message);var root=doc.RootElement;
   string action=root.GetProperty("action").GetString()??"";
   if(action=="save"||action=="fill"){
    int n=root.GetProperty("slot").GetInt32();
    if(n!=1&&n!=2)return;
    string user=root.GetProperty("user").GetString()??"";
    string pass=root.GetProperty("pass").GetString()??"";
    if(string.IsNullOrWhiteSpace(user)||string.IsNullOrEmpty(pass)){MessageBox.Show("아이디와 비밀번호를 입력해주세요.");return;}
    Directory.CreateDirectory(dir);
    File.WriteAllBytes(FileFor(n),ProtectedData.Protect(JsonSerializer.SerializeToUtf8Bytes(new Account(user,pass)),null,DataProtectionScope.CurrentUser));
    if(action=="fill")OpenRouter(n);
    else MessageBox.Show("계정을 암호화 저장했습니다.");
   }else if(action=="open")OpenRouter();
   else if(action=="settings"){OpenRouter();MessageBox.Show("로그인 후 공유기의 사용자 계정 설정 메뉴로 이동해주세요.");}
   else if(action=="refresh")ShowHome();
   else if(action=="logout")ShowHome();
  }catch(Exception ex){MessageBox.Show("작업을 완료하지 못했습니다: "+ex.Message);}
  await System.Threading.Tasks.Task.CompletedTask;
 }
 const string DashboardHtml = """
 <!doctype html><html lang="ko"><head><meta charset="utf-8">
 <meta name="viewport" content="width=device-width,initial-scale=1">
 <style>
 *{box-sizing:border-box}body{margin:0;background:#f7faf9;color:#18232d;font-family:'Malgun Gothic','Segoe UI',sans-serif}
 .top{background:linear-gradient(120deg,#078b55,#00663d);color:white;border-radius:0 0 14px 14px;padding:24px 36px;display:flex;align-items:center;gap:30px;box-shadow:0 4px 18px #155b3a29}
 .brand{font-size:38px;font-weight:900;white-space:nowrap;letter-spacing:-2px}.brand .kt{font-size:52px;color:white}.brand .service{color:#f52042}.bar{height:66px;width:2px;background:#d3f4e3}.heading{font-size:34px;font-weight:800}.sub{font-size:19px;margin-top:4px}.topright{margin-left:auto;font-size:30px}
 main{padding:22px 26px 14px;max-width:1600px;margin:auto}
 .accounts{display:grid;grid-template-columns:1fr 1fr;gap:22px}
 .card{border-radius:19px;border:2px solid #d5e6f5;background:#f9fcff;overflow:hidden;box-shadow:0 3px 12px #0000000c}
 .card.green{border-color:#c5ebd9;background:#f7fdf9}
 .cardhead{background:#e8f4ff;padding:17px 22px;display:flex;gap:20px;align-items:center}
 .green .cardhead{background:#e2f8eb}
 .avatar{font-size:47px;color:#0874d0}.green .avatar{color:#00965a}
 .cardtitle{font-size:26px;font-weight:800}.hint{color:#777;font-size:15px;margin-top:5px}
 .fields{padding:14px 22px 20px}
 .field{display:grid;grid-template-columns:100px 1fr;align-items:center;gap:10px;margin-bottom:12px}
 label{font-weight:700;font-size:18px}
 input{width:100%;font-size:20px;padding:11px 15px;border:2px solid #d1d5da;border-radius:12px;background:white;outline:none}
 input:focus{border-color:#0d8d65;box-shadow:0 0 0 3px #1ab97d25}
 .actions{display:flex;gap:12px;margin-top:16px}
 button{cursor:pointer;font-family:inherit}
 .action{border:0;border-radius:13px;background:linear-gradient(#158deb,#0568c5);color:white;font-weight:800;font-size:19px;padding:15px 8px;flex:1;box-shadow:0 4px 8px #0000001c}
 .green .action{background:linear-gradient(#06a35f,#007b43)}
 .save{flex:0 0 105px;font-size:15px;background:#dcecff;color:#12578c;box-shadow:none}
 .green .save{background:#d6f5e3;color:#087147}
 .settings{border:2px solid #e3e7ea;border-radius:19px;margin-top:20px;padding:15px 18px 20px;background:white}
 .settings h2{margin:0 0 16px;font-size:24px}
 .tiles{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
 .tile{display:flex;align-items:center;gap:13px;text-align:left;padding:16px;border-radius:14px;border:1px solid #e0e8ef;background:#eaf5ff;min-height:92px}
 .tile:nth-child(2){background:#e6f9ef}.tile:nth-child(3){background:#fff6df}.tile:nth-child(4){background:#fff0f0}
 .tile .emoji{font-size:35px}.tile strong{font-size:19px}.tile small{display:block;color:#6c737b;font-size:13px;margin-top:5px}
 footer{border-top:2px solid #e1e6e5;margin:17px 2px 0;padding:15px 6px;color:#424c52;display:flex;align-items:center;justify-content:space-between;gap:15px;font-size:14px}
 footer b{font-size:17px;color:#087c4f}.warning{font-size:12px;color:#7d8587;margin-top:10px}
 @media(max-width:1050px){.top{padding:18px}.brand{font-size:25px}.brand .kt{font-size:35px}.heading{font-size:25px}.cardtitle{font-size:20px}.field{grid-template-columns:1fr}.tiles{grid-template-columns:repeat(2,1fr)}}
 </style></head><body>
 <header class="top"><div class="brand"><span class="kt">kt</span> <span class="service">service 남부</span></div><div class="bar"></div><div><div class="heading">KT 공유기 매니저</div><div class="sub">GiGA WiFi home</div></div><div class="topright">⚙ ⓘ ⋮</div></header>
 <main><div class="accounts">
 <section class="card"><div class="cardhead"><div class="avatar">👤</div><div><div class="cardtitle">초기 관리자 계정</div><div class="hint">기본 관리자 계정 (공유기 초기 설정)</div></div></div>
 <div class="fields"><div class="field"><label for="u1">아이디</label><input id="u1" autocomplete="off"></div><div class="field"><label for="p1">비밀번호</label><input id="p1" type="password" autocomplete="off"></div>
 <div class="actions"><button class="action save" onclick="send('save',1)">저장</button><button class="action" onclick="send('fill',1)">🔑 초기 관리자 자동 입력</button></div></div></section>
 <section class="card green"><div class="cardhead"><div class="avatar">👤</div><div><div class="cardtitle">변경 관리자 계정</div><div class="hint">변경한 관리자 계정 (계정 변경 시 사용)</div></div></div>
 <div class="fields"><div class="field"><label for="u2">아이디</label><input id="u2" autocomplete="off"></div><div class="field"><label for="p2">비밀번호</label><input id="p2" type="password" autocomplete="off"></div>
 <div class="actions"><button class="action save" onclick="send('save',2)">저장</button><button class="action" onclick="send('fill',2)">🔑 변경 관리자 자동 입력</button></div></div></section></div>
 <section class="settings"><h2>🌐 공유기 접속 및 설정</h2><div class="tiles">
 <button class="tile" onclick="send('open')"><span class="emoji">🌐</span><span><strong>공유기 접속</strong><small>관리자 페이지 열기</small></span></button>
 <button class="tile" onclick="send('settings')"><span class="emoji">⚙️</span><span><strong>사용자 계정 설정</strong><small>로그인 후 설정 메뉴로 이동</small></span></button>
 <button class="tile" onclick="send('refresh')"><span class="emoji">🔄</span><span><strong>페이지 새로고침</strong><small>현재 첫 화면 다시 표시</small></span></button>
 <button class="tile" onclick="send('logout')"><span class="emoji">🚪</span><span><strong>로그아웃</strong><small>첫 화면으로 돌아가기</small></span></button>
 </div></section>
 <footer><div>📶 <b>KT 공유기 매니저</b> &nbsp; | &nbsp; Ver. 6.0 <div class="hint">GiGA WiFi home</div></div><div><b>kt service남부</b> &nbsp; | &nbsp; 충청본부 대전지점 대전지사 CS4팀(금산) 김홍식</div></footer>
 <div class="warning">KT 공유기 Wi-Fi에 연결한 상태에서 사용하세요. 보안문자는 직접 입력해야 합니다. 계정 정보는 Windows 사용자 계정으로 암호화해 저장합니다.</div></main>
 <script>
 const saved=__DATA__;
 document.getElementById('u1').value=saved.first;document.getElementById('p1').value=saved.firstPass;
 document.getElementById('u2').value=saved.second;document.getElementById('p2').value=saved.secondPass;
 function send(action,slot){const data={action};if(slot){data.slot=slot;data.user=document.getElementById('u'+slot).value;data.pass=document.getElementById('p'+slot).value;}window.chrome.webview.postMessage(data);}
 </script></body></html>
 """;
 string FileFor(int n)=>Path.Combine(dir,"account"+n+".dat");
 Account? Read(int n){try{return JsonSerializer.Deserialize<Account>(ProtectedData.Unprotect(File.ReadAllBytes(FileFor(n)),null,DataProtectionScope.CurrentUser));}catch{return null;}}
 void Save(int n){
 using var form=new Form{Text=n==1?"초기 관리자":"변경 관리자",Width=420,Height=230,StartPosition=FormStartPosition.CenterParent};
 var id=new TextBox{Left=120,Top=20,Width=250};var pw=new TextBox{Left=120,Top=65,Width=250,UseSystemPasswordChar=true};
 var ok=new Button{Text="암호화 저장",Left=120,Top=120,Width=250};
 form.Controls.AddRange(new Control[]{new Label{Text="아이디",Left=15,Top=25},new Label{Text="비밀번호",Left=15,Top=70},id,pw,ok});
 ok.Click+=(_,_)=>{if(string.IsNullOrWhiteSpace(id.Text)||string.IsNullOrEmpty(pw.Text)){MessageBox.Show("계정을 입력해주세요.");return;}Directory.CreateDirectory(dir);File.WriteAllBytes(FileFor(n),ProtectedData.Protect(JsonSerializer.SerializeToUtf8Bytes(new Account(id.Text,pw.Text)),null,DataProtectionScope.CurrentUser));form.Close();};
 form.ShowDialog(this);
 }
 bool Safe(){if(web.CoreWebView2==null||!Uri.TryCreate(web.Source?.ToString(),UriKind.Absolute,out var u)||u.Host!="172.30.1.254"||u.Port!=8899){MessageBox.Show("KT 공유기 관리 주소에서만 자동 입력합니다.");return false;}return true;}
 async void Fill(int n){if(!Safe())return;var a=Read(n);if(a==null){MessageBox.Show("계정을 먼저 저장해주세요.");return;}
 var js="(()=>{const f=[...document.querySelectorAll('input')];const p=f.find(x=>x.type==='password');const u=f.find(x=>x.type==='text'||x.type==='email');if(!u||!p)return false;u.value="+JsonSerializer.Serialize(a.User)+";p.value="+JsonSerializer.Serialize(a.Pass)+";for(const x of [u,p]){x.dispatchEvent(new Event('input',{bubbles:true}));x.dispatchEvent(new Event('change',{bubbles:true}));}return true})()";
 try{MessageBox.Show((await web.CoreWebView2!.ExecuteScriptAsync(js))=="true"?"계정 입력 완료. 보안문자를 직접 입력하세요.":"입력칸을 찾지 못했습니다.");}catch(Exception ex){MessageBox.Show(ex.Message);}
 }
 async void FillChange(){if(!Safe())return;var a=Read(1);var b=Read(2);if(a==null||b==null){MessageBox.Show("두 계정을 모두 저장해주세요.");return;}
 var vals=JsonSerializer.Serialize(new[]{a.User,b.User,a.Pass,b.Pass,b.Pass});
 var js="(()=>{if(!(document.body?.innerText||'').includes('사용자 계정 설정'))return 'wrong_page';const f=[...document.querySelectorAll('input')].filter(x=>['text','password'].includes(x.type)&&!x.disabled&&x.getClientRects().length>0);if(f.length!==5)return 'fields_'+f.length;const v="+vals+";f.forEach((x,i)=>{x.value=v[i];x.dispatchEvent(new Event('input',{bubbles:true}));x.dispatchEvent(new Event('change',{bubbles:true}));});return 'ok'})()";
 try{MessageBox.Show((await web.CoreWebView2!.ExecuteScriptAsync(js))=="\"ok\""?"5칸 입력 완료. 확인 후 적용을 직접 눌러주세요.":"계정 변경 화면의 입력칸을 찾지 못했습니다.");}catch(Exception ex){MessageBox.Show(ex.Message);}
 }
}