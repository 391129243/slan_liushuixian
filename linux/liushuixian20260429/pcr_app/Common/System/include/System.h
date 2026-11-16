#ifndef __SYSTEM_H__
#define __SYSTEM_H__

#ifdef __cplusplus
extern "C" {
#endif

typedef struct MemInfo
{
	long total;
	long avail;
	long free;
}MemInfo;

typedef struct DiskInfo
{
	long total;
	long avail;
}DiskInfo;

typedef struct BandwidthInfo
{
	char total[64];
	char avail[64];
}BandwidthInfo;

typedef struct sCpuInfo
{
	long user;
	long nice;
	long system;
	long idle;
	long iowait;
	long irq;
	long softirq;
}CpuInfo;

typedef struct sNetTransInfo
{
	long recveive;
	long transimit;
}NetTransInfo;

int system_call(char *cmd);

int Mkdir(const char *dir);

long GetFileSize(const char *file);

int UnzipFile(const char* file_name, const char* unzip_path);

unsigned long GetFileSizeInByte(char *file_name);

int BackUpFile(const char* file_name);

int RemoveFile(const char* file_name);

int GetLocalIp(char* ifname, char* ip, int ipLen);

int GetExeAbsolutePath(char* path, size_t sizeof_path);

long int GetTimestamp();

int ChmodDir(const char *dir);

int RebootSystem();

unsigned long GetAvailDiskSize(const char* path);

int ReadDirList(const char* dir_path, DirList* dir_list);

int ReadDirListRecurve(const char* dir_path, DirList* dir_list, int max_depth);

int GetOSInfo(char* os_name, char* os_version);

int RenameFile(const char* src_file_name, const char* dst_file_name);

int GetMemoryInfo(MemInfo* mem);

float GetMemoryUnUseRate();

int GetDiskInfo(char* dir_path, DiskInfo* disk);

float GetDiskUnuseRate(char* dir_path);

int GetBandwidthInfo(BandwidthInfo* bandwidth);

int GetCPUIdleRate(CpuInfo* cpu);

int ProcessExists(pid_t pid);

int GetNetInfo(NetTransInfo* net_info);

int GetProcNetInfo(int pid, NetTransInfo* net_info);

int GetProcMem(int pid);

#ifdef __cplusplus
}
#endif

#endif