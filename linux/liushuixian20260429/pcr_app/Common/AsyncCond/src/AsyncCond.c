#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <time.h>
#include <string.h>
#include <sys/time.h>
#include <sys/stat.h>  
#include <errno.h>

#include "AsyncCond.h"

AsyncCond CreateAsyncCond(const char *cond_name)
{
    AsyncCondCtx *ctx = malloc(sizeof(AsyncCondCtx));
    if (NULL == ctx) {
        return NULL;
    }

    memset(ctx, 0, sizeof(AsyncCondCtx));
    pthread_mutex_init(&(ctx->lock), NULL);
    pthread_cond_init(&(ctx->cond), NULL);
    if (NULL != cond_name) {
        snprintf(ctx->name, sizeof(ctx->name), "%s", cond_name);
    }

    return (AsyncCond)ctx;
}

void DestroyAsyncCond(AsyncCond cond)
{
    if (NULL == cond) {
        return;
    }

    AsyncCondCtx *ctx = (AsyncCondCtx *)cond;
    pthread_mutex_destroy(&(ctx->lock));
    pthread_cond_destroy(&(ctx->cond));
    free(ctx);
}

int WaitAsyncCond(AsyncCond cond, unsigned int timeout)
{
    if (NULL == cond) {
        return -1;
    }

    AsyncCondCtx *ctx = (AsyncCondCtx *)cond;

	pthread_mutex_lock(&(ctx->lock));
    if (!timeout) {
        pthread_cond_wait(&(ctx->cond), &(ctx->lock));
    } else {
        struct timespec abstime;
        clock_gettime(CLOCK_REALTIME, &abstime);
        abstime.tv_sec = abstime.tv_sec + timeout;
        int ret = pthread_cond_timedwait(&(ctx->cond), &(ctx->lock), &abstime); 
        if (ret == ETIMEDOUT) {
            pthread_mutex_unlock(&(ctx->lock));
            return -1;
        }
    }

	pthread_mutex_unlock(&(ctx->lock));
    return 0;
}

int SignalAsyncCond(AsyncCond cond, 
                     void (*sig_act)(const char *cond_name, void *param),
                     void *param)
{
    if (NULL == cond) {
        return -1;
    }
    AsyncCondCtx *ctx = (AsyncCondCtx *)cond;

	pthread_mutex_lock(&(ctx->lock));
    if (NULL != sig_act) {
        sig_act(ctx->name, param);
    }
    pthread_cond_signal(&(ctx->cond));
    pthread_mutex_unlock(&(ctx->lock));
    return 0;
}
