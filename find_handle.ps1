$ErrorActionPreference = "SilentlyContinue"

# Đường dẫn đến file R.jar
$filePath = "D:\MobileApp\app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\processDebugResources\R.jar"

Write-Host "Tìm tiến trình đang khóa file: $filePath" -ForegroundColor Yellow

# Kiểm tra xem file có tồn tại không
if (Test-Path $filePath) {
    Write-Host "File tồn tại, đang kiểm tra tiến trình..." -ForegroundColor Cyan
} else {
    Write-Host "File không tồn tại!" -ForegroundColor Red
    exit
}

# Lấy danh sách tất cả tiến trình Java đang chạy
$javaProcesses = Get-Process | Where-Object { $_.Name -like "*java*" -or $_.Name -like "*gradle*" -or $_.ProcessName -like "*studio*" }

Write-Host "`nTất cả tiến trình Java/Gradle đang chạy:" -ForegroundColor Green
$javaProcesses | Format-Table Id, ProcessName, Path -AutoSize

# Kết thúc tất cả tiến trình Java
Write-Host "`nĐang kết thúc tất cả tiến trình Java và Gradle..." -ForegroundColor Yellow
$javaProcesses | ForEach-Object {
    Write-Host "Kết thúc $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Red
    Stop-Process -Id $_.Id -Force
}

# Dừng Gradle Daemon
Write-Host "`nDừng Gradle Daemon..." -ForegroundColor Yellow
./gradlew --stop

# Thử xóa file R.jar
Write-Host "`nThử xóa file R.jar..." -ForegroundColor Yellow
try {
    # Đợi một chút để các tiến trình kết thúc hoàn toàn
    Start-Sleep -Seconds 3
    
    # Xóa file
    Remove-Item -Path $filePath -Force -ErrorAction Stop
    Write-Host "Đã xóa thành công file R.jar!" -ForegroundColor Green
} catch {
    Write-Host "Không thể xóa file: $_" -ForegroundColor Red
    
    # Thử phương pháp cuối cùng - xóa bằng cmd
    Write-Host "Thử xóa bằng CMD..." -ForegroundColor Yellow
    cmd /c "del /f /q `"$filePath`""
    
    if (Test-Path $filePath) {
        Write-Host "Vẫn không thể xóa file!" -ForegroundColor Red
    } else {
        Write-Host "Đã xóa thành công file R.jar bằng CMD!" -ForegroundColor Green
    }
}

# Thử tạo thư mục mới 
Write-Host "`nTạo thư mục build mới..." -ForegroundColor Yellow
$buildPath = Split-Path -Parent $filePath
$parentDir = Split-Path -Parent $buildPath

if (!(Test-Path $parentDir)) {
    try {
        New-Item -Path $parentDir -ItemType Directory -Force
        Write-Host "Đã tạo thư mục build mới!" -ForegroundColor Green
    } catch {
        Write-Host "Không thể tạo thư mục build mới: $_" -ForegroundColor Red
    }
} 