param(
    [switch]$Build,
    [switch]$ResetVolume,
    [string]$OutputFile = "e2e-result.json",
    [int]$ConfigPort = 18888,
    [int]$DiscoveryPort = 18761,
    [int]$GatewayPort = 18080,
    [int]$AuthPort = 18081,
    [int]$DoctorPort = 18082,
    [int]$AppointmentPort = 18083
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Wait-Http200 {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$Retries = 60,
        [int]$SleepSeconds = 2
    )

    for ($i = 1; $i -le $Retries; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($resp.StatusCode -eq 200) {
                Write-Host "READY $Name -> $Url"
                return
            }
        }
        catch {
        }

        Start-Sleep -Seconds $SleepSeconds
    }

    throw "Timeout waiting for $Name at $Url"
}

function Wait-HttpStatus {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][int[]]$AllowedStatusCodes,
        [int]$Retries = 60,
        [int]$SleepSeconds = 2
    )

    for ($i = 1; $i -le $Retries; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($AllowedStatusCodes -contains [int]$resp.StatusCode) {
                Write-Host "READY $Name -> $Url (HTTP $($resp.StatusCode))"
                return
            }
        }
        catch {
            $statusCode = $null
            if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }

            if ($null -ne $statusCode -and ($AllowedStatusCodes -contains $statusCode)) {
                Write-Host "READY $Name -> $Url (HTTP $statusCode)"
                return
            }
        }

        Start-Sleep -Seconds $SleepSeconds
    }

    throw "Timeout waiting for $Name at $Url. Allowed status codes: $($AllowedStatusCodes -join ', ')"
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI is not available in PATH."
}

$projectRoot = $PSScriptRoot
$gatewayBase = "http://localhost:$GatewayPort"
$outputPath = Join-Path $projectRoot $OutputFile

$env:CONFIG_SERVER_PORT = "$ConfigPort"
$env:DISCOVERY_SERVER_PORT = "$DiscoveryPort"
$env:API_GATEWAY_PORT = "$GatewayPort"
$env:AUTH_SERVICE_PORT = "$AuthPort"
$env:DOCTOR_SERVICE_PORT = "$DoctorPort"
$env:APPOINTMENT_SERVICE_PORT = "$AppointmentPort"

Push-Location $projectRoot
try {
    if ($ResetVolume) {
        Write-Host "Stopping stack and removing volumes..."
        docker compose down -v
    }

    if ($Build) {
        Write-Host "Starting stack with rebuild..."
        docker compose up -d --build
    }
    else {
        Write-Host "Starting stack..."
        docker compose up -d
    }

    Wait-Http200 -Name "config-server" -Url "http://localhost:$ConfigPort/discovery-server/default"
    Wait-Http200 -Name "discovery-server" -Url "http://localhost:$DiscoveryPort"
    Wait-Http200 -Name "gateway-health" -Url "$gatewayBase/actuator/health"
    Wait-HttpStatus -Name "gateway-auth-route" -Url "$gatewayBase/auth/me" -AllowedStatusCodes @(401, 403)

    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $doctorEmail = "doctor.e2e.$ts@example.com"
    $patientEmail = "patient.e2e.$ts@example.com"
    $password = "Password123!"

    $registerDoctorBody = @{
        fullName = "Doctor E2E"
        email    = $doctorEmail
        password = $password
    } | ConvertTo-Json

    $registerPatientBody = @{
        fullName = "Patient E2E"
        email    = $patientEmail
        password = $password
    } | ConvertTo-Json

    Invoke-RestMethod -Method Post -Uri "$gatewayBase/auth/register/doctor" -ContentType "application/json" -Body $registerDoctorBody | Out-Null
    Invoke-RestMethod -Method Post -Uri "$gatewayBase/auth/register/patient" -ContentType "application/json" -Body $registerPatientBody | Out-Null

    $doctorLogin = Invoke-RestMethod -Method Post -Uri "$gatewayBase/auth/login" -ContentType "application/json" -Body (@{ email = $doctorEmail; password = $password } | ConvertTo-Json)
    $patientLogin = Invoke-RestMethod -Method Post -Uri "$gatewayBase/auth/login" -ContentType "application/json" -Body (@{ email = $patientEmail; password = $password } | ConvertTo-Json)

    $doctorHeaders = @{ Authorization = "Bearer $($doctorLogin.accessToken)" }
    $patientHeaders = @{ Authorization = "Bearer $($patientLogin.accessToken)" }

    $doctorProfile = Invoke-RestMethod -Method Post -Uri "$gatewayBase/doctors/profile" -Headers $doctorHeaders -ContentType "application/json" -Body (@{
            fullName      = "Dr. E2E Test"
            specialty     = "Cardiology"
            licenseNumber = "LIC-$ts"
            clinicAddress = "Clinic E2E 123"
            bio           = "Profile for e2e flow"
        } | ConvertTo-Json)

    $doctorId = [int64]$doctorProfile.id

    $targetDate = $null
    for ($i = 1; $i -le 14; $i++) {
        $candidate = (Get-Date).Date.AddDays($i)
        if ($candidate.DayOfWeek -eq [System.DayOfWeek]::Monday) {
            $targetDate = $candidate
            break
        }
    }

    if ($null -eq $targetDate) {
        throw "Could not find target MONDAY date."
    }

    $dateStr = $targetDate.ToString("yyyy-MM-dd")

    $availability = Invoke-RestMethod -Method Post -Uri "$gatewayBase/doctors/$doctorId/availability" -Headers $doctorHeaders -ContentType "application/json" -Body (@{
            dayOfWeek           = "MONDAY"
            startTime           = "09:00:00"
            endTime             = "12:00:00"
            slotDurationMinutes = 30
        } | ConvertTo-Json)

    $availableSlots = Invoke-RestMethod -Method Get -Uri "$gatewayBase/appointments/available?doctorId=$doctorId&date=$dateStr" -Headers $patientHeaders
    if ($availableSlots.Count -lt 1) {
        throw "No available slots returned for doctorId=$doctorId on $dateStr"
    }

    $selectedSlot = $availableSlots[0]

    $createdAppointment = Invoke-RestMethod -Method Post -Uri "$gatewayBase/appointments" -Headers $patientHeaders -ContentType "application/json" -Body (@{
            doctorId        = $doctorId
            appointmentDate = $dateStr
            startTime       = $selectedSlot.startTime
        } | ConvertTo-Json)

    $appointmentId = [int64]$createdAppointment.id

    $cancelledAppointment = Invoke-RestMethod -Method Put -Uri "$gatewayBase/appointments/$appointmentId/cancel" -Headers $patientHeaders -ContentType "application/json" -Body (@{
            cancellationReason = "E2E cancel test"
        } | ConvertTo-Json)

    $availableAfterCancel = Invoke-RestMethod -Method Get -Uri "$gatewayBase/appointments/available?doctorId=$doctorId&date=$dateStr" -Headers $patientHeaders

    $slotRestored = $false
    foreach ($slot in $availableAfterCancel) {
        if ($slot.startTime -eq $selectedSlot.startTime) {
            $slotRestored = $true
            break
        }
    }

    $result = [PSCustomObject]@{
        gatewayBase              = $gatewayBase
        doctorId                 = $doctorId
        availabilityId           = $availability.id
        targetDate               = $dateStr
        initialAvailableSlots    = $availableSlots.Count
        selectedStartTime        = $selectedSlot.startTime
        appointmentId            = $appointmentId
        createStatus             = $createdAppointment.status
        cancelStatus             = $cancelledAppointment.status
        availableSlotsAfterCancel = $availableAfterCancel.Count
        slotRestoredAfterCancel  = $slotRestored
    }

    $resultJson = $result | ConvertTo-Json -Depth 5
    Set-Content -Path $outputPath -Value $resultJson -Encoding utf8
    Write-Host "E2E result saved to $outputPath"
    $resultJson
}
finally {
    Pop-Location
}
