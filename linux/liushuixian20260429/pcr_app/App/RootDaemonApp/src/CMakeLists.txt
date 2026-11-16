
add_executable(RootDaemonApp 
            main.c)

target_include_directories(RootDaemonApp PUBLIC
        "${Logger_SOURCE_DIR}/include"
        "${MsgQue_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include"
        "${Config_SOURCE_DIR}/include")

target_link_libraries(RootDaemonApp
    Logger
    MsgQue
    Timer 
    pthread
    Config
    System    
    cJSON)

install(TARGETS RootDaemonApp RUNTIME DESTINATION app)

# install target
comm_install()
