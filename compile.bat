@echo off
setlocal

REM Maven-based build (portable: no absolute paths)
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo [ERROR] Maven (mvn) not found in PATH.
  echo Install Maven or use the included scripts in Projet/ if you intended that setup.
  exit /b 1
)

echo Building project...
mvn -q -DskipTests clean package
if %ERRORLEVEL% EQU 0 (
  echo Build successful!
) else (
  echo Build failed!
)

endlocal
pause
