@echo off
echo ==========================================
echo   K&K Sales - Startar Server & Tunnel
echo ==========================================
echo.

:: Kontrollera Python-installation
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [FEL] Python hittades inte! Se till att Python ar installerat och i PATH.
    pause
    exit /b
)

:: Installera nodvandiga bibliotek
echo [1/3] Kontrollerar bibliotek...
pip install flask flask_sqlalchemy requests waitress python-dotenv >nul

:: Starta Python-servern i ett nytt fonster
echo [2/3] Startar Python-servern...
start "K&K Python Server" cmd /k "python server.py"

:: Vanta lite sa servern hinner starta
timeout /t 3 /nobreak >nul

:: Starta LocalTunnel
echo [3/3] Startar LocalTunnel (kksales-permanent)...
echo.
echo DIN APP ANVANDER NU: https://kksales-permanent.loca.lt
echo.
npx localtunnel --port 8080 --subdomain kksales-permanent

pause
