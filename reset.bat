@echo off
echo Killing java processes...
taskkill /F /IM java.exe
taskkill /F /IM javaw.exe

echo Stopping gradle...
call gradlew --stop

echo Waiting for processes to stop...
timeout /t 5 /nobreak

echo Removing R.jar...
cd /d D:\MobileApp
del /f /s /q app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\processDebugResources\R.jar

echo Cleaning Gradle cache...
rmdir /s /q .gradle
rmdir /s /q build

echo Done. Now run ./gradlew assembleDebug again. 