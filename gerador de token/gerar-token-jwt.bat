@echo off
setlocal
title Gerador de JWT HS256 - Desenvolvimento
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0gerar-token-jwt.ps1"
if errorlevel 1 (
    echo.
    echo Falha ao gerar o token. Verifique se o PowerShell esta disponivel.
    pause
)
endlocal
