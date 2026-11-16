add_library(MsgQue
        MsgQue.c
        MsgWrapper.c)

target_include_directories(MsgQue PUBLIC
        "${MsgQue_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Timer_SOURCE_DIR}/include")

# install target
comm_install()