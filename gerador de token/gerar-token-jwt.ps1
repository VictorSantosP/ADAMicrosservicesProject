$ErrorActionPreference = 'Stop'

# SOMENTE PARA DESENVOLVIMENTO LOCAL. Troque esta chave antes de qualquer uso real.
$secret = 'dev-only-secret-change-this-key-32bytes-minimum'
$issuer = 'http://localhost:9000'

function ConvertTo-Base64Url([byte[]] $bytes) {
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$now = [DateTimeOffset]::UtcNow
$header = @{
    alg = 'HS256'
    typ = 'JWT'
} | ConvertTo-Json -Compress

$payload = @{
    iss   = $issuer
    sub   = 'dev-user'
    iat   = $now.ToUnixTimeSeconds()
    exp   = $now.AddHours(1).ToUnixTimeSeconds()
    scope = 'products:read'
} | ConvertTo-Json -Compress

$headerPart = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($header))
$payloadPart = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload))
$unsignedToken = "$headerPart.$payloadPart"

$hmac = [System.Security.Cryptography.HMACSHA256]::new(
    [Text.Encoding]::UTF8.GetBytes($secret)
)
try {
    $signature = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsignedToken))
}
finally {
    $hmac.Dispose()
}

$token = "$unsignedToken.$(ConvertTo-Base64Url $signature)"

Write-Host ''
Write-Host 'JWT HS256 gerado (validade: 1 hora; uso local de desenvolvimento):' -ForegroundColor Green
Write-Host ''
Write-Host $token
Write-Host ''
try {
    Set-Clipboard -Value $token
    Write-Host 'Token copiado para a area de transferencia.' -ForegroundColor Cyan
}
catch {
    Write-Host 'Nao foi possivel copiar automaticamente; copie o token acima.'
}
Write-Host ''
Write-Host 'Issuer (iss): http://localhost:9000'
Write-Host 'Subject (sub): dev-user'
Write-Host 'Scope: products:read'
Write-Host ''
Read-Host 'Pressione Enter para fechar'
