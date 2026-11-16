#ifndef __ASYNC_COND_H__
#define __ASYNC_COND_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <pthread.h>

typedef void* AsyncCond;

typedef struct sAsyncCondCtx {
	pthread_mutex_t lock;
	pthread_cond_t  cond;
    char name[60];
} AsyncCondCtx;

AsyncCond CreateAsyncCond(const char *cond_name);

void DestroyAsyncCond(AsyncCond cond);

int WaitAsyncCond(AsyncCond cond, unsigned int timeout);

int SignalAsyncCond(AsyncCond cond, void (*sig_act)(const char *cond_name, void *param), void *param);

#ifdef __cplusplus
}
#endif

#endif