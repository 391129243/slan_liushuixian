# 默认仅查询版本。-IncludeDevices 仅在已确认无出错实验时使用。
# GetAllDevice 在设备存在出错实验时可能导致该实验结束，不能用于无条件恢复探测。
param([switch]$IncludeDevices)
$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8186/SlanServerService'
$version = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/GetVersion') -ContentType 'application/json' -Body '{}'
$version.GetVersionResult
if ($IncludeDevices) {
    $devices = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/GetAllDevice') -ContentType 'application/json' -Body '{}'
    $devices.GetAllDeviceResult | Format-List
}
