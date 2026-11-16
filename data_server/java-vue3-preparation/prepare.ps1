param([string]$JdkHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
New-Item -ItemType Directory -Force -Path "$root\evidence", "$root\vendor" | Out-Null
if (-not (Test-Path -LiteralPath "$root\vendor\slansdk.jar") -or
    -not (Test-Path -LiteralPath "$root\vendor\libslansdk.so")) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    # Match the supplied archive by its ASCII date; tolerate Windows PowerShell encoding.
    $archives = @(Get-ChildItem -LiteralPath (Split-Path $root -Parent) -Filter '*20241217*.zip')
    if ($archives.Count -ne 1) { throw 'Place exactly one supplied SDK 20241217 ZIP in the parent directory.' }
    $archiveReader = [IO.Compression.ZipFile]::OpenRead($archives[0].FullName)
    try {
        foreach ($fileName in @('slansdk.jar', 'libslansdk.so')) {
            $entry = $archiveReader.GetEntry("20241217/$fileName")
            if (-not $entry) { throw "SDK archive is missing $fileName" }
            $destination = Join-Path "$root\vendor" $fileName
            if (-not (Test-Path -LiteralPath $destination)) {
                [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination)
            }
        }
    } finally { $archiveReader.Dispose() }
}
if (-not $JdkHome) {
    $candidate = Join-Path $env:USERPROFILE '.jdks\jbr-21.0.11'
    if (Test-Path -LiteralPath "$candidate\bin\javac.exe") { $JdkHome = $candidate }
}
if (-not $JdkHome -or -not (Test-Path -LiteralPath "$JdkHome\bin\javac.exe")) {
    throw 'JDK 21 required. Set JAVA_HOME or pass -JdkHome.'
}
# These settings apply to this process only; no global PATH changes.
$env:JAVA_HOME = $JdkHome
$env:PATH = "$JdkHome\bin;$env:PATH"
$version = '3.9.16'
$tools = Join-Path $root 'tools'
$mvn = Join-Path $tools "apache-maven-$version\bin\mvn.cmd"
if (-not (Test-Path -LiteralPath $mvn)) {
    New-Item -ItemType Directory -Force -Path $tools | Out-Null
    $archive = Join-Path $tools "apache-maven-$version-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing -TimeoutSec 120
    $expected = (Invoke-WebRequest -Uri "$url.sha512" -UseBasicParsing -TimeoutSec 60).Content.Trim().Split(' ')[0]
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash -ine $expected) { throw 'Maven checksum mismatch' }
    Expand-Archive -LiteralPath $archive -DestinationPath $tools -Force
}
& $mvn -B "-Dmaven.repo.local=$root\.m2" -f "$root\backend\pom.xml" dependency:copy-dependencies '-DincludeScope=runtime' "-DoutputDirectory=$root\backend\lib"
if ($LASTEXITCODE -ne 0) { throw 'Backend dependency download failed' }
& $mvn -B "-Dmaven.repo.local=$root\.m2" -f "$root\backend\pom.xml" dependency:tree "-DoutputFile=$root\evidence\backend-dependency-tree.txt"
if ($LASTEXITCODE -ne 0) { throw 'Dependency tree failed' }
Push-Location "$root\frontend"
try {
    if (Test-Path -LiteralPath 'package-lock.json') { & npm.cmd ci --ignore-scripts } else { & npm.cmd install --ignore-scripts }
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
} finally { Pop-Location }
$classes = Join-Path $root 'sdk-probe\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& "$JdkHome\bin\javac.exe" -encoding UTF-8 -d $classes "$root\sdk-probe\SdkProbe.java"
if ($LASTEXITCODE -ne 0) { throw 'SDK probe compilation failed' }
& "$JdkHome\bin\java.exe" -cp "$classes;$root\vendor\slansdk.jar" SdkProbe "$root\vendor\libslansdk.so"
if ($LASTEXITCODE -ne 0) { throw 'SDK static probe failed' }
Write-Host 'Dependencies prepared. No database installed, no native library loaded, no PCR analysis executed.'
