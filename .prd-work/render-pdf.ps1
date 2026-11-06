$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[Windows.Storage.StorageFile, Windows.Storage, ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.StorageFolder, Windows.Storage, ContentType=WindowsRuntime] | Out-Null
[Windows.Data.Pdf.PdfDocument, Windows.Data.Pdf, ContentType=WindowsRuntime] | Out-Null
[Windows.Data.Pdf.PdfPageRenderOptions, Windows.Data.Pdf, ContentType=WindowsRuntime] | Out-Null
function Await-Result($operation, $resultType) {
    $method = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetGenericArguments().Count -eq 1 -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1
    $task = $method.MakeGenericMethod($resultType).Invoke($null, @($operation))
    $task.Wait()
    return $task.Result
}
function Await-Action($operation) {
    $task = [System.WindowsRuntimeSystemExtensions]::AsTask($operation)
    $task.Wait()
}
$taskDir = 'E:\桌面文件\开发\梗图\.prd-work'
$file = Await-Result ([Windows.Storage.StorageFile]::GetFileFromPathAsync("$taskDir\preview.pdf")) ([Windows.Storage.StorageFile])
$pdf = Await-Result ([Windows.Data.Pdf.PdfDocument]::LoadFromFileAsync($file)) ([Windows.Data.Pdf.PdfDocument])
$folder = Await-Result ([Windows.Storage.StorageFolder]::GetFolderFromPathAsync($taskDir)) ([Windows.Storage.StorageFolder])
for ($i=0; $i -lt $pdf.PageCount; $i++) {
    $page = $pdf.GetPage($i)
    $outFile = Await-Result ($folder.CreateFileAsync("page-$($i+1).png", 1)) ([Windows.Storage.StorageFile])
    $stream = Await-Result ($outFile.OpenAsync(1)) ([Windows.Storage.Streams.IRandomAccessStream])
    $options = New-Object Windows.Data.Pdf.PdfPageRenderOptions
    $options.DestinationWidth = 1240
    Await-Action ($page.RenderToStreamAsync($stream, $options))
    $stream.Dispose()
    $page.Dispose()
}
Write-Output "Rendered $($pdf.PageCount) page images"
