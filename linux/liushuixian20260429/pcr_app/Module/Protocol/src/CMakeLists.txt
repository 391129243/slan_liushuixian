add_library(Protocol
        UartProtocol.c
        WssProtocol.c)

target_include_directories(Protocol PUBLIC
        "${Protocol_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Timer_SOURCE_DIR}/include"
        "${DataComm_SOURCE_DIR}/include"
        "${Config_SOURCE_DIR}/include"
        "${LoopQue_SOURCE_DIR}/include"
        "${MD5_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}")

# install target
comm_install()