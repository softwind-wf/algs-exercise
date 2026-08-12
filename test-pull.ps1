Write-Output "=== START docker pull mysql:8.0 at $(Get-Date -Format 'HH:mm:ss') ==="
docker pull mysql:8.0 2>&1 | ForEach-Object { $_.ToString() }
Write-Output "=== EXIT=$LASTEXITCODE at $(Get-Date -Format 'HH:mm:ss') ==="
docker images 2>&1 | ForEach-Object { $_.ToString() }
