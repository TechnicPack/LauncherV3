[Setup]
AppId=TechnicLauncherBootstrap
AppName=Technic Launcher
AppVersion=4.0
AppVerName=Technic Launcher 4.0
AppPublisher=Syndicate, LLC
DefaultDirName={{ defaultDirName }}
DefaultGroupName=Technic Launcher
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputBaseFilename=TechnicLauncherBootstrap
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\{{ launcherExecutableName }}
ArchiveExtraction=full

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; Flags: {{ desktopShortcutFlags }}

[Icons]
Name: "{group}\Technic Launcher"; Filename: "{app}\{{ launcherExecutableName }}"
Name: "{autodesktop}\Technic Launcher"; Filename: "{app}\{{ launcherExecutableName }}"; Tasks: desktopicon

[Registry]
Root: {{ uninstallRegistryRoot }}; Subkey: "Software\Technic Launcher"; ValueType: string; ValueName: "InstallLocation"; ValueData: "{app}"; Flags: uninsdeletekey

[UninstallDelete]
Type: files; Name: "{app}\launcher.exe"
Type: filesandordirs; Name: "{app}\launcher-runtime"
Type: files; Name: "{app}\unins*.exe"
Type: files; Name: "{app}\unins*.dat"

[Run]
Filename: "{app}\{{ launcherExecutableName }}"; Description: "{cm:LaunchProgram,Technic Launcher}"; Flags: {{ launchAfterInstallFlags }}

[Code]
var
  DownloadPage: TDownloadWizardPage;
  DeleteAllLauncherData: Boolean;

function LauncherDownloadPath(): String;
begin
  Result := ExpandConstant('{tmp}\{{ launcherExecutableName }}');
end;

function LauncherInstalledPath(): String;
begin
  Result := ExpandConstant('{app}\{{ launcherExecutableName }}');
end;

function RuntimeArchivePath(): String;
begin
  Result := ExpandConstant('{tmp}\launcher-runtime.zip');
end;

function RuntimeExtractPath(): String;
begin
  Result := ExpandConstant('{tmp}\launcher-runtime-extracted');
end;

function TempRuntimeStagingPath(): String;
begin
  Result := ExpandConstant('{tmp}\launcher-runtime-staging');
end;

function AppRuntimePath(): String;
begin
  Result := ExpandConstant('{app}\{{ runtimeDirectory }}');
end;

function AppRuntimeStagingPath(): String;
begin
  Result := ExpandConstant('{app}\launcher-runtime-staging');
end;

function PreviousRuntimePath(): String;
begin
  Result := ExpandConstant('{app}\launcher-runtime-previous');
end;

function LauncherStagingPath(): String;
begin
  Result := ExpandConstant('{app}\{{ launcherExecutableName }}.staging');
end;

function PreviousLauncherPath(): String;
begin
  Result := ExpandConstant('{app}\{{ launcherExecutableName }}.previous');
end;

procedure DownloadInstallerArtifacts;
begin
  DownloadPage := CreateDownloadPage(SetupMessage(msgWizardPreparing), SetupMessage(msgPreparingDesc), nil);
  DownloadPage.Clear;
  DownloadPage.ShowBaseNameInsteadOfUrl := True;
  DownloadPage.Add('{{ runtimeArchiveUrl }}', 'launcher-runtime.zip', '');
  DownloadPage.Add('{{ launcherDownloadUrl }}', '{{ launcherExecutableName }}', '');
  DownloadPage.Show;
  try
    DownloadPage.Download;
  finally
    DownloadPage.Hide;
  end;
end;

function ResolveExtractedLauncherRuntimeRoot(const ExtractedPath: String): String;
var
  FindRec: TFindRec;
  ChildDirectory: String;
  DirectoryCount: Integer;
begin
  if FileExists(AddBackslash(ExtractedPath) + '{{ runtimeArchiveExecutableRelativePath }}') then
  begin
    Result := ExtractedPath;
    exit;
  end;

  DirectoryCount := 0;
  ChildDirectory := '';

  if FindFirst(AddBackslash(ExtractedPath) + '*', FindRec) then
  begin
    try
      repeat
        if (FindRec.Name <> '.') and (FindRec.Name <> '..') then
        begin
          if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then
          begin
            DirectoryCount := DirectoryCount + 1;
            ChildDirectory := AddBackslash(ExtractedPath) + FindRec.Name;
          end
          else
            RaiseException('Downloaded launcher runtime has unexpected layout');
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  if (DirectoryCount = 1) and FileExists(AddBackslash(ChildDirectory) + '{{ runtimeArchiveExecutableRelativePath }}') then
    Result := ChildDirectory
  else
    RaiseException('Downloaded launcher runtime has unexpected layout');
end;

procedure ValidateLauncherRuntimeRoot(const RuntimeRoot: String);
begin
  if not FileExists(AddBackslash(RuntimeRoot) + '{{ runtimeArchiveExecutableRelativePath }}') then
    RaiseException('Downloaded launcher runtime is missing bin\javaw.exe');
  if not DirExists(AddBackslash(RuntimeRoot) + 'lib') then
    RaiseException('Downloaded launcher runtime is missing lib');
  if not FileExists(AddBackslash(RuntimeRoot) + 'release') then
    RaiseException('Downloaded launcher runtime is missing release');
end;

procedure CopyDirectoryTree(const SourceDir, DestDir: String);
var
  FindRec: TFindRec;
  SourceChild: String;
  DestChild: String;
begin
  ForceDirectories(DestDir);

  if FindFirst(AddBackslash(SourceDir) + '*', FindRec) then
  begin
    try
      repeat
        if (FindRec.Name <> '.') and (FindRec.Name <> '..') then
        begin
          SourceChild := AddBackslash(SourceDir) + FindRec.Name;
          DestChild := AddBackslash(DestDir) + FindRec.Name;

          if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then
            CopyDirectoryTree(SourceChild, DestChild)
          else if not FileCopy(SourceChild, DestChild, False) then
            RaiseException('Failed to copy launcher runtime file: ' + FindRec.Name);
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;
end;

procedure PrepareLauncherRuntimeStaging;
var
  RuntimeRoot: String;
begin
  DelTree(RuntimeExtractPath(), True, True, True);
  DelTree(TempRuntimeStagingPath(), True, True, True);
  DelTree(AppRuntimeStagingPath(), True, True, True);
  ForceDirectories(RuntimeExtractPath());

  ExtractArchive(RuntimeArchivePath(), RuntimeExtractPath(), '', True, nil);
  RuntimeRoot := ResolveExtractedLauncherRuntimeRoot(RuntimeExtractPath());
  ValidateLauncherRuntimeRoot(RuntimeRoot);
  CopyDirectoryTree(RuntimeRoot, TempRuntimeStagingPath());
  CopyDirectoryTree(TempRuntimeStagingPath(), AppRuntimeStagingPath());
end;

procedure StageLauncherExecutable;
begin
  DeleteFile(LauncherStagingPath());
  if not FileCopy(LauncherDownloadPath(), LauncherStagingPath(), False) then
    RaiseException('Failed to stage launcher executable');
end;

procedure RestorePreviousRuntimeIfPresent;
begin
  if DirExists(PreviousRuntimePath()) then
  begin
    DelTree(AppRuntimePath(), True, True, True);
    if not RenameFile(PreviousRuntimePath(), AppRuntimePath()) then
      RaiseException('Failed to restore the previous launcher runtime');
  end;
end;

procedure RestorePreviousLauncherIfPresent;
begin
  if FileExists(PreviousLauncherPath()) then
  begin
    DeleteFile(LauncherInstalledPath());
    if not RenameFile(PreviousLauncherPath(), LauncherInstalledPath()) then
      RaiseException('Failed to restore the previous launcher executable');
  end;
end;

procedure PromoteLauncherRuntime;
begin
  DelTree(PreviousRuntimePath(), True, True, True);

  if DirExists(AppRuntimePath()) then
  begin
    if not RenameFile(AppRuntimePath(), PreviousRuntimePath()) then
      RaiseException('Failed to move the previous launcher runtime out of the way');
  end;

  if not RenameFile(AppRuntimeStagingPath(), AppRuntimePath()) then
  begin
    RestorePreviousRuntimeIfPresent();
    RaiseException('Failed to install the launcher runtime');
  end;
end;

procedure PromoteLauncherExecutable;
begin
  DeleteFile(PreviousLauncherPath());

  if FileExists(LauncherInstalledPath()) then
  begin
    if not RenameFile(LauncherInstalledPath(), PreviousLauncherPath()) then
      RaiseException('Failed to move the previous launcher executable out of the way');
  end;

  if not RenameFile(LauncherStagingPath(), LauncherInstalledPath()) then
  begin
    RestorePreviousLauncherIfPresent();
    RaiseException('Failed to install the launcher executable');
  end;
end;

procedure CleanupPreviousInstallBackups;
begin
  DeleteFile(PreviousLauncherPath());
  DelTree(PreviousRuntimePath(), True, True, True);
end;

procedure CleanupInstallerStaging;
begin
  DeleteFile(LauncherDownloadPath());
  DeleteFile(LauncherStagingPath());
  DeleteFile(RuntimeArchivePath());
  DelTree(RuntimeExtractPath(), True, True, True);
  DelTree(TempRuntimeStagingPath(), True, True, True);
  DelTree(AppRuntimeStagingPath(), True, True, True);
end;

procedure InstallDownloadedArtifacts;
begin
  ForceDirectories(ExpandConstant('{app}'));
  try
    DownloadInstallerArtifacts;
    PrepareLauncherRuntimeStaging;
    StageLauncherExecutable;

    try
      PromoteLauncherRuntime;
      PromoteLauncherExecutable;
      CleanupPreviousInstallBackups;
    except
      RestorePreviousLauncherIfPresent();
      RestorePreviousRuntimeIfPresent();
      RaiseException(GetExceptionMessage());
    end;
  finally
    CleanupInstallerStaging();
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then
    InstallDownloadedArtifacts;
end;

function InitializeUninstall(): Boolean;
var
  Choice: Integer;
begin
  Choice :=
    MsgBox(
      'Delete all launcher data?' + #13#10#13#10 +
      'Choose Yes to delete all launcher data under ' + ExpandConstant('{app}') + '.' + #13#10 +
      'Choose No to keep installedPacks and modpacks while deleting the rest of the launcher data.' + #13#10 +
      'Choose Cancel to abort uninstall.',
      mbConfirmation,
      MB_YESNOCANCEL
    );

  if Choice = IDCANCEL then
  begin
    Result := False;
    exit;
  end;

  DeleteAllLauncherData := Choice = IDYES;
  Result := True;
end;

procedure DeleteLauncherDataPreservingModpacks;
var
  FindRec: TFindRec;
  ChildPath: String;
begin
  if not DirExists(ExpandConstant('{app}')) then
    exit;

  if FindFirst(AddBackslash(ExpandConstant('{app}')) + '*', FindRec) then
  begin
    try
      repeat
        if (FindRec.Name <> '.') and (FindRec.Name <> '..') and
           (CompareText(FindRec.Name, 'installedPacks') <> 0) and
           (CompareText(FindRec.Name, 'modpacks') <> 0) then
        begin
          ChildPath := AddBackslash(ExpandConstant('{app}')) + FindRec.Name;

          if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then
            DelTree(ChildPath, True, True, True)
          else
            DeleteFile(ChildPath);
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
  begin
    if DeleteAllLauncherData then
      DelTree(ExpandConstant('{app}'), True, True, True)
    else
      DeleteLauncherDataPreservingModpacks();
  end;
end;
