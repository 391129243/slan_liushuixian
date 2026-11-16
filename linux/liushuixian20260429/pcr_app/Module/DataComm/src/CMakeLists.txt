add_library(DataComm
        mongoose.c
        Wss.c
        Uart.c)

target_include_directories(DataComm PUBLIC
        "${DataComm_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Timer_SOURCE_DIR}/include"
        "${LoopQue_SOURCE_DIR}/include"
        "${Config_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${MsgQue_SOURCE_DIR}/include")

# install target
comm_install()