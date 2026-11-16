
#include "Logger.h"
#include "Common.h"

#define DEBUG_BUF_LEN       3300
#define LOG_BUF_LEN         3200
#define LOCAL_PRINTF (20)

static int debug_level = LOGGER_WARN;
static char debug_str_head[4][10] = {"erro--", "warn--", "info--", "log-- "};

void LoggerDefaultOutput(eLoggerType type, char *data, size_t len)
{
	printf("%s\r\n", data);
}

static OutputLogFunc_t s_logger_out = (OutputLogFunc_t)LoggerDefaultOutput;

void InitLoggerLevel(eLoggerLevel level, OutputLogFunc_t output)
{
    if (level == LOCAL_PRINTF) {
		s_logger_out = LoggerDefaultOutput;
		level = LOGGER_INFO_D;
    } else {
		debug_level = level;
		if (NULL != output) {
			s_logger_out = output;
		} else {
			s_logger_out = LoggerDefaultOutput;
		}
	}
}

void SendDebugInfoMsg(char* info)
{
	if (NULL == info) {
		return;
	}
	s_logger_out(LOGGER_DEBUG, info, strlen(info));
}

int SetLoggerLevel(eLoggerLevel level) {
	if ((level <= LOGGER_NONE) || (level >= LOGGER_LEVEL_END)) {
		return -1;
	}
	debug_level = level;
	return 0;
}

int debug_out_func_v(int level_in, const char *file, const char *func, int line, const char *fmt, ...)
{
	char buf[DEBUG_BUF_LEN];	
	time_t timep;	 
	struct tm *p = NULL;
	clock_t t;
	int ms = 0;	
	int index = 0;	
	int level = 0;	
	va_list args;

	level = level_in % LOGGER_LEVEL_END;
	if (level > debug_level) {
        return 0;	
    }

	if (level == 0) {
        return 0;
    }
	
	index = level - 1;
	if (index > 2) {
        index = 2; 
    }

	timep = time(NULL);   
	p = localtime(&timep);
	t = clock();
	ms = t * 1000 / CLOCKS_PER_SEC % 1000;

	memset(buf, 0, DEBUG_BUF_LEN);	 
	sprintf(buf, "%s %04d_%02d_%02d_%02d_%02d_%02d-%03d: F: %s, L: %d, F: %s : ", 
		debug_str_head[index], (1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, 
		p->tm_hour, p->tm_min, p->tm_sec, ms, file, line, func);
	
	va_start(args, fmt);
	vsnprintf(buf + strlen(buf), DEBUG_BUF_LEN - strlen(buf), fmt, args);
	va_end(args);

    //printf("debug_out_func_v log length %ld \r\n", strlen(buf));

	s_logger_out(LOGGER_DEBUG, buf, strlen(buf));
	return 0;
}

int debug_out_func(int level_in, const char *file, const char *func, int line, const char *info)
{
	char buf[DEBUG_BUF_LEN];
	struct timespec time_s;	
	int index = 0;
	int level = 0;

	if (info == NULL) return 0;

	level = level_in%LOGGER_LEVEL_END;
	if (level > debug_level) return 0;
	if (level == 0) return 0;

	index = level_in - 1;
	if (index > 2) index = 2; 

	clock_gettime(CLOCK_REALTIME, &time_s);  //获取相对于1970到现在的秒数
	
	memset(buf, 0, DEBUG_BUF_LEN);	   
	sprintf(buf, "%s %d.%03d F: %s, L: %d, F: %s : %s", 
		debug_str_head[index], (int)time_s.tv_sec, (int)(time_s.tv_nsec)/1000000,
		file, line, func, info);

	s_logger_out(LOGGER_DEBUG, buf, strlen(buf));
	return 0;
}

int log_out_func_v(const char *fmt, ...)
{
	char buf[LOG_BUF_LEN];
	time_t timep;	 
	struct tm *p = NULL; 	
	va_list args;

	memset(buf, 0, LOG_BUF_LEN);	 

	timep = time(NULL);   
	p = localtime(&timep);
	sprintf(buf, "%04d_%02d_%02d_%02d_%02d_%02d: ",
		(1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, 
		p->tm_hour, p->tm_min, p->tm_sec);	
	
	/* 可变参的处理 */
	va_start(args, fmt);
	vsnprintf(buf + strlen(buf), LOG_BUF_LEN - strlen(buf), fmt, args);
	va_end(args);

	strcpy(buf + strlen(buf), "\r\n");

	s_logger_out(LOGGER_LOG, buf, strlen(buf));
	return 0;
}

void SendLogInfoMsg(char* info)
{
	if (NULL == info) {
		return;
	}
	s_logger_out(LOGGER_LOG, info, strlen(info));
}

int log_out_func(const char *info)
{
	char buf[LOG_BUF_LEN];	
	time_t timep;	 
	struct tm *p = NULL; 

	if (info == NULL) return -1;

	memset(buf, 0, LOG_BUF_LEN);	 

	timep = time(NULL);   
	p = localtime(&timep);
	sprintf(buf, "%04d_%02d_%02d_%02d_%02d_%02d: %s\r\n",
		(1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, 
		p->tm_hour, p->tm_min, p->tm_sec, info);	
	
	s_logger_out(LOGGER_LOG, buf, strlen(buf));
	return 0;
}
