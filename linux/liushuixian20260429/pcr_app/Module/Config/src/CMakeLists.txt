
add_library(Config
        ConfSrv.c
        ConfLocal.c
        ConfLog.c)

target_include_directories(Config PUBLIC
        "${Config_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include"
        "${Timer_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${CMAKE_THIRDLIB_OUTPUT_DIR}/include")

# install target
comm_install()
