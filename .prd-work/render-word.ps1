$ErrorActionPreference = 'Stop'
$taskRoot = 'E:\桌面文件\开发\梗图'
$word = $null
$document = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $document = $word.Documents.Open((Join-Path $taskRoot 'docx\双人梗簿_PRD_v0.1.docx'), $false, $true)
    $document.Repaginate()
    $pages = $document.ComputeStatistics(2)
    $document.ExportAsFixedFormat((Join-Path $taskRoot '.prd-work\preview.pdf'), 17)
    Write-Output "Rendered PDF with $pages pages"
} finally {
    if ($null -ne $document) { $document.Close(0) }
    if ($null -ne $word) { $word.Quit() }
}
