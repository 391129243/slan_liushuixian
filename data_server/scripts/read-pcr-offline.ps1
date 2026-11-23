param([Parameter(Mandatory=$true)][string]$Source,
      [Parameter(Mandatory=$true)][string]$OutputDirectory,
      [Parameter(Mandatory=$true)][string]$VendorDirectory)
$ErrorActionPreference='Stop'
$sourceFile=(Resolve-Path -LiteralPath $Source).Path
$vendor=(Resolve-Path -LiteralPath $VendorDirectory).Path
$output=[IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $output | Out-Null
$copy=Join-Path $output ([IO.Path]::GetFileName($sourceFile))
if($sourceFile -eq $copy){throw 'Source and private copy must be different files'}
Copy-Item -LiteralPath $sourceFile -Destination $copy -Force
Copy-Item -LiteralPath (Join-Path $vendor 'Dump.dll') -Destination (Join-Path $output 'Dump.dll') -Force
foreach($file in @('vendor-experiment.json','reader-diagnostics.log')) {
    $generated=Join-Path $output $file
    if(Test-Path -LiteralPath $generated){Remove-Item -LiteralPath $generated}
}
$compiler=Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$exe=Join-Path $output 'ReadPcrOffline.exe'
$sourceCode=Join-Path $PSScriptRoot 'dotnet-framework\ReadPcrOffline.cs'
& $compiler /nologo /target:exe /platform:x64 "/out:$exe" "/reference:$vendor\Service.dll" "/reference:$vendor\Images.dll" "/reference:$vendor\SLAN.API.dll" "/reference:$vendor\Newtonsoft.Json.dll" /reference:System.ServiceModel.dll /reference:System.Runtime.Serialization.dll "/reference:$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\WPF\WindowsBase.dll" "/reference:$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\WPF\PresentationFramework.dll" "/reference:$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\WPF\PresentationCore.dll" /reference:System.Xaml.dll $sourceCode
if($LASTEXITCODE -ne 0){throw 'Offline reader compilation failed'}
'<configuration><startup useLegacyV2RuntimeActivationPolicy="true"><supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.8" /></startup></configuration>'|Set-Content -LiteralPath "$exe.config" -Encoding UTF8
$stdout=Join-Path $output 'reader.stdout.log'
$stderr=Join-Path $output 'reader.stderr.log'
$argsText='"'+$vendor+'" "'+$copy+'" "'+(Join-Path $output 'vendor-experiment.json')+'"'
$process=Start-Process -FilePath $exe -ArgumentList $argsText -WorkingDirectory $output -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
if(-not $process.WaitForExit(60000)){Stop-Process -Id $process.Id;throw 'Offline reader timed out; inspect reader logs'}
$process.WaitForExit()
$process.Refresh()
if(-not (Test-Path -LiteralPath (Join-Path $output 'vendor-experiment.json')) -or (Get-Item -LiteralPath $stderr).Length -gt 0){Get-Content -LiteralPath $stderr -Encoding Default;throw 'Vendor offline read failed'}
Get-Content -LiteralPath $stdout -Encoding UTF8
