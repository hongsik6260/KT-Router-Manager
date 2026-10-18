using System;using System.IO;using System.Text.Json;using System.Drawing;using System.Windows.Forms;using System.Security.Cryptography;using Microsoft.Web.WebView2.WinForms;
namespace KTRouterManager;
internal static class Program{[STAThread]static void Main(){ApplicationConfiguration.Initialize();Application.Run(new RouterForm());}}
internal sealed class RouterForm:Form{
 readonly WebView2 web=new(){Dock=DockStyle.Fill}; readonly string dir=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"KTRouterManagerPC");
 const string Router="http://172.30.1.254:8899/";
 record Account(string User,string Pass);
 public RouterForm(){
 Text="KT 공유기 매니저 PC";Width=1120;Height=840;StartPosition=FormStartPosition.CenterScreen;
 var top=new FlowLayoutPanel{Dock=DockStyle.Top,Height=70,BackColor=Color.FromArgb(12,111,76),Padding=new Padding(8)};
 void B(string title,Action action){var b=new Button{Text=title,AutoSize=true,Height=42,BackColor=Color.White};b.Click+=(_,_)=>action();top.Controls.Add(b);}
 B("공유기 접속",()=>web.CoreWebView2?.Navigate(Router));
 B("초기 관리자 저장",()=>Save(1));B("변경 관리자 저장",()=>Save(2));
 B("초기 관리자 입력",()=>Fill(1));B("변경 관리자 입력",()=>Fill(2));
 B("계정 변경 5칸 입력",FillChange);B("새로고침",()=>web.CoreWebView2?.Reload());
 var footer=new Label{Text="KT service남부  |  KT 공유기 매니저 PC v1.0  |  충청본부 대전지점 대전지사 CS4팀(금산) 김홍식",Dock=DockStyle.Bottom,Height=36,TextAlign=ContentAlignment.MiddleCenter};
 Controls.Add(web);Controls.Add(footer);Controls.Add(top);
 Shown+=async(_,_)=>{try{await web.EnsureCoreWebView2Async();web.CoreWebView2.Navigate(Router);}catch(Exception ex){MessageBox.Show("Microsoft Edge WebView2 Runtime이 필요합니다. "+ex.Message);}};
 }
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