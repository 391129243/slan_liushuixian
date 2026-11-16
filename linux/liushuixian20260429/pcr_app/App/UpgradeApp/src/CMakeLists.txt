link_directories("${CMAKE_THIRDLIB_OUTPUT_DIR}/lib")

add_executable(UpgradeApp 
                main.c
                UpgradeMsgHandler.c)

target_include_directories(UpgradeApp PUBLIC
        "${UpgradeApp_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${MsgQue_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include"
        "${MD5_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${Config_SOURCE_DIR}/include"    
        "${Protocol_SOURCE_DIR}/include")

target_link_libraries(UpgradeApp
        Logger
        MsgQue
        Timer
        Config
        pthread
        System
        cJSON)
  
# install target
comm_install()
