@echo off
setlocal enabledelayedexpansion

REM Enable debug like "set -x"
echo on

REM ===== Extract version from pom.xml =====
for /f "tokens=*" %%i in ('powershell -Command "(Select-String -Path ..\pom.xml -Pattern '<version>' -AllMatches | Select-Object -First 1).Line"') do (
    set "LINE=%%i"
)

for /f "tokens=2 delims=<>" %%a in ("%LINE%") do (
    set "VERSION=%%a"
)

REM ===== Images =====
set "IMAGE_LOCAL=pierre-yves-monnet/zeebe-cherry-runtime"
set "IMAGE_REMOTE=ghcr.io/camunda-community-hub/zeebe-cherry-runtime"

echo Building Docker image version %VERSION%...

cd ..

docker build -t %IMAGE_LOCAL%:%VERSION% .

docker tag %IMAGE_LOCAL%:%VERSION% %IMAGE_REMOTE%:latest
docker tag %IMAGE_LOCAL%:%VERSION% %IMAGE_REMOTE%:%VERSION%

echo Pushing images...

docker push %IMAGE_REMOTE%:%VERSION%
docker push %IMAGE_REMOTE%:latest

echo Done.

endlocal