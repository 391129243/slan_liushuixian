
#ifndef __LOOP_QUEUE_UNLOCK_H_
#define __LOOP_QUEUE_UNLOCK_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <time.h> 
#include <sys/msg.h>
#include <sys/ipc.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <errno.h>
#include <pthread.h>  
#include <semaphore.h>
#include <fcntl.h>
#include <sys/socket.h>   

#include <sys/shm.h>

#include <stdio.h>
#include <stdarg.h>
#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef BOOL
#define BOOL int
#define FALSE 0
#define TRUE 1
#endif

#define MAX_DATA_QUEUE_LEN 600

typedef int (* compare_func_t)(void *que_val, void *pass_val);
typedef int (* printf_func_t)(void *val);

typedef struct stLoopQueue
{
    void** pBuff; //动态分配的数据缓冲区
    int front;    //指向队列第一个元素
    int rear;    //指向队列最后一个元素的下一个元素
    int maxsize; //循环队列的最大存储空间
    int len;
    compare_func_t comp_f;
    printf_func_t  printf_f;
}LoopQueue,*PLoopQueue;

extern void InitQueue(PLoopQueue Q, int maxsize);
extern void InitOrderQueue(PLoopQueue Q, int maxsize, compare_func_t _comp_f, printf_func_t _printf_f);
extern void PrintQueue(PLoopQueue Q);
extern BOOL IsFullQueue(PLoopQueue Q);
extern BOOL IsEmptyQueue(PLoopQueue Q);
extern BOOL InQueue(PLoopQueue Q, void* val);
extern void* OutQueue(PLoopQueue Q);
extern int GetQueueLen(PLoopQueue Q);
extern void UninitQueue(PLoopQueue Q);
void* PeekQueue(PLoopQueue Q);

#ifdef __cplusplus
}
#endif


#endif



