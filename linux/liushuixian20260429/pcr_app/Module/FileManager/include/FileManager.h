#ifndef __FILE_MANAGER_H__
#define __FILE_MANAGER_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include "Common.h"

#define MAX_MGR_FILE_TYPE_NUM 3
#define MAX_NEW_FILES_NUM 20

typedef enum eMgrFileType {
    MGR_FILE_TYPE_RNA_APP_LOG,
    MGR_FILE_TYPE_PCR_APP_LOG,
    MGR_FILE_TYPE_PCR_RESULT,
    MGR_FILE_TYPE_END
}MgrFileType;

typedef struct sFileMgrCfg
{
    MgrFileType file_type;
    char file_dir[MAX_FILE_NAME_LEN];
    char prefix[MAX_FILE_NAME_LEN];
    int clean_time; //day
    long clean_threshold; //MB
}FileMgrCfg;

typedef struct sFileManager
{
    float disk_used_rate_threshold;
    int type_num;
    FileMgrCfg mgr_cfg[MAX_MGR_FILE_TYPE_NUM];
}FileManager;

typedef struct sDownloadHeader {
    unsigned int file_id;
    unsigned int file_size;
	int file_type;
    char file_dst[MAX_FILE_NAME_LEN];
    char file_url[MAX_URL_LEN];
    char file_name[MAX_FILE_NAME_LEN];
	char file_checksum[64];
} DownloadHeader;

int InitFileManager();

int DeinitFileManager();

int ReadFileMgrCfg(FileManager *pCfg);

int SetFileMgrCfg(FileManager *pCfg);

int SaveFileManagerFile(FileManager *pCfg);

#ifdef __cplusplus
}
#endif

#endif
