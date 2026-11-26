@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\reinstall-phone.ps1" %*

exit /b %ERRORLEVEL%
