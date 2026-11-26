
$ErrorActionPreference = "Stop"

$inputJson = [Console]::In.ReadToEnd()
$data = $inputJson | ConvertFrom-Json
$cmd = [string]$data.tool_input.command

# Chỉ chặn các lệnh phá hủy dữ liệu đã nhận diện
$dangerPatterns = @(
    '(?i)\bgit\s+reset\s+--hard\b',
    '(?i)\bgit\s+clean\s+.*-[^\s]*[fd]',
    '(?i)\brm\s+-[^\n]*r[^\n]*f',
    '(?i)\bRemove-Item\b.*(-Recurse|-Force)',
    '(?i)\brmdir\s+/s',
    '(?i)\bdel\s+/s\b',
    '(?i)\bformat(\.com)?\s+[a-z]:',
    '(?i)\bdiskpart\b'
)

foreach ($pattern in $dangerPatterns) {
    if ($cmd -match $pattern) {
        [Console]::Error.WriteLine(
            "SECURITY GUARD: Blocked destructive command: $cmd"
        )
        exit 2
    }
}

exit 0
