param(
    [int]$Port = 8081
)
# Launch a detached test instance of the university web app on the given port.
# - Avoids PowerShell 5.1 Start-Process "Path/PATH duplicate key" bug
#   (the Codex execution sandbox injects both Path and PATH into the env).
# - Uses Win32_Process.Create so the child does NOT inherit the caller's
#   output pipes (which previously made the shell appear to hang).
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$cpFile = Join-Path $root 'target\cp.txt'
if (-not (Test-Path $cpFile)) {
    & mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath "-Dmdep.outputFile=target\cp.txt"
    if ($LASTEXITCODE -ne 0) { throw 'mvn build-classpath failed' }
}
$cp = (Get-Content $cpFile -Raw).Trim()
$classes = Join-Path $root 'target\classes'
$outLog = Join-Path $root ('target\app' + $Port + '.out.log')
$errLog = Join-Path $root ('target\app' + $Port + '.err.log')
$pidFile = Join-Path $root ('target\app' + $Port + '.pid')

$javaCmd = 'java -cp "' + $classes + ';' + $cp + '" com.ds.university.UniversityApplication --server.port=' + $Port
$commandLine = 'cmd /c ' + $javaCmd + ' > "' + $outLog + '" 2> "' + $errLog + '"'

$result = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    CommandLine = $commandLine
    CurrentDirectory = $root
}
if ($result.ReturnValue -ne 0) {
    throw ('Win32_Process.Create failed, return=' + $result.ReturnValue)
}
$result.ProcessId | Set-Content $pidFile
Write-Output ('started pid=' + $result.ProcessId + ' port=' + $Port)
Write-Output ('stdout log: ' + $outLog)
Write-Output ('stderr log: ' + $errLog)