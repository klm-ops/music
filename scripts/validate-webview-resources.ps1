Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$pairs = @(
  @("web-player\index.html", "app\src\main\assets\player\index.html"),
  @("web-player\app.js", "app\src\main\assets\player\app.js"),
  @("web-player\styles.css", "app\src\main\assets\player\styles.css")
)

$requiredIds = @(
  "localModule", "localTransport", "lyricsPreview", "lyricsTools", "lyricsFullPanel",
  "bluetoothModule", "btScanBtn", "btDeviceList", "btPlayBtn",
  "radioModule", "radioScanBtn", "presetGrid",
  "favoritePanel", "playlistPanel", "fileInput",
  "usbModal", "usbToast", "usbFolderList", "usbVolume"
)

# Keep this list explicit: these are common visible artifacts from UTF-8 text
# being read/written as an ANSI/GBK code page.
$mojibakeText = @(
  [string][char]0xfffd,
  [string][char]0x9237,
  [string][char]0x9238,
  [string][char]0x95c1,
  [string][char]0x95ca,
  [string][char]0x95c2,
  [string][char]0x95b8,
  [string][char]0x93c8,
  [string][char]0x93be,
  [string][char]0x93c6,
  [string][char]0x9359,
  [string][char]0x7027,
  [string][char]0x59ab,
  [string][char]0x934b,
  "未连?",
  "?/button",
  "?/h2"
)

function Resolve-ProjectPath([string]$relativePath) {
  return Join-Path $root $relativePath
}

function Assert-Utf8([string]$relativePath) {
  $path = Resolve-ProjectPath $relativePath
  $bytes = [IO.File]::ReadAllBytes($path)
  $encoding = [Text.UTF8Encoding]::new($false, $true)
  $null = $encoding.GetString($bytes)
}

function Assert-NoMojibake([string]$relativePath) {
  $path = Resolve-ProjectPath $relativePath
  $text = [IO.File]::ReadAllText($path, [Text.UTF8Encoding]::new($false, $true))
  foreach ($pattern in $mojibakeText) {
    if ($text.Contains($pattern)) {
      throw "Mojibake marker '$pattern' found in $relativePath"
    }
  }
}

foreach ($pair in $pairs) {
  foreach ($file in $pair) {
    Assert-Utf8 $file
    Assert-NoMojibake $file
  }

  $left = Get-FileHash (Resolve-ProjectPath $pair[0]) -Algorithm SHA256
  $right = Get-FileHash (Resolve-ProjectPath $pair[1]) -Algorithm SHA256
  if ($left.Hash -ne $right.Hash) {
    throw "Asset hash mismatch: $($pair[0]) != $($pair[1])"
  }
}

$html = [IO.File]::ReadAllText((Resolve-ProjectPath "web-player\index.html"), [Text.UTF8Encoding]::new($false, $true))
foreach ($id in $requiredIds) {
  if ($html -notmatch ('id="' + [regex]::Escape($id) + '"')) {
    throw "Required DOM id missing: $id"
  }
}

Write-Host "WebView resource validation passed."
