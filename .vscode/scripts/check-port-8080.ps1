$ErrorActionPreference = "Stop"

$listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -eq 0) {
    Write-Output "Port 8080 is free. fw-todo can start."
    exit 0
}

$owners = @(
    $listeners |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            Get-CimInstance Win32_Process -Filter ("ProcessId = " + $_) -ErrorAction SilentlyContinue
        }
)

$ownerNames = @($owners | ForEach-Object { "{0} (PID {1})" -f $_.Name, $_.ProcessId }) -join ", "
$statusCode = $null
$request = [System.Net.HttpWebRequest]::Create("http://localhost:8080/api/todos")
$request.Method = "GET"
$request.Timeout = 3000
$request.ReadWriteTimeout = 3000

try {
    $response = $request.GetResponse()
    $statusCode = [int]$response.StatusCode
    $response.Close()
}
catch [System.Net.WebException] {
    if ($_.Exception.Response) {
        $statusCode = [int]$_.Exception.Response.StatusCode
        $_.Exception.Response.Close()
    }
    else {
        Write-Error ("Port 8080 is owned by {0}, but its health check failed: {1}" -f $ownerNames, $_.Exception.Message)
        exit 1
    }
}

if ($statusCode -eq 200) {
    Write-Error ("fw-todo is already responding on port 8080 ({0}). Use the existing app; stop it with Shift+F5 before restarting." -f $ownerNames)
    exit 1
}

Write-Error ("Port 8080 is already in use by {0} (HTTP {1}). No process was stopped and fw-todo was not started." -f $ownerNames, $statusCode)
exit 1
