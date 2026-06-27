# scripts/setup-signing.ps1
#
# One-command signing setup for Auto Unstack (Windows PowerShell edition).
#
# Generates a release keystore and stores all four signing secrets directly
# in the GitHub repository using the GitHub CLI (gh).  After this runs once,
# every Release APK workflow invocation will produce a properly signed APK.
#
# Requirements:
#   - gh CLI installed and authenticated  (https://cli.github.com)
#   - keytool available (included with any JDK; Android Studio ships one)
#
# Usage:
#   .\scripts\setup-signing.ps1 [owner/repo]
#
# owner/repo defaults to the repository detected from the current directory.

param(
    [string]$Repo = ""
)

$ErrorActionPreference = "Stop"
$Alias         = "auto-unstack"
$KeystoreFile  = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), "auto-unstack-$(New-Guid).jks")

try {
    # -----------------------------------------------------------------------
    # Resolve repository
    # -----------------------------------------------------------------------
    if (-not $Repo) {
        $Repo = (gh repo view --json nameWithOwner -q ".nameWithOwner" 2>$null)
        if (-not $Repo) {
            Write-Error "Could not detect repository. Run from the repo directory or pass owner/repo."
            exit 1
        }
    }

    # -----------------------------------------------------------------------
    # Check dependencies
    # -----------------------------------------------------------------------
    foreach ($cmd in @("keytool", "gh")) {
        if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
            Write-Error "'$cmd' is required but not found in PATH."
            exit 1
        }
    }

    # -----------------------------------------------------------------------
    # Generate secure random passwords (32 alphanumeric characters)
    # -----------------------------------------------------------------------
    $chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    $rng   = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    function New-RandomPassword([int]$Length) {
        $bytes = New-Object byte[] $Length
        $rng.GetBytes($bytes)
        -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
    }

    $KeystorePassword = New-RandomPassword 32
    $KeyPassword      = $KeystorePassword

    # -----------------------------------------------------------------------
    # Generate keystore
    # -----------------------------------------------------------------------
    Write-Host "Generating keystore..."
    $keytoolArgs = @(
        "-genkeypair",
        "-storetype", "PKCS12",
        "-keystore",  $KeystoreFile,
        "-keyalg",    "RSA",
        "-keysize",   "2048",
        "-validity",  "36500",
        "-alias",     $Alias,
        "-storepass", $KeystorePassword,
        "-keypass",   $KeyPassword,
        "-dname",     "CN=Auto Unstack, O=Auto Unstack, C=US",
        "-noprompt"
    )
    & keytool @keytoolArgs 2>$null
    if ($LASTEXITCODE -ne 0) { throw "keytool exited with code $LASTEXITCODE" }

    # -----------------------------------------------------------------------
    # Push all four secrets to GitHub
    # -----------------------------------------------------------------------
    Write-Host "Uploading signing secrets to $Repo..."

    $keystoreBase64 = [System.Convert]::ToBase64String(
        [System.IO.File]::ReadAllBytes($KeystoreFile)
    )
    $keystoreBase64   | gh secret set KEYSTORE_BASE64 --repo $Repo
    $KeystorePassword | gh secret set KEYSTORE_PASSWORD --repo $Repo
    $Alias            | gh secret set KEY_ALIAS --repo $Repo
    $KeyPassword      | gh secret set KEY_PASSWORD --repo $Repo

    # -----------------------------------------------------------------------
    # Done
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "✓ Signing secrets configured for $Repo."
    Write-Host "  Future releases will be automatically signed with your release keystore."
    Write-Host ""
    Write-Host "  NOTE: The keystore is stored only in GitHub Secrets."
    Write-Host "  To obtain a local copy for backup, re-run this script and copy"
    Write-Host "  the keystore file before it is deleted."

} finally {
    if (Test-Path $KeystoreFile) { Remove-Item $KeystoreFile -Force }
    if ($rng) { $rng.Dispose() }
}
