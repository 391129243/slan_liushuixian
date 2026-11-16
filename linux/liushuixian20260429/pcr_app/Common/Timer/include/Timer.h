#ifndef __TIMER_H__
#define __TIMER_H__

#include <stdbool.h>
#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct stTimerMsgInfo {
	long timer_len; 
    void (* timeout_handler)(void *args); // 超时处理函数
	void *handler_args;
    bool quit_flag;
} TimerMsgInfo, *PTimerMsgInfo;

void MillisecondsSleep(unsigned long mSec);

void MicrosecondsSleep(unsigned long uSec);

void SetMicrosendsTimer(unsigned long timer_len, void (* handler)(void *args), void *args);

void SetMillisecondsTimer(unsigned long timer_len, void (* handler)(void *args), void *args);

PTimerMsgInfo ResetMillisecondsTimer(unsigned long timer_len, void (* handler)(void *args), void *args, pthread_t* timer_thread);

#ifdef __cplusplus
}
#endif

#endif
