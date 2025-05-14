# Dừng các tiến trình Gradle
Write-Host "Dừng Gradle Daemon..." -ForegroundColor Yellow
./gradlew --stop

# Tìm và kết thúc các tiến trình Java liên quan đến Android
Write-Host "Đang tìm và kết thúc các tiến trình Java..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object {$_.MainWindowTitle -like "*Android*" -or $_.Path -like "*android*"} | ForEach-Object { 
    Write-Host "Kết thúc: $($_.ProcessName) (ID: $($_.Id))" -ForegroundColor Cyan
    Stop-Process -Id $_.Id -Force 
}

# Chờ một chút
Start-Sleep -Seconds 2

# Xóa thư mục build
Write-Host "Xóa thư mục build..." -ForegroundColor Yellow
if (Test-Path "app/build") {
    try {
        Remove-Item -Path "app/build" -Recurse -Force -ErrorAction Stop
        Write-Host "Đã xóa thành công thư mục build" -ForegroundColor Green
    } catch {
        Write-Host "Không thể xóa thư mục build: $_" -ForegroundColor Red
        Write-Host "Cố gắng xóa riêng file R.jar..." -ForegroundColor Yellow
        
        # Thử xóa file R.jar bằng cách khác
        $rJarPath = "app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
        if (Test-Path $rJarPath) {
            try {
                [System.IO.File]::Delete((Resolve-Path $rJarPath).Path)
                Write-Host "Đã xóa thành công file R.jar" -ForegroundColor Green
            } catch {
                Write-Host "Vẫn không thể xóa R.jar: $_" -ForegroundColor Red
            }
        }
    }
}

# Xóa thư mục cache Gradle
Write-Host "Xóa cache Gradle..." -ForegroundColor Yellow
if (Test-Path ".gradle") {
    try {
        Remove-Item -Path ".gradle" -Recurse -Force
        Write-Host "Đã xóa thành công thư mục .gradle" -ForegroundColor Green
    } catch {
        Write-Host "Không thể xóa thư mục .gradle: $_" -ForegroundColor Red
    }
}

Write-Host "Quá trình khởi động lại hoàn tất!" -ForegroundColor Green
Write-Host "Bây giờ hãy khởi động lại Android Studio và thử build lại project." -ForegroundColor Green 