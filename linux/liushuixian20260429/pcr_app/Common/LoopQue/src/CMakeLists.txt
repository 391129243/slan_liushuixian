add_library(LoopQue
        LoopQueue.c)

target_include_directories(LoopQue PUBLIC
        "${LoopQue_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include")

# install target
comm_install()