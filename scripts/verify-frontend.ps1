# DevPilot AI — Frontend Quality Verification Script

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " DevPilot AI — Frontend Verification Suite " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$root = Join-Path $PSScriptRoot "..\frontend"
$pages = @("index.html", "login.html", "register.html", "dashboard.html", "workspace.html", "settings.html", "profile.html", "404.html")
$cssFiles = @("variables.css", "reset.css", "animations.css", "global.css", "components.css", "navbar.css", "sidebar.css", "landing.css", "auth.css", "dashboard.css", "workspace.css", "settings.css", "profile.css", "responsive.css")
$jsFiles = @("config.js", "storage.js", "utils.js", "api.js", "components.js", "navigation.js", "landing.js", "login.js", "register.js", "dashboard.js", "workspace.js", "settings.js", "profile.js")

$allPassed = $true

Write-Host "`n[1] Verifying HTML Pages..." -ForegroundColor Yellow
foreach ($page in $pages) {
    $filePath = Join-Path $root $page
    if (Test-Path $filePath) {
        Write-Host "  [OK] $page exists" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Missing $page" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host "`n[2] Verifying CSS Modules..." -ForegroundColor Yellow
foreach ($css in $cssFiles) {
    $filePath = Join-Path (Join-Path $root "css") $css
    if (Test-Path $filePath) {
        Write-Host "  [OK] css/$css exists" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Missing css/$css" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host "`n[3] Verifying JavaScript Modules..." -ForegroundColor Yellow
foreach ($js in $jsFiles) {
    $filePath = Join-Path (Join-Path $root "js") $js
    if (Test-Path $filePath) {
        Write-Host "  [OK] js/$js exists" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Missing js/$js" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host "`n[4] Scanning for duplicate HTML element IDs..." -ForegroundColor Yellow
foreach ($page in $pages) {
    $filePath = Join-Path $root $page
    if (Test-Path $filePath) {
        $content = Get-Content $filePath -Raw
        $idMatches = [System.Text.RegularExpressions.Regex]::Matches($content, 'id="([^"]+)"')
        $idList = @()
        foreach ($m in $idMatches) {
            $idList += $m.Groups[1].Value
        }
        $duplicates = $idList | Group-Object | Where-Object { $_.Count -gt 1 }
        if ($duplicates) {
            $dupNames = ($duplicates | ForEach-Object { $_.Name }) -join ", "
            Write-Host "  [FAIL] Duplicate IDs in $page : $dupNames" -ForegroundColor Red
            $allPassed = $false
        } else {
            Write-Host "  [OK] $page has unique IDs ($($idList.Count) IDs scanned)" -ForegroundColor Green
        }
    }
}

if ($allPassed) {
    Write-Host "`n>>> All Frontend Quality Checks Passed Successfully! <<<" -ForegroundColor Green
} else {
    Write-Host "`n>>> Verification Failed. Please fix reported issues. <<<" -ForegroundColor Red
    exit 1
}
