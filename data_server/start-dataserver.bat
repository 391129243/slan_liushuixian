@echo off
setlocal
title PCR DataServer
cd /d "%~dp0"
if errorlevel 1 exit /b 1

set "DATASERVER_ADMIN_PASSWORD=12345678"
if not defined DATASERVER_MANAGED_ENABLED set "DATASERVER_MANAGED_ENABLED=true"

echo Starting PCR DataServer. Account: admin
echo Default address: http://127.0.0.1:8088
echo Press Ctrl+C to stop.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
set "dataserverExitCode=%ERRORLEVEL%"
if not "%dataserverExitCode%"=="0" echo DataServer exited with code %dataserverExitCode%. Check the output above and runtime\managed logs.
if not "%DATASERVER_NO_PAUSE%"=="1" pause
exit /b %dataserverExitCode%
