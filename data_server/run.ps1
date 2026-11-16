param([switch]$Build)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if (-not $env:DATASERVER_ADMIN_PASSWORD -or $env:DATASERVER_ADMIN_PASSWORD.Length -lt 12) {
    throw 'Set DATASERVER_ADMIN_PASSWORD (at least 12 characters) before starting.'
}
$jdk=$env:JAVA_HOME
if (-not $jdk) { $jdk=Join-Path $env:USERPROFILE '.jdks\jbr-21.0.11' }
if (-not (Test-Path -LiteralPath "$jdk\bin\java.exe")) { throw 'Set JAVA_HOME to JDK 21.' }
$env:JAVA_HOME=$jdk
$env:PATH="$jdk\bin;$env:PATH"
$jar=Join-Path $PSScriptRoot 'target\dataserver-0.1.0.jar'
if ($Build -or -not (Test-Path -LiteralPath $jar)) {
    $maven=Join-Path $PSScriptRoot 'java-vue3-preparation\tools\apache-maven-3.9.16\bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $maven)) { throw 'Run java-vue3-preparation/prepare.ps1 to prepare Maven.' }
    & $maven -B "-Dmaven.repo.local=$PSScriptRoot\java-vue3-preparation\.m2" clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
}
& "$jdk\bin\java.exe" -jar $jar
exit $LASTEXITCODE
