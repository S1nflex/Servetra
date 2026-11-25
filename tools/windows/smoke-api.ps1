param(
    [string]$BaseUrl = "http://localhost:8080/servetra-server/api/v1"
)

$ErrorActionPreference = "Stop"
$suffix = [Guid]::NewGuid().ToString("N").Substring(0, 8)
$equipment = $null
$specialist = $null
$request = $null
$record = $null

function Invoke-Json([string]$Method, [string]$Path, $Body = $null) {
    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = @{ Accept = "application/json" }
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json; charset=UTF-8"
        $parameters.Body = $Body | ConvertTo-Json -Depth 8
    }
    Invoke-RestMethod @parameters
}

try {
    Write-Host "[1/8] Health"
    $health = Invoke-Json "GET" "/health"
    if ($health.status -ne "UP" -or $health.database -ne "UP") { throw "Health check is not UP" }

    Write-Host "[2/8] Equipment CRUD"
    $equipment = Invoke-Json "POST" "/equipment" @{
        inventoryNumber = "SMOKE-$suffix"; name = "Smoke test device"; category = "LAB"
        location = "Smoke lab"; criticalityScore = 90; active = $true
    }
    $equipmentRead = Invoke-Json "GET" "/equipment/$($equipment.id)"
    if ($equipmentRead.inventoryNumber -ne "SMOKE-$suffix") { throw "Equipment read did not return the created item" }
    $equipment = Invoke-Json "PUT" "/equipment/$($equipment.id)" @{
        inventoryNumber = $equipment.inventoryNumber; name = "Updated smoke device"; category = "LAB"
        location = "Smoke lab"; criticalityScore = 90; active = $true
    }
    if ($equipment.name -ne "Updated smoke device") { throw "Equipment update failed" }

    Write-Host "[3/8] Specialist CRUD"
    $specialist = Invoke-Json "POST" "/specialists" @{
        fullName = "Smoke Specialist"; email = "smoke-$suffix@example.edu"
        specialization = "Diagnostics"; active = $true
    }
    $specialistRead = Invoke-Json "GET" "/specialists/$($specialist.id)"
    if ($specialistRead.email -ne "smoke-$suffix@example.edu") { throw "Specialist read failed" }
    $specialist = Invoke-Json "PUT" "/specialists/$($specialist.id)" @{
        fullName = "Updated Smoke Specialist"; email = $specialist.email
        specialization = "Diagnostics and repair"; active = $true
    }
    if ($specialist.fullName -ne "Updated Smoke Specialist") { throw "Specialist update failed" }

    Write-Host "[4/8] Formula"
    $request = Invoke-Json "POST" "/requests" @{
        equipmentId = $equipment.id; specialistId = $specialist.id; summary = "Smoke request"
        severityScore = 80; downtimeImpactScore = 60
    }
    if ([decimal]$request.priorityIndex -ne [decimal]79.00) { throw "Expected priority 79.00, got $($request.priorityIndex)" }
    $requestRead = Invoke-Json "GET" "/requests/$($request.id)"
    if ($requestRead.status -ne "NEW") { throw "Request read failed" }
    $request = Invoke-Json "PUT" "/requests/$($request.id)" @{
        equipmentId = $equipment.id; specialistId = $specialist.id; summary = "Updated smoke request"
        description = "Updated through HTTP PUT"; severityScore = 80; downtimeImpactScore = 60
    }
    if ($request.summary -ne "Updated smoke request") { throw "Request update failed" }

    Write-Host "[5/8] Invalid lifecycle transition"
    try {
        Invoke-Json "POST" "/requests/$($request.id)/transitions" @{ toStatus = "COMPLETED" }
        throw "NEW -> COMPLETED unexpectedly succeeded"
    } catch {
        $response = $_.Exception.Response
        $statusCode = if ($null -eq $response) { 0 } else { [int]$response.StatusCode }
        if ($statusCode -ne 409) { throw }
    }

    Write-Host "[6/8] Valid lifecycle"
    foreach ($status in @("DIAGNOSED", "IN_SERVICE", "COMPLETED")) {
        $request = Invoke-Json "POST" "/requests/$($request.id)/transitions" @{ toStatus = $status }
    }
    if ($request.status -ne "COMPLETED") { throw "Lifecycle did not reach COMPLETED" }

    Write-Host "[7/8] ServiceRecord CRUD"
    $record = Invoke-Json "POST" "/service-records" @{
        requestId = $request.id; specialistId = $specialist.id; workType = "DIAGNOSTICS"
        description = "Smoke diagnostics"; durationMinutes = 15; costAmount = 0
    }
    $recordRead = Invoke-Json "GET" "/service-records/$($record.id)"
    if ($recordRead.workType -ne "DIAGNOSTICS") { throw "Service record read failed" }
    $record = Invoke-Json "PUT" "/service-records/$($record.id)" @{
        requestId = $request.id; specialistId = $specialist.id; workType = "REPAIR"
        description = "Updated smoke repair"; performedAt = $record.performedAt
        durationMinutes = 25; costAmount = 12.50
    }
    if ($record.workType -ne "REPAIR" -or [int]$record.durationMinutes -ne 25) { throw "Service record update failed" }
    $records = Invoke-Json "GET" "/service-records?requestId=$($request.id)"
    if (($records | Measure-Object).Count -lt 1) { throw "Service record was not returned" }

    Write-Host "[8/8] PASS - full HTTP/JSON/JDBC/MySQL smoke scenario completed" -ForegroundColor Green
} finally {
    if ($record) { try { Invoke-Json "DELETE" "/service-records/$($record.id)" | Out-Null } catch {} }
    if ($request) { try { Invoke-Json "DELETE" "/requests/$($request.id)" | Out-Null } catch {} }
    if ($specialist) { try { Invoke-Json "DELETE" "/specialists/$($specialist.id)" | Out-Null } catch {} }
    if ($equipment) { try { Invoke-Json "DELETE" "/equipment/$($equipment.id)" | Out-Null } catch {} }
}
