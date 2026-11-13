param(
    [Parameter(Mandatory=$true)][string]$InputApk,
    [Parameter(Mandatory=$true)][string]$OutputApk,
    [Parameter(Mandatory=$true)][string]$SigningConfig,
    [Parameter(Mandatory=$true)][string]$ApkSigner
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Security
$protected = [System.IO.File]::ReadAllBytes($SigningConfig)
$bytes = [System.Security.Cryptography.ProtectedData]::Unprotect($protected, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
$config = [System.Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json
$env:SVD_RELEASE_STORE_PASSWORD = $config.storePassword
$env:SVD_RELEASE_KEY_PASSWORD = $config.keyPassword
try {
    & $ApkSigner sign --ks $config.storeFile --ks-key-alias $config.keyAlias --ks-pass env:SVD_RELEASE_STORE_PASSWORD --key-pass env:SVD_RELEASE_KEY_PASSWORD --out $OutputApk $InputApk
    if ($LASTEXITCODE -ne 0) { throw 'Release signing failed' }
    & $ApkSigner verify --verbose $OutputApk
    if ($LASTEXITCODE -ne 0) { throw 'Release signature verification failed' }
} finally {
    Remove-Item Env:SVD_RELEASE_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SVD_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
    [Array]::Clear($bytes, 0, $bytes.Length)
}
