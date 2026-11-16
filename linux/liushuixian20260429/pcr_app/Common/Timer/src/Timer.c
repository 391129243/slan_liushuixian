#include "Common.h"
#include "Timer.h"

void MillisecondsSleep(unsigned long mSec)
{
    struct timeval tv;
    tv.tv_sec = mSec/1000;
    tv.tv_usec = (mSec%1000)*1000;
    int err;
    do {
       err = select(0, NULL, NULL, NULL, &tv);
    } while((err < 0) && (errno == EINTR));
}

void MicrosecondsSleep(unsigned long uSec)
{
    struct timeval tv;
    tv.tv_sec = uSec/1000000;
    tv.tv_usec = uSec%1000000;
    int err;
    do {
        err = select(0, NULL, NULL, NULL, &tv);
    } while((err < 0) && (errno == EINTR));
}

//微秒级高精度定时线程
void* MicrosendsTimerThread(void* arg)
{
	unsigned long timer_len = 0;
	PTimerMsgInfo pInfo = NULL;

	if(arg == NULL) {
        pthread_exit((void *)0); 
    }

	pInfo = (PTimerMsgInfo)arg;
	timer_len = pInfo->timer_len;
	
	while(1) {
		MicrosecondsSleep(timer_len);
		if (NULL != pInfo->timeout_handler) {
            pInfo->timeout_handler(pInfo->handler_args);
        }
	}

	free(pInfo);		
	pthread_exit((void *)0); 
}

void SetMicrosendsTimer(unsigned long timer_len, void (* handler)(void *args), void *args)
{
	pthread_t timer_thread; 
	PTimerMsgInfo pInfo = NULL;

    if (NULL == handler) {
        return;
    }

	pInfo = (PTimerMsgInfo)malloc(sizeof(TimerMsgInfo));
	if (pInfo == NULL) {
		printf("timer_common Create microseconds timer thread failed for memory. \r\n");	
		return;
	}
	
	pInfo->timer_len = timer_len;
    pInfo->timeout_handler = handler;
	pInfo->handler_args = args;

	if (pthread_create(&timer_thread, NULL, MicrosendsTimerThread, pInfo) != 0) {
		printf("timer_common Create microseconds timer thread failed. \r\n");			
	}
}

//毫秒级高精度定时线程
void* MillisecondsTimerThread(void* arg)
{
	unsigned long timer_len = 0;
	PTimerMsgInfo pInfo = NULL;

	if(arg == NULL) {
        pthread_exit((void *)0); 
    }

	pInfo = (PTimerMsgInfo)arg;
	timer_len = pInfo->timer_len;

	while(1) {
		MillisecondsSleep(timer_len);
        if (NULL != pInfo->timeout_handler) {
            pInfo->timeout_handler(pInfo->handler_args);
        }
	}

	free(pInfo);		
	pthread_exit((void *)0); 
}

//设置毫秒级定时器
void SetMillisecondsTimer(unsigned long timer_len, void (* handler)(void *args), void *args)
{
	pthread_t timer_thread; 
	PTimerMsgInfo pInfo = NULL;

    if (NULL == handler) {
        return;
    }

	pInfo = (PTimerMsgInfo)malloc(sizeof(TimerMsgInfo));
	if (pInfo == NULL) {
		printf("timer_common Create milliseconds timer thread failed for memory. \r\n");	
		return;
	}
	
	pInfo->timer_len = timer_len;
    pInfo->timeout_handler = handler;
	pInfo->handler_args = args;

	if (pthread_create(&timer_thread, NULL, MillisecondsTimerThread, pInfo) != 0) {
		printf("timer_common Create milliseconds timer thread failed. \r\n");			
	}
}
 
void* ResetMillisecondsTimerThread(void* arg)
{
	unsigned long timer_len = 0;
	PTimerMsgInfo pInfo = NULL;

	if(arg == NULL) {
        pthread_exit((void *)0); 
    }

	pInfo = (PTimerMsgInfo)arg;
	timer_len = pInfo->timer_len;
	while(pInfo && !pInfo->quit_flag) {
		MillisecondsSleep(timer_len);
        if (pInfo && NULL != pInfo->timeout_handler) {
            pInfo->timeout_handler(pInfo->handler_args);
        }
	}

	if (pInfo) {
		free(pInfo);
	}	
	pthread_exit((void *)0); 
}

PTimerMsgInfo ResetMillisecondsTimer(unsigned long timer_len, void (* handler)(void *args), void *args, pthread_t* timer_thread)
{
	PTimerMsgInfo pInfo = NULL;

    if (NULL == handler) {
        return NULL;
    }

	pInfo = (PTimerMsgInfo)malloc(sizeof(TimerMsgInfo));
	if (pInfo == NULL) {
		printf("timer_common Create milliseconds timer thread failed for memory. \r\n");	
		return NULL;
	}
	
	pInfo->timer_len = timer_len;
    pInfo->timeout_handler = handler;
	pInfo->handler_args = args;
	pInfo->quit_flag = 0;

	if (pthread_create(timer_thread, NULL, ResetMillisecondsTimerThread, pInfo) != 0) {
		printf("timer_common Create milliseconds timer thread failed. \r\n");			
	}

	return pInfo;
}

