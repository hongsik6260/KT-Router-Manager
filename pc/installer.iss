#define AppName "KT Router Manager PC"
[Setup]
AppId={{AB29E22B-2D3E-4CF9-B09B-8CCB9821A8ED}
AppName={#AppName}
AppVersion=4.0
DefaultDirName={autopf}\KT Router Manager PC
DefaultGroupName=KT Router Manager PC
OutputDir=output
OutputBaseFilename=KT_Router_Manager_PC_Setup_v3
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=app.ico
[Files]
Source: "publish\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion
[Icons]
Name: "{group}\KT 공유기 매니저 PC"; Filename: "{app}\KT_Router_Manager_PC.exe"
Name: "{autodesktop}\KT 공유기 매니저 PC"; Filename: "{app}\KT_Router_Manager_PC.exe"
[Run]
Filename: "{app}\KT_Router_Manager_PC.exe"; Description: "KT 공유기 매니저 PC 실행"; Flags: nowait postinstall skipifsilent