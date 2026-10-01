$ErrorActionPreference = 'Stop'
$jdk = 'C:\Program Files\Java\jdk-1.8'
Write-Output ("JAVA_HOME candidate exists: " + (Test-Path -LiteralPath $jdk))
Write-Output ("javac exists: " + (Test-Path -LiteralPath (Join-Path $jdk 'bin\javac.exe')))
$env:JAVA_HOME = $jdk
& (Join-Path $jdk 'bin\javac.exe') -version
& (Join-Path $jdk 'bin\java.exe') -version
Write-Output ("maven repo exists: " + (Test-Path -LiteralPath 'D:\maven-repo'))
