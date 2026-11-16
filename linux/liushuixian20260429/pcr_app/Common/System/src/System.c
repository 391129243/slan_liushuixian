#include <stdio.h>
#include <stdlib.h>
#include <sys/statfs.h>
#include <dirent.h>

#include <sys/socket.h>  
#include <unistd.h>        /* System V */
#include <sys/ioctl.h>    /* BSD and Linux */
#include <arpa/inet.h>
#include <net/if.h>
#include <errno.h>
#include <sys/utsname.h>

#include "Common.h"
#include "System.h"
#include "cJSON.h"

typedef  struct statfs DISK,*pDISK;

int system_call(char *cmd)
{
    if(NULL == cmd) {
        return -1;
    }

    pid_t status;
    status = system(cmd);

    if(-1 == status) {
        return -1;
    } else {
        if (WIFEXITED(status)) {
            if (0 == WEXITSTATUS(status)) {
                return 0;
            } else {
                return -1;
            }
        } else {
            printf("exit status = [%d]\n", WEXITSTATUS(status));
            return -1;
        }
    }
    return 0;
}

/*int system_call_debug(char* cmd) {
	if(NULL == cmd) {
        return -1;
    }

	FILE* fp = NULL;
	char buf[1024] = {0};
	if (NULL != (fp=popen((const char*)cmd,"r"))) {
		fgets(buf, sizeof(buf), fp);
		printf("systerm call cmd:%s. result:%s\n", cmd, buf);
		pclose(fp);
		return 0;
	} else {
		return -1;
	}
}*/

int Mkdir(const char *dir)
{
	if (NULL == dir) {
		return -1;
	}

	if (access(dir, W_OK) != 0) {
		if (mkdir(dir, S_IRWXU|S_IRWXG|S_IRWXO) == -1) {
			printf("create path %s failed.", dir);
			return -1;
		}
	}
	return 0;
}

long GetFileSize(const char *file)
{
	if (NULL == file) {
		return 0;	
	}

	FILE *fp = NULL;
	char cmd[300] = {0};
	snprintf(cmd, sizeof(cmd), "du -s %s", file);
	fp = popen(cmd, "r");
	if (NULL == fp) {
		return 0;	
	}

	long bytes = 0;
	char str[400] = {0};
 	fscanf(fp, "%ld	%s", &bytes, str);
	pclose(fp);

	return bytes;
}

int UnzipFile(const char* file_name, const char* unzip_path)
{
	int ret = 0;
	char cmd_str[MAX_FILE_NAME_LEN] = {0};
	char file_dir[MAX_FILE_NAME_LEN] = {0};
	char* pos = NULL;
	
	if (file_name == NULL) {
		printf("Input error, file_name is NULL.\n");
		return -1;
	}

	if (unzip_path == NULL)
	{
		if (strstr(file_name, ".tar.gz")) {
			snprintf(cmd_str, sizeof(cmd_str), "tar -xzf %s", file_name);
			pos = strstr(file_name, ".tar.gz");
		} else {
			snprintf(cmd_str, sizeof(cmd_str), "tar -xf %s", file_name);
			pos = strstr(file_name, ".tar");
		}
		
	}
	else
	{
		if (access(unzip_path, W_OK) != 0)
		{
			if(Mkdir(unzip_path) != 0) 
			{
				printf(" Create path %s failed.", unzip_path);
				return -1;
			}
		}

		if (strstr(file_name, ".tar.gz")) {
			snprintf(cmd_str, sizeof(cmd_str), "tar -xzf %s -C %s", file_name, unzip_path);
			pos = strstr(file_name, ".tar.gz");
		} else {
			snprintf(cmd_str, sizeof(cmd_str), "tar -xf %s -C %s", file_name, unzip_path);
			pos = strstr(file_name, ".tar");
		}
	}

	ret = system_call(cmd_str); 
	if(ret == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd_str);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}

	if (pos != NULL) {
		strncpy(file_dir, file_name, pos-file_name);
	}
	snprintf(cmd_str, sizeof(cmd_str), "chmod -R +x %s", file_dir); 
	if(system_call(cmd_str) == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd_str);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}

	return ret;	
}

int ChmodDir(const char *dir) {
	if (NULL == dir) {
		return -1;
	}
	printf("chmod -R 777 %s\r\n", dir);

	char cmd[512] = {0};
	snprintf(cmd, sizeof(cmd), "chmod -R 777 %s", dir);
	int ret = system_call(cmd); 
	if (ret == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd);		
	}

	return ret;
}

unsigned long GetFileSizeInByte(char *file_name)
{
    FILE *fp = fopen(file_name, "r");
    if (NULL == fp)
    {
        printf("FOpen file %s failed.\n", file_name);
        return 0;
    }

    unsigned long file_size = 0;
    fseeko(fp, 0, SEEK_END);
	file_size = ftello(fp);
    fseeko(fp, 0, SEEK_SET);

	fclose(fp);
    return file_size;
}

int BackUpFile(const char* file_name)
{
	int ret = 0;
	char cmd_str[MAX_FILE_NAME_LEN] = {0};
	
	if (file_name == NULL) {
		printf("Input error, file_name is NULL.\n");
		return -1;
	}

	//step1: remove the old backup file
	snprintf(cmd_str, sizeof(cmd_str), "rm -rf %s_*_bak", file_name);
	ret = system_call(cmd_str); 
	if(ret == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd_str);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}
	memset(cmd_str, 0, sizeof(cmd_str));

	//step2: backup file
	snprintf(cmd_str, sizeof(cmd_str), "cp -rf %s %s_`(date +%%Y-%%m-%%d)`_bak", file_name, file_name);
	ret = system_call(cmd_str); 
	if(ret == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd_str);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}

	return ret;	
}

int RemoveFile(const char* file_name)
{
	int ret = 0;
	char cmd_str[MAX_FILE_NAME_LEN] = {0};
	
	if (file_name == NULL) {
		printf("Input error, file_name is NULL.\n");
		return -1;
	}

	if (access(file_name, W_OK) != 0)
	{
		printf("Can not find file:%s.\n", file_name);
		return -1;
	}

	snprintf(cmd_str, sizeof(cmd_str), "rm -rf %s", file_name);
	ret = system_call(cmd_str); 
	if(ret == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd_str);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}

	return ret;	
}

int RebootSystem()
{
	char *cmd_str = "reboot now";
	int ret = system_call(cmd_str); 
	if (ret == 0) {
		printf(" system_call exec success : %s\r\n", cmd_str);
	} else {
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}
	return ret;
}

int GetLocalIp(char* ifname, char* ip, int ipLen)
{
    char *temp = NULL;
    int inet_sock;
    struct ifreq ifr;

    memset(ip, 0, ipLen);
    
    inet_sock = socket(AF_INET, SOCK_DGRAM, 0); 
    if(-1 == inet_sock) {   
        return -1;     
    } 

    memset(ifr.ifr_name, 0, sizeof(ifr.ifr_name));
    memcpy(ifr.ifr_name, ifname, strlen(ifname));

    if(0 != ioctl(inet_sock, SIOCGIFADDR, &ifr)) {   
        return -1;
    }

    temp = inet_ntoa(((struct sockaddr_in*)&(ifr.ifr_addr))->sin_addr);     
    memcpy(ip, temp, strlen(temp));

    close(inet_sock);
    return 0;
}

int GetExeAbsolutePath(char* path,
					   size_t sizeof_path)
{
	if (NULL == path) {
		return -1;
	}
	char* path_end;
	if (readlink("/proc/self/exe", path, sizeof_path) <= 0) {
		return -1;
	}

	path_end = strrchr(path,  '/');
	if (path_end == NULL) {
		return -1;
	}

	++path_end;
	*path_end = '\0';
	return (int)(path_end - path);
}

long int GetTimestamp()
{
 	struct timeval now;
    gettimeofday(&now, NULL);
	return (now.tv_sec*1000 + now.tv_usec/1000);
}

unsigned long GetAvailDiskSize(const char* path) {
	DISK diskInfo;
	unsigned long avail = 0;

	if (path == NULL) {
		return -1;
	}

	if (statfs(path, &diskInfo) == -1) {
		printf("Get diskInfo statfs failed.\n");
		return -1;
	}

	avail = (diskInfo.f_bsize * diskInfo.f_bavail)/1024;
	printf("Get diskInfo statfs, avail:%ld.\n", avail);
	return avail;                                   
}

int ReadDirList(const char* dir_path, DirList* dir_list) {
	DIR *dir;
	struct dirent *ptr;
	int cnt = 0;

	if ((dir = opendir(dir_path)) == NULL) {
		printf("Open directory:%s err.\n", dir_path);
		return -1; 
	}

	while ((ptr = readdir(dir)) != NULL) {
		if (strcmp(ptr->d_name, ".") == 0 || strcmp(ptr->d_name, "..") == 0) {
			continue;
		} else if (ptr->d_type == 8) { // file
			continue;
		} else if (ptr->d_type == 10) { // link file
			continue;
		} else if (ptr->d_type == 4) { // dir
			sprintf(dir_list->dir_name[cnt], "%s/%s", dir_path, ptr->d_name);
			cnt++;
		}
	}

	closedir(dir);
	dir_list->dir_cnt = cnt;
	return cnt;
}

int ReadDirListRecurve(const char* dir_path, DirList* dir_list, int max_depth) {
	DIR *dir;
	struct dirent *ptr;
	int cnt = 0;
	int dir_depth = 0;
	int j = 0;

	if ((dir = opendir(dir_path)) == NULL) {
		printf("Open directory:%s err.\n", dir_path);
		return -1; 
	}

	while ((ptr = readdir(dir)) != NULL && cnt < 500) {
		if (strcmp(ptr->d_name, ".") == 0 || strcmp(ptr->d_name, "..") == 0) {
			continue;
		} else if (ptr->d_type == 8) { // file
			sprintf(dir_list->dir_name[cnt], "%s/%s", dir_path, ptr->d_name);
			dir_list->dir_type[cnt] = 8;
			cnt++;
		} else if (ptr->d_type == 10) { // link file
			continue;
		} else if (ptr->d_type == 4) { // dir
			sprintf(dir_list->dir_name[cnt], "%s/%s", dir_path, ptr->d_name);
			dir_list->dir_type[cnt] = 4;
			cnt++;			
			if (dir_depth < max_depth) {
				dir_depth++;
				DirList sub_dir_list = {0};
				ReadDirListRecurve(dir_list->dir_name[cnt-1], &sub_dir_list, max_depth-dir_depth);
				for (j = 0; j < sub_dir_list.dir_cnt; j++) {
					if (cnt >= 500) {
						break;
					}
					sprintf(dir_list->dir_name[cnt], "%s", sub_dir_list.dir_name[j]);
					dir_list->dir_type[cnt] = sub_dir_list.dir_type[j];
					cnt++;
				}
				dir_depth--;
			}
		}
	}

	closedir(dir);
	dir_list->dir_cnt = cnt;
	return cnt;
}

int GetOSInfo(char* os_name, char* os_version) {
	if (os_name == NULL || os_version == NULL) {
		printf("Input err, ptr is NULL.\n");
		return -1;
	}

	struct utsname os_info;
	if (uname(&os_info)) {
		printf("uname err.\n");
		return -1;
	}
	
	snprintf(os_name, OS_INFO_LEN, "%s", os_info.sysname);
	snprintf(os_version, OS_INFO_LEN, "%s", os_info.release);

	return 0;
}

int RenameFile(const char* src_file_name, const char* dst_file_name)
{
	int ret = 0;
	char cmd_str[MAX_FILE_NAME_LEN] = {0};
	
	if (src_file_name == NULL || dst_file_name == NULL) {
		printf("Input error, file_name is NULL.\n");
		return -1;
	}

	snprintf(cmd_str, sizeof(cmd_str), "mv %s %s", src_file_name, dst_file_name);
	ret = system_call(cmd_str); 
	if(ret == 0)
	{
		printf(" system_call exec success : %s\r\n", cmd_str);
	}
	else
	{
		printf(" system_call exec failed, error %d : %s\r\n", errno, cmd_str);		
	}

	return ret;	
}

int GetMemoryInfo(MemInfo* mem) {
	if (mem == NULL) {
		return -1;
	}

	FILE* fd;
	char buf[1024];
	long mem_free;
	fd = fopen("/proc/meminfo", "r");
	if (fd == NULL) {
		return -1;
	}

	char name_tmp[128] = {0};
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld", name_tmp, &mem->total);
	}
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld", name_tmp, &mem_free);
	}
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld", name_tmp, &mem->avail);
	}

	fclose(fd);
	return 0;
}

float GetMemoryUnUseRate() {
	long mem_total;
	long mem_free;
	long mem_avail;
	FILE* fd;
	char buf[1024];
	float unuse_rate = 0;
	fd = fopen("/proc/meminfo", "r");
	if (fd == NULL) {
		return -1;
	}

	char name_tmp[128] = {0};
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld", name_tmp, &mem_total);
	}
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld", name_tmp, &mem_free);
	}
	if (fgets(buf, sizeof(buf), fd) != NULL) {
    	sscanf(buf, "%s %ld", name_tmp, &mem_avail);
    }
	fclose(fd);

	unuse_rate = (float)(mem_avail) / (float)(mem_total);
	return unuse_rate;
}

int GetDiskInfo(char* dir_path, DiskInfo* disk) {
	if (dir_path == NULL || disk == NULL) {
		return -1;
	}

	FILE* pipe;
	char buf[1024] = {0};
	char cmd[128] = {0};
	snprintf(cmd, sizeof(cmd), "df %s", dir_path);

	pipe = popen(cmd, "r");
	if (pipe == NULL) {
		return -1;
	}

	char file_sys[32] = {0};
	char block[32] = {0};
	char used[32] = {0};
	char avail[32] = {0};
	char use_percent[32] = {0};
	char mount[32] = {0};
	long used_size = 0;

	if (fgets(buf, sizeof(buf), pipe) != NULL) {
		sscanf(buf, "%s %s %s %s %s %s", file_sys, block, used, avail, use_percent, mount);
	}
	if (fgets(buf, sizeof(buf), pipe) != NULL) {
		sscanf(buf, "%s %ld %ld %ld %s %s", file_sys, &disk->total, &used_size, &disk->avail, use_percent, mount);
	}

	pclose(pipe);
	return 0;
}

float GetDiskUnuseRate(char* dir_path) {
	if (dir_path == NULL) {
		return -1;
	}

	FILE* pipe;
	char buf[1024] = {0};
	char cmd[128] = {0};
	snprintf(cmd, sizeof(cmd), "df %s", dir_path);

	pipe = popen(cmd, "r");
	if (pipe == NULL) {
		return -1;
	}

	char file_sys[32] = {0};
	char block[32] = {0};
	char used[32] = {0};
	char avail[32] = {0};
	char use_percent[32] = {0};
	char mount[32] = {0};
	float unuse_rate = 0;
	long block_size = 0;
	long used_size = 0;
	long avail_size = 0;

	if (fgets(buf, sizeof(buf), pipe) != NULL) {
		sscanf(buf, "%s %s %s %s %s %s", file_sys, block, used, avail, use_percent, mount);
	}
	if (fgets(buf, sizeof(buf), pipe) != NULL) {
		sscanf(buf, "%s %ld %ld %ld %s %s", file_sys, &block_size, &used_size, &avail_size, use_percent, mount);
	}
	unuse_rate = (float)avail_size / (float)block_size;
	pclose(pipe);
	
	return unuse_rate;
}

int GetBandwidthInfo(BandwidthInfo* bandwidth) {
	if (bandwidth == NULL) {
		return -1;
	}

	FILE* pipe;
	char buf[1024] = {0};
	char cmd[128] = {0};
	snprintf(cmd, sizeof(cmd), "ethtool eth0| grep Speed");

	pipe = popen(cmd, "r");
	if (pipe == NULL) {
		return -1;
	}

	char speed_tmp[32] = {0};

	if (fgets(buf, sizeof(buf), pipe) != NULL) {
		sscanf(buf, "%s %s", speed_tmp, bandwidth->total);
	}

	pclose(pipe);
	return 0;
}

int GetCPUIdleRate(CpuInfo* cpu) {
	int idle_rate = 0;
	FILE* fd;
	char buf[1024];
	fd = fopen("/proc/stat", "r");
	if (fd == NULL) {
		return -1;
	}

	char cpu_name[16] = {0};
	char other[128] = {0};
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld %ld %ld %ld %ld %ld %ld %s", cpu_name, &cpu->user, &cpu->nice, &cpu->system, &cpu->idle, &cpu->iowait, &cpu->irq, &cpu->softirq, other);
	} else {
		fclose(fd);
		return -1;
	}
	idle_rate = ((float)cpu->idle / (float)(cpu->user + cpu->nice + cpu->system + cpu->idle + cpu->iowait + cpu->irq + cpu->softirq)) * 100;

	fclose(fd);
	return idle_rate;
}

int ProcessExists(pid_t pid) {
    // 使用特殊进程号0来检查进程是否存在
    if (kill(pid, 0) == -1) {
        // 如果kill函数返回-1，则进程不存在
        return 0; // 不存在
    }
	
    return 1; // 存在
}

int GetNetInfo(NetTransInfo* net_info) {
	FILE* fd;
	char buf[1024];
	char net_name[32];
	long receive = 0;
	long transimit = 0;

	if (net_info == NULL) {
		return -1;
	}

	fd = fopen("/proc/net/dev", "r");
	if (fd == NULL) {
		return -1;
	}

	/*
	Inter-|   Receive                                                |  Transmit
 	face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
  	eth1: 8731341340589 6368156988    0 878783    0     0          0   1320622 4523577265725 1125774802    0    0    0     0       0          0
  	usb0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
    lo: 89463530 1208870    0    0    0     0          0         0 89463530 1208870    0    0    0     0       0          0
	*/
	//read first line
	fgets(buf, sizeof(buf), fd);
	//read second line
	fgets(buf, sizeof(buf), fd);
	//read realtime net info
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %ld %ld %ld %ld %ld %ld %ld %ld %ld", 
			net_name, &receive, &transimit, &transimit, &transimit, &transimit, &transimit, &transimit, &transimit, &transimit);

	} else {
		fclose(fd);
		return -1;
	}

	net_info->recveive = receive;
	net_info->transimit = transimit;
	fclose(fd);
	return 0;
}

int GetProcNetInfo(int pid, NetTransInfo* net_info) {
	FILE* fd;
	char buf[1024];
	char filename[64] = {0};
	char net_name[32];
	int receive = 0;
	int transimit = 0;

	if (net_info == NULL) {
		return -1;
	}
	
	sprintf(filename, "/proc/%d/net/dev", pid);
	fd = fopen(filename, "r");
	if (fd == NULL) {
		return -1;
	}

	/*
	Inter-|   Receive                                                |  Transmit
 	face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
  	eth1: 8731341340589 6368156988    0 878783    0     0          0   1320622 4523577265725 1125774802    0    0    0     0       0          0
  	usb0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
    lo: 89463530 1208870    0    0    0     0          0         0 89463530 1208870    0    0    0     0       0          0
	*/
	//read first line
	fgets(buf, sizeof(buf), fd);
	//read second line
	fgets(buf, sizeof(buf), fd);
	//read realtime net info
	if (fgets(buf, sizeof(buf), fd) != NULL) {
		sscanf(buf, "%s %d %d %d %d %d %d %d %d %d", 
			net_name, &receive, &transimit, &transimit, &transimit, &transimit, &transimit, &transimit, &transimit, &transimit);
	} else {
		fclose(fd);
		return -1;
	}

	net_info->recveive = receive;
	net_info->transimit = transimit;
	fclose(fd);
	return 0;
}

int GetProcMem(int pid) {
    FILE *fd;
    char filename[256] = {0};
    char buff[512];
    char name[128];
    char value[256];
    char *ret = NULL;
    int size = -1;

    sprintf(filename, "/proc/%d/status", pid);
    fd = fopen(filename, "r");
    if (fd == NULL) {
        printf("Open file failed: %s, pid = %d.\n", filename, pid);
        return -1;
    }

    while (1) {
        memset(name, 0, 128);
        memset(value, 0, 256);
        ret = fgets(buff, sizeof(buff), fd);
        if (ret == NULL) break;
        sscanf(buff, "%s %s\n", name, value);
        // printf("pid = %d, name = %s, value = %s.", pid, name, value);
        if (strcmp(name, "VmRSS:") == 0) {
            size = atoi(value);
            break;
        }
    }

    fclose(fd);  //关闭文件fd
    return size;
}
