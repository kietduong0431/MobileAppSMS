@echo off
echo ===== SUPER AGGRESSIVE CLEAN =====

echo Killing all Java processes...
taskkill /F /IM java.exe
taskkill /F /IM javaw.exe

echo Killing Gradle daemon and Android processes...
taskkill /F /IM gradle.exe
taskkill /F /FI "IMAGENAME eq java*"

echo Waiting 3 seconds for processes to terminate...
timeout /t 3

echo Stopping Gradle daemon...
call gradlew --stop

echo Waiting 3 seconds for Gradle to fully stop...
timeout /t 3

echo Removing R.jar directly...
del /F /Q "app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\processDebugResources\R.jar"

echo Removing all build directories...
rmdir /S /Q build
rmdir /S /Q app\build
rmdir /S /Q .gradle

echo Waiting 2 seconds...
timeout /t 2

echo Cleaning Android Studio caches...
rmdir /S /Q %USERPROFILE%\.gradle\caches
rmdir /S /Q %USERPROFILE%\.android\cache

echo Waiting 2 seconds...
timeout /t 2

echo Running Gradle clean...
call gradlew clean

echo ===== CLEAN COMPLETED ===== 