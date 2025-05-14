@echo off
echo DANG XOA TIEN TRINH JAVA...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
taskkill /F /IM gradle.exe 2>nul

echo DUNG GRADLE DAEMON...
call gradlew --stop
timeout /t 3 /nobreak > nul

echo TAO CHO TRONG THU MUC...
rd /s /q ".gradle" 2>nul
rd /s /q "build" 2>nul

echo XOA TAT CA BUILD FOLDER...
for /d /r %%a in (*build) do (
  if exist "%%a" rd /s /q "%%a" 2>nul
)

echo XOA R.JAR BANG CACH TRUC TIEP...
set RJAR_PATH=app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\processDebugResources\R.jar
if exist "%RJAR_PATH%" (
  del /f /q "%RJAR_PATH%" 2>nul
  if exist "%RJAR_PATH%" (
    echo KHONG THE XOA, THU PHUONG PHAP KHAC...
    cd app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\
    rd /s /q processDebugResources 2>nul
    cd ..\..\..\..\..\..
  )
)

echo XOA THU MUC BUILD TRUC TIEP...
rd /s /q "app\build" 2>nul

echo XOA GRADLE CACHE...
rd /s /q "%USERPROFILE%\.gradle\caches\6.7.1" 2>nul

echo ----------------------------
echo HOAN THANH! THU BUILD LAI!
echo ---------------------------- 