Write-Output "=== $(Get-Date -Format 'HH:mm:ss') docker ps -a ==="
docker ps -a 2>&1 | ForEach-Object { $_.ToString() }
Write-Output "=== docker images ==="
docker images 2>&1 | ForEach-Object { $_.ToString() }
Write-Output "=== compose logs ==="
docker compose logs --tail=10 2>&1 | ForEach-Object { $_.ToString() }
