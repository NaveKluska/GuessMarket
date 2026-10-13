@echo off
setlocal
cd /d "%~dp0"
title Guess Market - Web Client

where node >nul 2>nul
if errorlevel 1 (
    echo Node.js was not found on this computer. Install it from https://nodejs.org and run this file again.
    pause
    exit /b 1
)

node -e "const m = +process.versions.node.split('.')[0]; process.exit(m === 18 || m === 20 || m >= 22 ? 0 : 1)"
if errorlevel 1 (
    echo This web client needs Node.js 18, 20, or 22 and newer. Installed version:
    node --version
    pause
    exit /b 1
)

echo [1/2] Installing the web client's libraries - takes a minute on the first run, needs internet...
call npm install --no-fund --no-audit
if errorlevel 1 (
    echo npm install failed - see the messages above.
    pause
    exit /b 1
)

echo.
echo [2/2] Starting the web client on http://localhost:5173 - your browser opens it automatically.
echo The Guess Market server must be running: Tomcat with guessmarket.war deployed, on localhost:8080.
echo Keep this window open while using the client. Close it to stop the client.
echo.
call npm run dev
pause
