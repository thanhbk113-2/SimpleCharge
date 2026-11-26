# Kiem tra claude-mem co dang chay qua OmniRoute khong
# Cach chay: chuot phai file -> "Run with PowerShell"
# Hoac trong terminal: powershell -ExecutionPolicy Bypass -File .\.claude-mem\check-claude-mem.ps1

$dir = Join-Path $env:USERPROFILE ".claude-mem"

# ---------- 1. Cau hinh ----------
Write-Host "`n== 1. CAU HINH ==" -ForegroundColor Cyan
$settingsPath = Join-Path $dir "settings.json"
$envPath = Join-Path $dir ".env"
$model = "antigravity-combo"
$baseUrl = "http://localhost:20128"
$token = "omniroute"

if (Test-Path $settingsPath) {
    try {
        $s = Get-Content $settingsPath -Raw | ConvertFrom-Json
        Write-Host "CLAUDE_MEM_PROVIDER         = $($s.CLAUDE_MEM_PROVIDER)"
        Write-Host "CLAUDE_MEM_MODEL            = $($s.CLAUDE_MEM_MODEL)"
        Write-Host "CLAUDE_MEM_CLAUDE_AUTH_METHOD = $($s.CLAUDE_MEM_CLAUDE_AUTH_METHOD)"
        if ($s.CLAUDE_MEM_MODEL) { $model = $s.CLAUDE_MEM_MODEL }
        if ($s.CLAUDE_MEM_CLAUDE_AUTH_METHOD -ne "gateway") {
            Write-Host "  [!] AUTH_METHOD khong phai 'gateway' -> co the van goi thang Anthropic" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [X] settings.json sai cu phap JSON!" -ForegroundColor Red
    }
} else {
    Write-Host "  [X] Khong thay $settingsPath" -ForegroundColor Red
}

if (Test-Path $envPath) {
    foreach ($line in Get-Content $envPath) {
        if ($line -match '^\s*ANTHROPIC_BASE_URL\s*=\s*(.+)$') { $baseUrl = $Matches[1].Trim() }
        if ($line -match '^\s*ANTHROPIC_AUTH_TOKEN\s*=\s*(.+)$') { $token = $Matches[1].Trim() }
    }
    Write-Host "ANTHROPIC_BASE_URL (.env)   = $baseUrl"
} else {
    Write-Host "  [X] Khong thay file .env" -ForegroundColor Red
}

# ---------- 2. Goi thu OmniRoute bang dung model cua claude-mem ----------
Write-Host "`n== 2. GOI THU OMNIROUTE (model: $model) ==" -ForegroundColor Cyan
$body = @{
    model = $model
    max_tokens = 20
    messages = @(@{ role = "user"; content = "Reply with the single word: OK" })
} | ConvertTo-Json -Depth 5
try {
    $r = Invoke-RestMethod -Uri "$($baseUrl.TrimEnd('/'))/v1/messages" -Method Post `
        -Headers @{ Authorization = "Bearer $token"; "anthropic-version" = "2023-06-01" } `
        -ContentType "application/json" -Body $body -TimeoutSec 60
    $text = ($r.content | Where-Object { $_.type -eq "text" } | ForEach-Object { $_.text }) -join " "
    Write-Host "  [OK] OmniRoute tra loi: $text" -ForegroundColor Green
} catch {
    Write-Host "  [X] Goi that bai: $($_.Exception.Message)" -ForegroundColor Red
}

# ---------- 3. Log claude-mem gan nhat ----------
Write-Host "`n== 3. LOG CLAUDE-MEM GAN NHAT ==" -ForegroundColor Cyan
$log = Get-ChildItem (Join-Path $dir "logs") -Filter "*.log" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($log) {
    Write-Host "File: $($log.Name)"
    $hits = Get-Content $log.FullName -Tail 500 |
        Select-String "Response received|Credit balance|quota|cooldown|authMethod|exited with code"
    $hits | Select-Object -Last 12 | ForEach-Object { Write-Host "  $($_.Line)" }

    $last = $hits | Select-Object -Last 1
    Write-Host "`n== KET LUAN ==" -ForegroundColor Cyan
    if (-not $last) {
        Write-Host "  Chua co hoat dong tom tat nao gan day. Lam vai thao tac trong Claude Code roi chay lai." -ForegroundColor Yellow
    } elseif ($last.Line -match "Credit balance|quota|cooldown|exited with code") {
        Write-Host "  [X] Dong log moi nhat van bao loi/tam dung -> claude-mem CHUA chay on." -ForegroundColor Red
    } else {
        Write-Host "  [OK] Dong log moi nhat la phan hoi binh thuong -> claude-mem dang chay qua OmniRoute." -ForegroundColor Green
    }
} else {
    Write-Host "  [X] Khong tim thay file log" -ForegroundColor Red
}

Write-Host ""
Read-Host "Nhan Enter de dong"
