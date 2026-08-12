$out = @()
$out += '--- winget ---'
$out += (Get-Command winget -ErrorAction SilentlyContinue).Source
$out += '--- wsl ---'
$out += (Get-Command wsl -ErrorAction SilentlyContinue).Source
$out += '--- virtualization enabled ---'
$out += (Get-CimInstance Win32_Processor).VirtualizationFirmwareEnabled
$out += '--- os build ---'
$out += (Get-CimInstance Win32_OperatingSystem).BuildNumber
$out += '--- docker cli ---'
$out += (Get-Command docker -ErrorAction SilentlyContinue).Source
$out += '--- docker desktop exe ---'
$out += Test-Path 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
$out += '--- hyperv feature ---'
try {
    $vm = Get-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -ErrorAction Stop
    $out += ('VirtualMachinePlatform: ' + $vm.State)
    $wslf = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -ErrorAction Stop
    $out += ('WSL: ' + $wslf.State)
} catch {
    $out += ('Feature query failed (need admin?): ' + $_.Exception.Message)
}
$out += '--- wsl status ---'
$wslStatus = wsl --status 2>&1 | Out-String
$out += $wslStatus
$out += '--- wsl list ---'
$wslList = wsl -l -v 2>&1 | Out-String
$out += $wslList
$out | Out-File D:\downloads\algs4-master\algs4-master\docker-env.txt -Encoding utf8
Write-Output SAVED
