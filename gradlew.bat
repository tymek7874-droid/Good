@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo gradle not found. Install Gradle or open this project in AndroidIDE.
  exit /b 127
)
gradle %*
