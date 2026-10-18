using System;using System.IO;using System.Text.Json;using System.Drawing;using System.Windows.Forms;using System.Security.Cryptography;using Microsoft.Web.WebView2.WinForms;
namespace KTRouterManager;
internal static class Program{[STAThread]static void Main(){ApplicationConfiguration.Initialize();Application.Run(new RouterForm());}}
internal sealed class RouterForm:Form{
 readonly WebView2 web=new(){Dock=DockStyle.Fill}; readonly string dir=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"KTRouterManagerPC");
 const string Router="http://172.30.1.254:8899/";
 record Account(string User,string Pass);
 public RouterForm(){
 Text="KT 공유기 매니저 PC";Width=1240;Height=940;MinimumSize=new Size(850,700);StartPosition=FormStartPosition.CenterScreen;
 BackColor=Color.FromArgb(248,251,249);
 var green=Color.FromArgb(0,128,77);
 var header=new Panel{Dock=DockStyle.Top,Height=108,BackColor=green};
 var logo=new Label{Text="kt service남부",ForeColor=Color.White,Font=new Font("Malgun Gothic",24,FontStyle.Bold),Location=new Point(25,18),AutoSize=true};
 var title=new Label{Text="KT 공유기 매니저",ForeColor=Color.White,Font=new Font("Malgun Gothic",23,FontStyle.Bold),Location=new Point(390,13),AutoSize=true};
 var subtitle=new Label{Text="GiGA WiFi home",ForeColor=Color.White,Font=new Font("Malgun Gothic",12),Location=new Point(394,61),AutoSize=true};
 header.Controls.AddRange(new Control[]{logo,title,subtitle});
 var dashboard=new TableLayoutPanel{Dock=DockStyle.Top,Height=410,ColumnCount=2,RowCount=2,Padding=new Padding(14,12,14,8)};
 dashboard.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,50));dashboard.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,50));
 dashboard.RowStyles.Add(new RowStyle(SizeType.Absolute,245));dashboard.RowStyles.Add(new RowStyle(SizeType.Percent,100));
 Panel AccountPanel(int n,string label,Color color){
 var p=new Panel{Dock=DockStyle.Fill,BackColor=n==1?Color.FromArgb(234,244,255):Color.FromArgb(231,249,239),Margin=new Padding(6),Padding=new Padding(12)};
 var h=new Label{Text=label,ForeColor=color,Font=new Font("Malgun Gothic",15,FontStyle.Bold),Dock=DockStyle.Top,Height=36};
 var fields=new TableLayoutPanel{Dock=DockStyle.Top,ColumnCount=2,RowCount=2,Height=80};
 fields.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,90));fields.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,100));
 var id=new TextBox{Dock=DockStyle.Fill,Font=new Font("Malgun Gothic",11)};
 var pw=new TextBox{Dock=DockStyle.Fill,Font=new Font("Malgun Gothic",11),UseSystemPasswordChar=true};
 var saved=Read(n);if(saved!=null){id.Text=saved.User;pw.Text=saved.Pass;}
 fields.Controls.Add(new Label{Text="아이디",Dock=DockStyle.Fill,TextAlign=ContentAlignment.MiddleLeft},0,0);fields.Controls.Add(id,1,0);
 fields.Controls.Add(new Label{Text="비밀번호",Dock=DockStyle.Fill,TextAlign=ContentAlignment.MiddleLeft},0,1);fields.Controls.Add(pw,1,1);
 var actions=new FlowLayoutPanel{Dock=DockStyle.Fill,FlowDirection=FlowDirection.LeftToRight,WrapContents=false};
 void SaveInline(){if(string.IsNullOrWhiteSpace(id.Text)||string.IsNullOrEmpty(pw.Text)){MessageBox.Show("아이디와 비밀번호를 입력해주세요.");return;}Directory.CreateDirectory(dir);File.WriteAllBytes(FileFor(n),ProtectedData.Protect(JsonSerializer.SerializeToUtf8Bytes(new Account(id.Text,pw.Text)),null,DataProtectionScope.CurrentUser));}
 void Btn(string txt,Action act){var b=new Button{Text=txt,Width=145,Height=43,BackColor=color,ForeColor=Color.White,FlatStyle=FlatStyle.Flat,Font=new Font("Malgun Gothic",10,FontStyle.Bold)};b.Click+=(_,_)=>act();actions.Controls.Add(b);}
 Btn("계정 저장",()=>{SaveInline();MessageBox.Show("계정 저장 요청을 처리했습니다.");});
 Btn("관리자 자동 입력",()=>{SaveInline();Fill(n);});
 p.Controls.Add(actions);p.Controls.Add(fields);p.Controls.Add(h);return p;
 }
 dashboard.Controls.Add(AccountPanel(1,"초기 관리자 계정",Color.FromArgb(20,106,196)),0,0);
 dashboard.Controls.Add(AccountPanel(2,"변경 관리자 계정",green),1,0);
 var settings=new FlowLayoutPanel{Dock=DockStyle.Fill,AutoScroll=true,Margin=new Padding(6),Padding=new Padding(10),BackColor=Color.White};
 void S(string text,Action act){var b=new Button{Text=text,Width=205,Height=53,BackColor=Color.FromArgb(234,246,241),FlatStyle=FlatStyle.Flat,Font=new Font("Malgun Gothic",10,FontStyle.Bold)};b.Click+=(_,_)=>act();settings.Controls.Add(b);}
 S("🌐 공유기 접속",()=>web.CoreWebView2?.Navigate(Router));
 S("⚙ 사용자 계정 설정",()=>MessageBox.Show("공유기 로그인 후 사용자 계정 설정 메뉴로 이동해주세요."));
 S("↻ 페이지 새로고침",()=>web.CoreWebView2?.Reload());
 S("🔑 계정 변경 5칸 입력",FillChange);
 dashboard.Controls.Add(settings,0,1);dashboard.SetColumnSpan(settings,2);
 var footer=new Label{Text="KT 공유기 매니저 PC  |  Ver. 3.0     kt service남부     충청본부 대전지점 대전지사 CS4팀(금산) 김홍식",Dock=DockStyle.Bottom,Height=42,TextAlign=ContentAlignment.MiddleCenter,Font=new Font("Malgun Gothic",10),ForeColor=Color.FromArgb(75,85,85)};
 Controls.Add(web);Controls.Add(footer);Controls.Add(dashboard);Controls.Add(header);
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