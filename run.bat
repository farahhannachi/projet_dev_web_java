@echo off
setlocal

REM Run using Maven JavaFX plugin (portable)
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo [ERROR] Maven (mvn) not found in PATH.
  exit /b 1
)

echo Starting CuraVita...
mvn -q -DskipTests javafx:run

endlocal
pause
