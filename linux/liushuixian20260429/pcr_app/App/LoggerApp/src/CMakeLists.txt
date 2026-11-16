
add_executable(LoggerApp main.c)

target_include_directories(LoggerApp PUBLIC
        "${LoggerApp_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${MsgQue_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Config_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}")

target_link_libraries(LoggerApp
    Config
    cJSON
    Logger
    MsgQue    
    Timer
    System
    pthread)

# install target
comm_install()
