$ErrorActionPreference = "Stop"
$workspace = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
$classes = Join-Path $workspace "target\classes"

function Get-Listeners {
    @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
}

$listeners = @(Get-Listeners)
if ($listeners.Count -eq 0) {
    Write-Output "Port 8080 is free. fw-todo can start."
    exit 0
}

# Only the app service in this Compose project is eligible for a graceful stop.
$stoppedKnownApp = $false
$appContainer = @(docker compose --project-directory $workspace ps --status running -q app 2>$null)
if ($LASTEXITCODE -eq 0 -and $appContainer.Count -gt 0) {
    Write-Output "Stopping this project's fw-app container; fw-mysql remains running."
    docker compose --project-directory $workspace stop app
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Could not stop fw-app. Port 8080 was not cleared."
        exit 1
    }
    $stoppedKnownApp = $true
}

$listeners = @(Get-Listeners)
foreach ($ownerId in @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)) {
    $owner = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerId" -ErrorAction SilentlyContinue
    if ($null -eq $owner) { continue }

    $arguments = $owner.CommandLine
    if ($arguments -match '@(?:"([^"]+\.argfile)"|([^\s"]+\.argfile))') {
        $argumentFile = if ($Matches[1]) { $Matches[1] } else { $Matches[2] }
        if (Test-Path -LiteralPath $argumentFile) {
            $arguments += " " + (Get-Content -LiteralPath $argumentFile -Raw)
        }
    }

    $isThisApp = $owner.Name -in @("java.exe", "javaw.exe") -and
        $owner.CommandLine -match 'com\.example\.todoapp\.TodoappApplication(?:\s|$)' -and
        ($arguments -match [regex]::Escape($classes) -or
         $arguments -match [regex]::Escape($classes.Replace('\', '\\')))
    if ($isThisApp) {
        Write-Output "Stopping the previous fw-todo Java process (PID $ownerId)."
        Stop-Process -Id $ownerId -ErrorAction Stop
        $stoppedKnownApp = $true
    }
}

if ($stoppedKnownApp) {
    $attempts = 20
} else {
    $attempts = 1
}
for ($attempt = 0; $attempt -lt $attempts; $attempt++) {
    $listeners = @(Get-Listeners)
    if ($listeners.Count -eq 0) {
        Write-Output "Port 8080 is free. fw-todo can start."
        exit 0
    }
    if ($stoppedKnownApp) { Start-Sleep -Milliseconds 500 }
}

$owners = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $_" -ErrorAction SilentlyContinue
    if ($process) { "{0} (PID {1})" -f $process.Name, $_ } else { "PID $_" }
}) -join ", "
Write-Error "Port 8080 is still in use by $owners. Check the owner and stop it manually if safe. No unknown process was stopped."
exit 1
