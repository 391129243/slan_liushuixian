#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "Common.h"
#include "Logger.h"
#include "MsgWrapper.h"

#define MAX_LOG_LEN         3200

static void SendLoggerMsg(eLoggerType type, char *data, size_t len)
{
	int ret = 0;
    int msg_id = 0;
    //printf("SendLoggerMsg log length %ld \r\n", len);

	if (data == NULL) {
        return;
    }

    MsgInfo msg = {0};

	if (len > MAX_LOG_LEN) {
        len = MAX_LOG_LEN;
    }

    if (type == LOGGER_LOG) {
        msg_id = MSG_ID_LOG_OUT;
    } else {
        msg_id = MSG_ID_DEBUG_OUT;
    }

	memcpy(msg.data, data, len);
    ret = SendMsg(APP_LOGGER_ID, msg_id, &msg);
    if(ret < 0) {
        printf("send debug info msg error %d %s. \r\n", errno, strerror(errno));
    }
    return;
}

void InitLoggerModule(int level)
{
    InitLoggerLevel(level, SendLoggerMsg);
}