' GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
' Silent Windows launcher for PPMS (prevents terminal window flash for business owners)
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
rootDir = fso.GetParentFolderName(scriptDir)
shell.CurrentDirectory = rootDir

packagedExe = rootDir & "\dist\PPMS\PPMS.exe"
If fso.FileExists(packagedExe) Then
    shell.Run """" & packagedExe & """", 0, False
    WScript.Quit 0
End If

binMain = rootDir & "\bin\com\ppms\app\Main.class"
If Not fso.FileExists(binMain) Then
    shell.Run "cmd /c """ & rootDir & "\compile.bat""", 1, True
End If

shell.Run "javaw -cp ""bin;lib/*"" com.ppms.app.Main", 0, False
