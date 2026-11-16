
add_executable(PcrMgrApp 
            main.c
            MsgHandler.c
            CmdHandler.c)

target_include_directories(PcrMgrApp PUBLIC
        "${PcrMgrApp_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${MsgQue_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${AsyncCond_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${Config_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include"
        "${Timer_SOURCE_DIR}/include"
        "${DataComm_SOURCE_DIR}/include"
        "${Protocol_SOURCE_DIR}/include"
        "${WorkflowMgr_SOURCE_DIR}/include"
        "${FileManager_SOURCE_DIR}/include")

target_link_libraries(PcrMgrApp
    WorkflowMgr
    DataComm
    Protocol
    Logger
    MsgQue
    LoopQue
    Config
    Timer 
    System
    AsyncCond
    cJSON
    pthread
    ssl
    crypto
    MD5
    FileManager)

# install target
comm_install()
