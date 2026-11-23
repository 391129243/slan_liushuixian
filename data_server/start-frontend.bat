@echo off
setlocal
title PCR Studio UI
cd /d "%~dp0frontend"
if errorlevel 1 exit /b 1
if not exist node_modules (
    call npm.cmd ci --ignore-scripts
    if errorlevel 1 goto failed
)
echo Open http://127.0.0.1:5173/?mode=server after Vite is ready.
echo If port 5173 is already occupied by this UI, use the existing page.
call npm.cmd run dev -- --strictPort
set "frontendExitCode=%ERRORLEVEL%"
goto done
:failed
set "frontendExitCode=1"
echo Frontend dependency installation failed.
:done
if not "%DATASERVER_NO_PAUSE%"=="1" pause
exit /b %frontendExitCode%
