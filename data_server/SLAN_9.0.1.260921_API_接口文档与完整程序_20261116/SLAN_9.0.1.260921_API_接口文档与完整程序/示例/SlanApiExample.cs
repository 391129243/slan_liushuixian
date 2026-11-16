using System;
using System.Linq;
using System.ServiceModel;
using SLAN.API;
using SLAN.API.Client;
using SLAN.API.Enum;
using SLAN.API.Model;

// 示例库：调用者负责串行调度，并在真实实验前确认项目、孔板和设备状态。
// 本文件编译为 DLL，不会自行连接服务或启动实验。
public static class SlanApiExample
{
    // 仅用于正常初次连接：GetAllDevices 在存在出错实验时可能结束该实验。
    // 异常恢复应保留原设备/模块信息并定向查询，不要自动重新枚举。
    public static SlanDevice ConnectAndSelect(string serialNumber)
    {
        SlanService.Initialize("SlanServerService");
        Console.WriteLine("Service version: " + SlanService.GetVersion());
        var devices = SlanService.GetAllDevices() ?? new SlanDevice[0];
        var device = devices.FirstOrDefault(x => x.SerialNumber == serialNumber);
        if (device == null) throw new InvalidOperationException("Device not found: " + serialNumber);
        return device;
    }

    // validatedPlateContent 必须引用已经注册且验证过的项目。
    // 本方法只演示 08H/16H；拒绝未处理的已有实验，不自动停止或覆盖它们。
    public static RunExperimentResponse StartValidatedPlate(
        SlanDevice device, string validatedPlateContent, string uniqueExperimentName)
    {
        if (device == null) throw new ArgumentNullException("device");
        if (String.IsNullOrWhiteSpace(validatedPlateContent)) throw new ArgumentException("Plate is empty");
        if (String.IsNullOrWhiteSpace(uniqueExperimentName)) throw new ArgumentException("Name is empty");
        if (device.DeviceType != DeviceType.SLAN_08H && device.DeviceType != DeviceType.SLAN_16H)
            throw new NotSupportedException("Example supports only SLAN_08H and SLAN_16H");
        if (device.ExperimentList != null && device.ExperimentList.Count > 0)
            throw new InvalidOperationException("Handle existing experiments before starting");
        device.UseBlock = 0x01;
        var before = device.GetRunningInformation();
        if (before == null || before.State != RunningState.None)
            throw new InvalidOperationException("Device is not confirmed idle");
        var setting = new ExperimentSetting
        {
            PlateContent = validatedPlateContent,
            PlateFile = null,
            Block = 0x01,
            Name = uniqueExperimentName,
            UseHotBonnet = device.DeviceType == DeviceType.SLAN_08H,
            ScanMode = ChannelScanMode.Project,
            TemperatureControlMode = TemperatureControlMode.Standard,
            FilePath = null
        };
        var response = device.RunExperimentEx(setting);
        if (response == null || response.Error != RunExperimentError.None)
            throw new InvalidOperationException("Start failed: " + (response == null ? "null" : response.Error.ToString()));
        Console.WriteLine("Server result path: " + response.SavePath);
        return response;
    }

    // 调用者约每秒一次调用；不要与其他设备方法并发调用。
    // 返回 null 表示尚未完成。异常、超时均不会自动停止或释放仪器。
    public static Experiment ReadCompletedResult(SlanDevice device)
    {
        var info = device.GetRunningInformation();
        if (info == null) throw new InvalidOperationException("No running information");
        Console.WriteLine("State={0}, Code={1}, Remaining={2}", info.State, info.Code, info.RemainTime);
        if (info.State == RunningState.Error || info.State == RunningState.Crash)
            throw new InvalidOperationException("Experiment needs recovery: " + info.State + "/" + info.Code);
        if (info.State != RunningState.Finished) return null;
        var result = device.GetExperimentResult();
        if (result == null || result.WellResult == null || result.WellResult.Length == 0)
            throw new InvalidOperationException("Final result not ready; retry within a bounded deadline");
        // 应用还应按实际实验核对目标、曲线和标准曲线完整性，并先可靠保存。
        return result;
    }

    public static void ReleaseAfterResultSaved(SlanDevice device)
    {
        var info = device.GetRunningInformation();
        if (info == null || info.State != RunningState.Finished)
            throw new InvalidOperationException("Normal completion is not confirmed");
        if (!device.ReleaseExperiment()) throw new InvalidOperationException("Release failed; retain session and retry");
    }

    public static void CloseClientOnly()
    {
        SlanService.Close();
    }

    // 捕获示例，调用者应记录异常并确认设备状态，不能直接重复启动。
    public static void PrintCommunicationFailure(Exception ex)
    {
        var fault = ex as FaultException<SlanServerFault>;
        Console.Error.WriteLine(fault != null && fault.Detail != null ? fault.Detail.Message : ex.Message);
    }
}
