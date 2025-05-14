@echo off
echo Killing all Java processes...
taskkill /F /IM java.exe
taskkill /F /IM javaw.exe
taskkill /F /IM studio64.exe

echo Stopping Gradle daemons...
call gradlew --stop

timeout /t 5 /nobreak

echo Removing read-only attributes from build folders...
attrib -r -s -h app\build\* /s /d
attrib -r -s -h .gradle\* /s /d

echo Cleaning Gradle cache...
rmdir /s /q .gradle
rmdir /s /q .idea
rmdir /s /q app\.cxx
rmdir /s /q app\build
rmdir /s /q build

echo Fixing file locks using unlocker method...
echo This may take a moment...

echo Clearing Android Studio cache...
rmdir /s /q %LOCALAPPDATA%\Google\AndroidStudio*\system\caches
rmdir /s /q %LOCALAPPDATA%\Google\AndroidStudio*\system\compile-server

echo Done! Now you can restart Android Studio and it should work properly. 