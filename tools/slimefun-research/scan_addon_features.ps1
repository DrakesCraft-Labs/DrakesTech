param(
    [Parameter(Mandatory = $true)]
    [string]$SlimefunRoot,
    [Parameter(Mandatory = $false)]
    [string]$OutputCsv = ".\\tools\\slimefun-research\\addon-feature-scan.csv"
)

$ErrorActionPreference = "Stop"

$keywords = @(
    "3x3",
    "4x4",
    "5x5",
    "7x7",
    "explosive",
    "missile",
    "weapon",
    "gun",
    "drill",
    "chainsaw",
    "network",
    "storage",
    "bridge",
    "controller"
)

if (!(Test-Path $SlimefunRoot)) {
    throw "Slimefun root not found: $SlimefunRoot"
}

$rows = New-Object System.Collections.Generic.List[object]

Get-ChildItem -Path $SlimefunRoot -Directory | ForEach-Object {
    $addonName = $_.Name
    $javaFiles = Get-ChildItem -Path $_.FullName -Recurse -Filter *.java -ErrorAction SilentlyContinue
    foreach ($file in $javaFiles) {
        $matches = Select-String -Path $file.FullName -Pattern $keywords -CaseSensitive:$false -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $rows.Add([PSCustomObject]@{
                addon = $addonName
                keyword = $m.Matches.Value
                file = $file.FullName
                line = $m.LineNumber
                snippet = $m.Line.Trim()
            })
        }
    }
}

$dir = Split-Path -Parent $OutputCsv
if ($dir -and !(Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$rows | Export-Csv -Path $OutputCsv -NoTypeInformation -Encoding UTF8
Write-Output "Scan completed. Rows: $($rows.Count)"
Write-Output "Output: $OutputCsv"

