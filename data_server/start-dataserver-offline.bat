@echo off
setlocal
title PCR DataServer - Offline SDK
cd /d "%~dp0"
if errorlevel 1 exit /b 1
set "DATASERVER_ADMIN_PASSWORD=12345678"
echo Starting offline SDK analysis server. Account: admin
echo Real PCR files are analyzed with the vendor SDK. No instrument connection.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-offline-server.ps1" %*
set "dataserverExitCode=%ERRORLEVEL%"
if not "%dataserverExitCode%"=="0" echo Startup failed. Check the output above and runtime\offline-server logs.
if not "%DATASERVER_NO_PAUSE%"=="1" pause
exit /b %dataserverExitCode%
