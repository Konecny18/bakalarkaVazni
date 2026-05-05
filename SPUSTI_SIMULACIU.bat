@echo off
echo Startujem simulaciu cez Maven Wrapper...
echo Aktualny priecinok: %cd%

:: Skusime spustit cez call a pridame pauzu na konci
call .\mvnw.cmd javafx:run

if %errorlevel% neq 0 (
    echo.
    echo [CHYBA] Maven Wrapper sa nepodarilo spustit.
    echo Moze to byt kvoli tomu, ze v systéme nie je nastavena JAVA_HOME.
)

echo.
pause