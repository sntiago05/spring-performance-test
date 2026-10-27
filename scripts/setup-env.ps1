<#
.SYNOPSIS
  Generates a local .env with random secrets (never committed). Windows / PowerShell counterpart of setup-env.sh.
.DESCRIPTION
  Works with Windows PowerShell 5.1 and PowerShell 7+. Use -Force to overwrite an existing .env.
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\scripts\setup-env.ps1
#>
param([switch]$Force)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

if ((Test-Path '.env') -and -not $Force) {
    Write-Host '.env already exists (use -Force to regenerate)'
    exit 0
}

function New-Secret([int]$Length) {
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $secret = ''
    while ($secret.Length -lt $Length) {
        $bytes = New-Object byte[] 48
        $rng.GetBytes($bytes)
        $secret += ([Convert]::ToBase64String($bytes) -replace '[^A-Za-z0-9]', '')
    }
    $rng.Dispose()
    return $secret.Substring(0, $Length)
}

$values = @{
    'DB_PASSWORD'                   = New-Secret 24
    'JWT_SECRET'                    = New-Secret 48
    'BOOTSTRAP_ADMIN_PASSWORD'      = (New-Secret 16) + 'Aa1'
    'BOOTSTRAP_TECHNICIAN_PASSWORD' = (New-Secret 16) + 'Aa1'
    'GRAFANA_ADMIN_PASSWORD'        = New-Secret 16
}

$content = Get-Content -Raw -Path '.env.example'
foreach ($key in $values.Keys) {
    $content = [regex]::Replace($content, "(?m)^$key=.*$", "$key=$($values[$key])")
}

# UTF-8 without BOM: a BOM would corrupt the first variable name for Docker Compose.
[System.IO.File]::WriteAllText((Join-Path (Get-Location) '.env'), $content, (New-Object System.Text.UTF8Encoding($false)))

$adminEmail = ([regex]::Match($content, '(?m)^BOOTSTRAP_ADMIN_EMAIL=(.*)$').Groups[1].Value).Trim()
Write-Host ".env created. Admin: $adminEmail / password in .env (BOOTSTRAP_ADMIN_PASSWORD)"
