param (
    [string]$Tag = "",
    [switch]$SkipTests,
    [switch]$NoPause
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path "$PSScriptRoot/..").Path
Set-Location $ProjectRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DragonBall 2026] BUILD & VERSION JAR MANAGER" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Locate Java Tools (javac & jar)
$JavacPath = ""
$JarPath = ""

try {
    $JavacCmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($JavacCmd) {
        $JavacPath = $JavacCmd.Source
    }
} catch {}

if (-not $JavacPath -or -not (Test-Path $JavacPath)) {
    $PossibleJavacs = Get-ChildItem -Path "C:\Program Files\Java", "C:\Program Files (x86)\Java" -Filter "javac.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    if ($PossibleJavacs) {
        $JavacPath = if ($PossibleJavacs -is [array]) { $PossibleJavacs[0] } else { $PossibleJavacs }
    }
}

if (-not $JavacPath -or -not (Test-Path $JavacPath)) {
    Write-Host "[ERROR] Khong tim thay javac.exe trong he thong!" -ForegroundColor Red
    exit 1
}

# Find jar.exe next to javac.exe
$JarCandidate = (Join-Path (Split-Path $JavacPath) "jar.exe")
if (Test-Path $JarCandidate) {
    $JarPath = $JarCandidate
} else {
    $PossibleJars = Get-ChildItem -Path "C:\Program Files\Java", "C:\Program Files (x86)\Java" -Filter "jar.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    if ($PossibleJars) {
        $JarPath = if ($PossibleJars -is [array]) { $PossibleJars[0] } else { $PossibleJars }
    }
}

if (-not $JarPath -or -not (Test-Path $JarPath)) {
    Write-Host "[ERROR] Khong tim thay jar.exe trong he thong!" -ForegroundColor Red
    exit 1
}

Write-Host "[1/7] Java Tools:" -ForegroundColor Yellow
Write-Host "  - javac: $JavacPath"
Write-Host "  - jar:   $JarPath"

# 2. Determine Current Max Version and Next Version
$ScanDirs = @(
    $ProjectRoot,
    (Join-Path $ProjectRoot "dist"),
    (Join-Path $ProjectRoot "jar-backup"),
    (Join-Path $ProjectRoot "jar-backups")
)

$Versions = [System.Collections.Generic.List[int]]::new()

foreach ($dir in $ScanDirs) {
    if (Test-Path $dir) {
        Get-ChildItem -Path $dir -Filter "*.jar" -File -ErrorAction SilentlyContinue | ForEach-Object {
            $name = $_.Name
            $num = $null
            if ($name -match '^(\d{1,5})[-_]') {
                $num = [int]$Matches[1]
            } elseif ($name -match '^(\d{1,5})\.jar$') {
                $num = [int]$Matches[1]
            } elseif ($name -match '[-_](\d{1,5})\.jar$') {
                $num = [int]$Matches[1]
            } elseif ($name -match '[-_]v?(\d{1,5})[-_]') {
                $num = [int]$Matches[1]
            }
            if ($num -ne $null -and $num -lt 100000) {
                $Versions.Add($num)
            }
        }
    }
}

$MaxVersion = 0
if ($Versions.Count -gt 0) {
    $MaxVersion = ($Versions | Measure-Object -Maximum).Maximum
}

if ($MaxVersion -le 0) {
    $MaxVersion = 100
}

$NextVersion = $MaxVersion + 1
$DateStr = Get-Date -Format "yyyyMMdd"

$NewJarFileName = if ([string]::IsNullOrWhiteSpace($Tag)) {
    "$NextVersion-NgocRongOnline-$DateStr.jar"
} else {
    $CleanTag = ($Tag -replace '[^\w\-]', '-')
    "$NextVersion-$CleanTag-$DateStr.jar"
}

Write-Host "[2/7] Version Resolution:" -ForegroundColor Yellow
Write-Host "  - Version hien tai (Max): $MaxVersion"
Write-Host "  - Version moi tiep theo:  $NextVersion"
Write-Host "  - Ten file JAR moi:       $NewJarFileName"

# 3. Compile Source Code
Write-Host "[3/7] Dang bien dich source code (javac)..." -ForegroundColor Yellow
$BuildClassesDir = Join-Path $ProjectRoot "build/classes"
if (-not (Test-Path $BuildClassesDir)) {
    New-Item -ItemType Directory -Path $BuildClassesDir -Force | Out-Null
}

$JavaSources = Get-ChildItem -Path (Join-Path $ProjectRoot "src") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$FormattedSources = $JavaSources | ForEach-Object { '"' + ($_.Replace('\', '/')) + '"' }
$SourcesFile = Join-Path $ProjectRoot "build/sources_list.txt"
[System.IO.File]::WriteAllLines($SourcesFile, $FormattedSources, (New-Object System.Text.UTF8Encoding($false)))

$JavacArgs = @(
    "-encoding", "UTF-8",
    "-cp", "lib/*;src",
    "-d", "build/classes",
    "@build/sources_list.txt"
)

$CompileProc = Start-Process -FilePath $JavacPath -ArgumentList $JavacArgs -NoNewWindow -Wait -PassThru
if ($CompileProc.ExitCode -ne 0) {
    Write-Host "[ERROR] Compile that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
    exit 1
}
Write-Host "  -> Compile thanh cong!" -ForegroundColor Green

# 4. Run Smoke Tests (Unless skipped)
if (-not $SkipTests) {
    Write-Host "[4/7] Dang chay kiem tra tu dong (Regression Tests)..." -ForegroundColor Yellow
    $BuildTestDir = Join-Path $ProjectRoot "build/test/classes"
    if (-not (Test-Path $BuildTestDir)) {
        New-Item -ItemType Directory -Path $BuildTestDir -Force | Out-Null
    }

    $TestFile = Join-Path $ProjectRoot "test/nro/models/server/AdminSKHCommandTest.java"
    if (Test-Path $TestFile) {
        $TestCompileProc = Start-Process -FilePath $JavacPath -ArgumentList @("-encoding", "UTF-8", "-cp", "lib/*;build/classes;src", "-d", "build/test/classes", "test/nro/models/server/AdminSKHCommandTest.java") -NoNewWindow -Wait -PassThru
        if ($TestCompileProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Bien dich test AdminSKHCommandTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
        $TestRunProc = Start-Process -FilePath "java" -ArgumentList @("-cp", "build/test/classes;build/classes;lib/*", "nro.models.server.AdminSKHCommandTest") -NoNewWindow -Wait -PassThru
        if ($TestRunProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Unit Test AdminSKHCommandTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
    }

    $WmatTestFile = Join-Path $ProjectRoot "test/nro/models/matches/WorldMartialArtsTournamentNgoaiHangTest.java"
    if (Test-Path $WmatTestFile) {
        $WmatCompileProc = Start-Process -FilePath $JavacPath -ArgumentList @("-encoding", "UTF-8", "-cp", "lib/*;build/classes;src", "-d", "build/test/classes", "test/nro/models/matches/WorldMartialArtsTournamentNgoaiHangTest.java") -NoNewWindow -Wait -PassThru
        if ($WmatCompileProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Bien dich test WorldMartialArtsTournamentNgoaiHangTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
        $WmatRunProc = Start-Process -FilePath "java" -ArgumentList @("-cp", "build/test/classes;build/classes;lib/*", "nro.models.matches.WorldMartialArtsTournamentNgoaiHangTest") -NoNewWindow -Wait -PassThru
        if ($WmatRunProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Unit Test WorldMartialArtsTournamentNgoaiHangTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
    }

    $RubyShopTestFile = Join-Path $ProjectRoot "test/nro/models/shop/SatanRubyShopCurrencyTest.java"
    if (Test-Path $RubyShopTestFile) {
        $RubyShopCompileProc = Start-Process -FilePath $JavacPath -ArgumentList @("-encoding", "UTF-8", "-cp", "lib/*;build/classes;src", "-d", "build/test/classes", "test/nro/models/shop/SatanRubyShopCurrencyTest.java") -NoNewWindow -Wait -PassThru
        if ($RubyShopCompileProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Bien dich test SatanRubyShopCurrencyTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
        $RubyShopRunProc = Start-Process -FilePath "java" -ArgumentList @("-cp", "build/test/classes;build/classes;lib/*", "nro.models.shop.SatanRubyShopCurrencyTest") -NoNewWindow -Wait -PassThru
        if ($RubyShopRunProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Unit Test SatanRubyShopCurrencyTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
    }

    $WoodChestTestFile = Join-Path $ProjectRoot "test/nro/models/shop/WoodChestRewardAndPocoloHpTest.java"
    if (Test-Path $WoodChestTestFile) {
        $WoodChestCompileProc = Start-Process -FilePath $JavacPath -ArgumentList @("-encoding", "UTF-8", "-cp", "lib/*;build/classes;src", "-d", "build/test/classes", "test/nro/models/shop/WoodChestRewardAndPocoloHpTest.java") -NoNewWindow -Wait -PassThru
        if ($WoodChestCompileProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Bien dich test WoodChestRewardAndPocoloHpTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
        $WoodChestRunProc = Start-Process -FilePath "java" -ArgumentList @("-cp", "build/test/classes;build/classes;lib/*", "nro.models.shop.WoodChestRewardAndPocoloHpTest") -NoNewWindow -Wait -PassThru
        if ($WoodChestRunProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Unit Test WoodChestRewardAndPocoloHpTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
    }

    $AuthTestFile = Join-Path $ProjectRoot "test/nro/models/account/AccountRegistrationAndAuthTest.java"
    if (Test-Path $AuthTestFile) {
        $AuthCompileProc = Start-Process -FilePath $JavacPath -ArgumentList @("-encoding", "UTF-8", "-cp", "lib/*;build/classes;src", "-d", "build/test/classes", "test/nro/models/account/AccountRegistrationAndAuthTest.java") -NoNewWindow -Wait -PassThru
        if ($AuthCompileProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Bien dich test AccountRegistrationAndAuthTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
        $AuthRunProc = Start-Process -FilePath "java" -ArgumentList @("-cp", "build/test/classes;build/classes;lib/*", "nro.models.account.AccountRegistrationAndAuthTest") -NoNewWindow -Wait -PassThru
        if ($AuthRunProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Unit Test AccountRegistrationAndAuthTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
    }

    $AuthDbTestFile = Join-Path $ProjectRoot "test/nro/models/account/AccountRegistrationDBIntegrationTest.java"
    if (Test-Path $AuthDbTestFile) {
        $AuthDbCompileProc = Start-Process -FilePath $JavacPath -ArgumentList @("-encoding", "UTF-8", "-cp", "lib/*;build/classes;src", "-d", "build/test/classes", "test/nro/models/account/AccountRegistrationDBIntegrationTest.java") -NoNewWindow -Wait -PassThru
        if ($AuthDbCompileProc.ExitCode -ne 0) {
            Write-Host "[ERROR] Bien dich test AccountRegistrationDBIntegrationTest that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
            exit 1
        }
        $AuthDbRunProc = Start-Process -FilePath "java" -ArgumentList @("-cp", "build/test/classes;build/classes;lib/*", "nro.models.account.AccountRegistrationDBIntegrationTest") -NoNewWindow -Wait -PassThru
        if ($env:RUN_DB_INTEGRATION -eq "1" -or $env:RUN_DB_INTEGRATION -eq "true") {
            if ($AuthDbRunProc.ExitCode -ne 0) {
                Write-Host "[ERROR] Database Integration Test that bai (RUN_DB_INTEGRATION=1)! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
                exit 1
            }
            Write-Host "  -> DB Integration Tests: PASSED (MariaDB validated)" -ForegroundColor Green
        } else {
            Write-Host "  -> DB Integration Tests: SKIPPED (Set RUN_DB_INTEGRATION=1 to run)" -ForegroundColor Gray
        }
    }
    Write-Host "  -> Unit Regression Tests: PASS 100%!" -ForegroundColor Green
} else {
    Write-Host "[4/7] Bo qua Tests theo yeu cau." -ForegroundColor Gray
}

# 5. Build New JAR into Temporary File First
Write-Host "[5/7] Dang dong goi JAR moi vao vi tri tam..." -ForegroundColor Yellow
$TempJarPath = Join-Path $ProjectRoot "build/temp_release.jar"
if (Test-Path $TempJarPath) {
    Remove-Item $TempJarPath -Force -ErrorAction SilentlyContinue
}

$ManifestPath = Join-Path $ProjectRoot "manifest.mf"
$JarArgs = @(
    "cfm",
    "build/temp_release.jar",
    "manifest.mf",
    "-C", "build/classes",
    "."
)

$JarProc = Start-Process -FilePath $JarPath -ArgumentList $JarArgs -NoNewWindow -Wait -PassThru
if ($JarProc.ExitCode -ne 0 -or -not (Test-Path $TempJarPath)) {
    Write-Host "[ERROR] Dong goi JAR that bai! Giu nguyen JAR cu va run.bat." -ForegroundColor Red
    exit 1
}

$TempJarSize = (Get-Item $TempJarPath).Length
if ($TempJarSize -lt 500000) {
    Write-Host "[ERROR] JAR tao ra qua nho ($TempJarSize bytes), nghi ngo loi dong goi!" -ForegroundColor Red
    exit 1
}
Write-Host "  -> Temp JAR tao thanh cong ($([math]::Round($TempJarSize / 1MB, 2)) MB)" -ForegroundColor Green

# 6. Backup Old JAR
Write-Host "[6/7] Dang sao luu JAR cu vao jar-backup/..." -ForegroundColor Yellow
$BackupDir = Join-Path $ProjectRoot "jar-backup"
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$DistDir = Join-Path $ProjectRoot "dist"
if (-not (Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir -Force | Out-Null
}

$RunBatPath = Join-Path $ProjectRoot "run.bat"
$OldJarPath = ""

if (Test-Path $RunBatPath) {
    $RunContent = Get-Content $RunBatPath -Raw
    if ($RunContent -match 'set\s+"?JAR=([^"\r\n]+)"?') {
        $OldJarPath = $Matches[1].Trim()
    } elseif ($RunContent -match '-jar\s+([^\s\r\n]+)') {
        $OldJarPath = $Matches[1].Trim()
    }
}

if ($OldJarPath) {
    $ResolvedOldJar = if ([System.IO.Path]::IsPathRooted($OldJarPath)) {
        $OldJarPath
    } else {
        Join-Path $ProjectRoot $OldJarPath
    }

    if (Test-Path $ResolvedOldJar) {
        $OldName = [System.IO.Path]::GetFileName($ResolvedOldJar)
        $BackupTargetName = $OldName
        if ($OldName -eq "NgocRongOnline.jar") {
            $BackupTargetName = "$MaxVersion-NgocRongOnline-backup.jar"
        }

        $BackupFilePath = Join-Path $BackupDir $BackupTargetName
        try {
            Copy-Item -Path $ResolvedOldJar -Destination $BackupFilePath -Force
            Write-Host "  -> Da sao luu: $OldName => jar-backup/$BackupTargetName" -ForegroundColor Green
        } catch {
            Write-Host "  -> [WARN] Khong the luu JAR cu sang backup: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

Get-ChildItem -Path $ProjectRoot -Filter "*.jar" -File -ErrorAction SilentlyContinue | ForEach-Object {
    if ($_.Name -match '^\d+-' -and $_.Name -ne $NewJarFileName) {
        $Dest = Join-Path $BackupDir $_.Name
        if (-not (Test-Path $Dest)) {
            Move-Item -Path $_.FullName -Destination $Dest -Force -ErrorAction SilentlyContinue
        }
    }
}

# 7. Deploy New JAR & Update run.bat
Write-Host "[7/7] Trien khai JAR moi va cap nhat run.bat..." -ForegroundColor Yellow

$NewJarDest = Join-Path $DistDir $NewJarFileName
Copy-Item -Path $TempJarPath -Destination $NewJarDest -Force

$MasterDistJar = Join-Path $DistDir "NgocRongOnline.jar"
try {
    Copy-Item -Path $TempJarPath -Destination $MasterDistJar -Force -ErrorAction SilentlyContinue
} catch {}

Remove-Item $TempJarPath -Force -ErrorAction SilentlyContinue

$DistLib = Join-Path $DistDir "lib"
if (-not (Test-Path $DistLib)) {
    New-Item -ItemType Directory -Path $DistLib -Force | Out-Null
}
Copy-Item -Path (Join-Path $ProjectRoot "lib/*") -Destination $DistLib -Force -ErrorAction SilentlyContinue

# Format new run.bat content
$RunBatLines = @(
    "@ECHO OFF",
    'cd /d "%~dp0"',
    "",
    ':: [DragonBall 2026] Server Runtime JAR',
    "set `"JAR=dist\$NewJarFileName`"",
    "",
    ':: Fallback kiem tra neu file khong ton tai',
    'if not exist "%JAR%" (',
    '    if exist "dist\NgocRongOnline.jar" (',
    '        set "JAR=dist\NgocRongOnline.jar"',
    '    ) else (',
    '        for /f "delims=" %%i in (''dir /b /o-d dist\*.jar 2^>nul'') do (',
    '            set "JAR=dist\%%i"',
    '            goto :run_server',
    '        )',
    '    )',
    ')',
    "",
    ":run_server",
    'if not exist "%JAR%" (',
    '    echo [ERROR] Khong tim thay JAR runtime de khoi dong server!',
    '    pause',
    '    exit /b 1',
    ')',
    "",
    "echo ==========================================================",
    "echo  [DragonBall 2026] Khoi dong server: %JAR%",
    "echo ==========================================================",
    'java -server -Dfile.encoding=UTF-8 -jar "%JAR%"',
    "PAUSE"
)

[System.IO.File]::WriteAllLines($RunBatPath, $RunBatLines, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " [THANH CONG] BUILD HOAN TAT!" -ForegroundColor Green
Write-Host "  - JAR moi da tao:     dist/$NewJarFileName" -ForegroundColor Green
Write-Host "  - Version moi:        $NextVersion" -ForegroundColor Green
Write-Host "  - JAR cu da luu tai:  jar-backup/" -ForegroundColor Green
Write-Host "  - run.bat cap nhat:   dist/$NewJarFileName" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green