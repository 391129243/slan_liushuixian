add_library(Logger
        Logger.c
        LoggerWrapper.c)

target_include_directories(Logger PUBLIC
        "${Logger_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${MsgQue_SOURCE_DIR}/include")

# install target
comm_install()