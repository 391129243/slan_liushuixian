param([switch]$Build)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $root
foreach($port in @(8088,5099)){
    $client=[Net.Sockets.TcpClient]::new()
    $inUse=$false
    try{$client.Connect('127.0.0.1',$port);$inUse=$true}catch{}finally{$client.Dispose()}
    if($inUse){throw "Port $port is already in use. Existing services were not stopped. Use the running server or close it first."}
}
if(-not $env:DATASERVER_ADMIN_PASSWORD){$env:DATASERVER_ADMIN_PASSWORD='12345678'}
$jdk=$env:JAVA_HOME
if(-not $jdk){$jdk=Join-Path $env:USERPROFILE '.jdks\jbr-21.0.11'}
if(-not(Test-Path -LiteralPath (Join-Path $jdk 'bin\java.exe'))){throw 'Set JAVA_HOME to JDK 21.'}
$env:JAVA_HOME=$jdk
if($Build -or -not(Test-Path -LiteralPath 'target\dataserver-0.1.0.jar')){
    & (Join-Path $root 'java-vue3-preparation\tools\apache-maven-3.9.16\bin\mvn.cmd') "-Dmaven.repo.local=$root\java-vue3-preparation\.m2" verify
    if($LASTEXITCODE -ne 0){throw 'Java build failed.'}
}
if($Build -or -not(Test-Path -LiteralPath 'target\csharp-bridge\PcrSdkBridge.dll')){
    & dotnet build (Join-Path $root 'csharp\PcrSdkBridge') -o (Join-Path $root 'target\csharp-bridge')
    if($LASTEXITCODE -ne 0){throw 'C# build failed.'}
}
$package=Get-ChildItem -LiteralPath $root -Directory -Filter 'SLAN_9.0.1.260921_API_*'
if(@($package).Count -ne 1){throw 'Expected exactly one supplied SLAN SDK package.'}
$exe=Get-ChildItem -LiteralPath $package.FullName -File -Recurse -Filter 'SLAN.WCFServer.exe'
if(@($exe).Count -ne 1){throw 'Expected exactly one vendor distribution.'}
$sample=Get-ChildItem -LiteralPath (Join-Path $root 'result') -File -Recurse -Filter '2026-10-29-17-15-07.pcr'
if(@($sample).Count -ne 1){throw 'Singleplex reference PCR file is missing.'}
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'read-pcr-offline.ps1') -Source $sample.FullName -OutputDirectory (Join-Path $root 'target\real-sdk-reanalysis-20261123') -VendorDirectory $exe.DirectoryName
if($LASTEXITCODE -ne 0){throw 'Offline SDK initialization failed.'}
& node (Join-Path $PSScriptRoot 'simulate-real-sdk-reanalysis.mjs') --serve
exit $LASTEXITCODE
