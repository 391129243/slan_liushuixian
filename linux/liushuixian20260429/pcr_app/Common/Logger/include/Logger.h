
#ifndef __LOGGER_H__
#define __LOGGER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stddef.h>
#include <string.h> //strrchr()函数所需头文件

#define filename(x) (strrchr(x,'/')?strrchr(x,'/')+1:x)


int debug_out_func_v(int level, const char *file, const char *func, int line, const char *fmt, ...);

int log_out_func_v(const char *fmt, ...);

/*
    0--关闭输出，
    1--err，
    2--warning，
    3--debuge,
    4--debugw,
    5--debug
*/
typedef enum _LoggerLevel {
    LOGGER_NONE,
    LOGGER_ERR,
    LOGGER_WARN,
    LOGGER_INFO_E,
    LOGGER_INFO_W,
    LOGGER_INFO_D,
    LOGGER_LEVEL_END,
} eLoggerLevel;

typedef enum _LoggerType {
    LOGGER_DEBUG,
    LOGGER_LOG,
} eLoggerType;

typedef void (* OutputLogFunc_t)(eLoggerType type, char *data, size_t len);

void InitLoggerLevel(eLoggerLevel level, OutputLogFunc_t output);

int SetLoggerLevel(eLoggerLevel level);

void InitLoggerModule(int level);

/* 封装带可变参数的调试函数 */
#define debug_printf(fmt, ...)  (debug_out_func_v(LOGGER_INFO_D, filename(__FILE__), __func__, __LINE__, fmt, ##__VA_ARGS__))
#define debugw_printf(fmt, ...)  (debug_out_func_v(LOGGER_INFO_W, filename(__FILE__), __func__, __LINE__, fmt, ##__VA_ARGS__))
#define debuge_printf(fmt, ...)  (debug_out_func_v(LOGGER_INFO_E, filename(__FILE__), __func__, __LINE__, fmt, ##__VA_ARGS__))

#define warn_printf(fmt, ...)  (debug_out_func_v(LOGGER_WARN, filename(__FILE__), __func__, __LINE__, fmt, ##__VA_ARGS__))
#define err_printf(fmt, ...)  (debug_out_func_v(LOGGER_ERR, filename(__FILE__), __func__, __LINE__, fmt, ##__VA_ARGS__))

#define log_printf(fmt, ...)  (log_out_func_v(fmt, ##__VA_ARGS__))

#ifdef __cplusplus
}
#endif

#endif
