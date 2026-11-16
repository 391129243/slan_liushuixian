
add_executable(SysDaemonApp 
            main.c)

target_include_directories(SysDaemonApp PUBLIC
        "${SysDaemonApp_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${MsgQue_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${Config_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include")

target_link_libraries(SysDaemonApp
    Logger
    MsgQue
    Config
    Timer 
    cJSON
    pthread
    System
    )

# install target
comm_install()
