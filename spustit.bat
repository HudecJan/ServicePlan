@echo off
title Planovanie sluzieb lekarov
echo.
echo  ====================================
echo   Planovanie sluzieb lekarov
echo  ====================================
echo.

:: Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo  CHYBA: Java nie je nainstalovana!
    echo  Stiahnite ju z: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: Find the JAR file
set JAR_FILE=
for %%f in (target\servicePlan*.jar) do set JAR_FILE=%%f

if "%JAR_FILE%"=="" (
    echo  JAR subor nenajdeny. Buildujem...
    call mvnw.cmd package -DskipTests -q
    for %%f in (target\servicePlan*.jar) do set JAR_FILE=%%f
)

if "%JAR_FILE%"=="" (
    echo  CHYBA: Nepodarilo sa vytvorit JAR subor.
    pause
    exit /b 1
)

echo  Startujem aplikaciu...
echo  Po spusteni otvorte prehliadac na: http://localhost:8081
echo  Pre ukoncenie stlacte Ctrl+C
echo.

:: Open browser after short delay
start "" cmd /c "timeout /t 4 /nobreak >nul && start http://localhost:8081"

:: Run the application
java -jar %JAR_FILE%
