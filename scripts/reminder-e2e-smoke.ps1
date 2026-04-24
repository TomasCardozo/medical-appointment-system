Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$gateway = "http://localhost:18080"
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$doctorEmail = "doctor.r7.$ts@example.com"
$patientEmail = "patient.r7.$ts@example.com"
$password = "Password123!"

Invoke-RestMethod -Method Post -Uri "$gateway/auth/register/doctor" -ContentType "application/json" -Body (@{
    fullName = "Doctor Reminder"
    email = $doctorEmail
    password = $password
} | ConvertTo-Json) | Out-Null

Invoke-RestMethod -Method Post -Uri "$gateway/auth/register/patient" -ContentType "application/json" -Body (@{
    fullName = "Patient Reminder"
    email = $patientEmail
    password = $password
} | ConvertTo-Json) | Out-Null

$doctorLogin = Invoke-RestMethod -Method Post -Uri "$gateway/auth/login" -ContentType "application/json" -Body (@{
    email = $doctorEmail
    password = $password
} | ConvertTo-Json)

$patientLogin = Invoke-RestMethod -Method Post -Uri "$gateway/auth/login" -ContentType "application/json" -Body (@{
    email = $patientEmail
    password = $password
} | ConvertTo-Json)

$doctorHeaders = @{ Authorization = "Bearer $($doctorLogin.accessToken)" }
$patientHeaders = @{ Authorization = "Bearer $($patientLogin.accessToken)" }

$doctorProfile = Invoke-RestMethod -Method Post -Uri "$gateway/doctors/profile" -Headers $doctorHeaders -ContentType "application/json" -Body (@{
    fullName = "Dr. Reminder Test"
    specialty = "Cardiology"
    licenseNumber = "REM-$ts"
    clinicAddress = "Reminder Clinic 123"
    bio = "Smoke reminder"
} | ConvertTo-Json)

$doctorId = [int64]$doctorProfile.id
$targetDate = (Get-Date).Date.AddDays(1)
$dateStr = $targetDate.ToString("yyyy-MM-dd")
$dayName = $targetDate.DayOfWeek.ToString().ToUpperInvariant()

Invoke-RestMethod -Method Post -Uri "$gateway/doctors/$doctorId/availability" -Headers $doctorHeaders -ContentType "application/json" -Body (@{
    dayOfWeek = $dayName
    startTime = "08:00:00"
    endTime = "20:00:00"
    slotDurationMinutes = 30
} | ConvertTo-Json) | Out-Null

$availableSlots = Invoke-RestMethod -Method Get -Uri "$gateway/appointments/available?doctorId=$doctorId&date=$dateStr" -Headers $patientHeaders
if ($availableSlots.Count -lt 1) {
    throw "No available slots for reminder smoke"
}

$selectedSlot = $availableSlots[0]
$created = Invoke-RestMethod -Method Post -Uri "$gateway/appointments" -Headers $patientHeaders -ContentType "application/json" -Body (@{
    doctorId = $doctorId
    appointmentDate = $dateStr
    startTime = $selectedSlot.startTime
} | ConvertTo-Json)

$appointmentId = [int64]$created.id
$found = $null

for ($i = 1; $i -le 8; $i++) {
    Start-Sleep -Seconds 20
    $notifications = Invoke-RestMethod -Method Get -Uri "$gateway/notifications?appointmentId=$appointmentId&eventType=appointment.reminder.requested&status=SENT" -Headers $patientHeaders
    if ($notifications -and $notifications.Count -ge 1) {
        $found = $notifications
        break
    }
}

$result = [PSCustomObject]@{
    doctorId = $doctorId
    appointmentId = $appointmentId
    appointmentDate = $dateStr
    slotStart = $selectedSlot.startTime
    reminderFound = ($null -ne $found)
    reminderCount = if ($found) { $found.Count } else { 0 }
    eventType = if ($found) { $found[0].eventType } else { $null }
}

$result | ConvertTo-Json -Depth 5
