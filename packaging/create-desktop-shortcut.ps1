# GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
# Creates a Windows Desktop shortcut for PPMS
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$desktopDir = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopDir "PPMS - Product Price Management.lnk"
$iconPath = Join-Path $scriptDir "ppms-icon.ico"

$packagedExe = Join-Path $rootDir "dist\PPMS\PPMS.exe"
$vbsLauncher = Join-Path $scriptDir "launch-ppms.vbs"

$wsh = New-Object -ComObject WScript.Shell
$shortcut = $wsh.CreateShortcut($shortcutPath)

if (Test-Path $packagedExe) {
    $shortcut.TargetPath = $packagedExe
    $shortcut.WorkingDirectory = Split-Path -Parent $packagedExe
} else {
    $shortcut.TargetPath = "wscript.exe"
    $shortcut.Arguments = "`"$vbsLauncher`""
    $shortcut.WorkingDirectory = $rootDir
}

if (Test-Path $iconPath) {
    $shortcut.IconLocation = "$iconPath,0"
}
$shortcut.Description = "Product Price Management System (Local Business App)"
$shortcut.Save()

Write-Host "[SUCCESS] Desktop shortcut created at: $shortcutPath" -ForegroundColor Green
