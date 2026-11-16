
#include "LoopQueue.h"
#include "Logger.h"
#include "Common.h"

//注意本循环队列无多线程互斥保护。
//如果多线程或中断处理，可能导致数据错误。需自行通过信号量或互斥量来保护

void InitQueue(PLoopQueue Q, int maxsize)
{
	int size = (sizeof(void*)) * maxsize;
	if (Q == NULL) return;

	debug_printf("LoopQueue Init  0x%x.\r\n", (void*)Q);
	
	Q->pBuff=(void**)malloc(size);	
	if(NULL == Q->pBuff)
	{
	    err_printf("LoopQueue 0x%x memory allocation failure.\r\n", (void*)Q);
	    return;
	}

	memset(Q->pBuff, 0, size);
	Q->front=0;         //初始化參数
	Q->rear=0;
	Q->maxsize=maxsize;
	Q->len = 0;
	Q->comp_f = NULL;
	Q->printf_f = NULL;
}

void InitOrderQueue(PLoopQueue Q, int maxsize, compare_func_t _comp_f, printf_func_t _printf_f)
{
	int size = (sizeof(void*)) * maxsize;
	if (Q == NULL) return;


	Q->pBuff=(void**)malloc(size);
	if(NULL == Q->pBuff)
	{
	    return;
	}

	memset(Q->pBuff, 0, size);
	Q->front = 0;         //初始化參数
	Q->rear = 0;
	Q->maxsize = maxsize;
	Q->len = 0;
	Q->comp_f = _comp_f;
	Q->printf_f = _printf_f;
}

void PrintQueue(PLoopQueue Q)
{
    int i= 0;
	int j = 0;
	if (Q == NULL) return;
	if (Q->pBuff == NULL) return;		
	
    debug_printf("LoopQueue 0x%x Len = %d.\r\n", (void*)Q, Q->len);

	i= Q->front;
    while(j < Q->len)
    {
        //printf("0x%x, ",Q->pBuff[i]);
        i++;
		i = i%(Q->maxsize);
		j++;
    }
    //printf("\r\n");
}

BOOL IsFullQueue(PLoopQueue Q)
{
	if (Q == NULL) return TRUE;
	if (Q->pBuff == NULL) return TRUE;	
	
	if (Q->len == Q->maxsize) 
	{
		return TRUE;
	}
	else
	{
		return FALSE;
	}
}

BOOL IsEmptyQueue(PLoopQueue Q)
{	
	if (Q == NULL) return TRUE;
	if (Q->pBuff == NULL) return TRUE;
	
	if (Q->len == 0) 
	{
		return TRUE;
	}
	else
	{
		return FALSE;
	}
}

BOOL InQueue(PLoopQueue Q, void* val)
{	
    if(IsFullQueue(Q))
    {
		err_printf("LoopQueue 0x%x is full, len = %d, lost data 0x%x. \r\n", (void*)Q, Q->len, val);
        return FALSE;
    }

	if (Q->pBuff == NULL) return FALSE;	

	if (!Q->comp_f)
    {
        Q->pBuff[Q->rear] = val;
    }
    else
    {
        int pos = Q->rear;
		int prev = 0;

		do {
			// empty que
			if (pos == Q->front) {
				Q->pBuff[pos] = val;
				break;
			}

			prev = pos - 1;
			if (prev < 0) {
				prev = Q->maxsize - 1;
			}

			if (Q->comp_f(Q->pBuff[prev], val)) {
				Q->pBuff[pos] = Q->pBuff[prev];
			}
			else {
				Q->pBuff[pos] = val;
				break;
			}

			pos = prev;
		} while (1);
    }

    Q->len++;
    Q->rear++;
    Q->rear = (Q->rear)%(Q->maxsize);

	debug_printf("LoopQueue 0x%x len = %d, In data 0x%x. front = %d, rear = %d.\r\n", 
		(void*)Q, Q->len, val, Q->front, Q->rear);

	if (Q->printf_f)
	{
		int i = Q->front;
		int j = 0;
		while(j < Q->len)
		{
			Q->printf_f(Q->pBuff[i]);
			i++;
			i = i%(Q->maxsize);
			j++;
		}
	}
	return TRUE;
}

void* OutQueue(PLoopQueue Q)
{
	void* val = NULL;
	
    if(IsEmptyQueue(Q))
    {
        return NULL;
    }
	
	if (Q->pBuff == NULL) return NULL;

    val = Q->pBuff[Q->front];		
	Q->len--;
	Q->front++;
    Q->front = (Q->front)%(Q->maxsize);

	debug_printf("LoopQueue 0x%x len = %d, Out data 0x%x. front = %d, rear = %d.\r\n", 
		(void*)Q, Q->len, val, Q->front, Q->rear);
	
    return val;
}

void* PeekQueue(PLoopQueue Q)
{
	void* val = NULL;
	
    if(IsEmptyQueue(Q))
    {
        return NULL;
    }
	
	if (Q->pBuff == NULL) return NULL;

    val = Q->pBuff[Q->front];		
    return val;
}

int GetQueueLen(PLoopQueue Q)
{
	if (Q == NULL) return 0;
	
	return Q->len;
}

void UninitQueue(PLoopQueue Q)
{
	if (Q == NULL) return;	

	debug_printf("LoopQueue Uninit 0x%x.\r\n", (void*)Q);
	
	if(NULL != Q->pBuff)
	{
	    free(Q->pBuff);
		Q->pBuff = NULL;
	}	

	Q->front=0;     
	Q->rear=0;
	Q->maxsize=0;
	Q->len = 0;
}

void DeinitQueue(PLoopQueue Q) {
	RawData *raw_data = NULL;
	while(1) {
		raw_data = OutQueue(Q);
		if (raw_data == NULL) {
			break;
		}

		if (raw_data->data != NULL) {
			free(raw_data->data);
		}
		free(raw_data);
	}
	UninitQueue(Q);
}
