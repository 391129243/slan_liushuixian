add_library(FileManager
        FileManager.c)

target_include_directories(FileManager PUBLIC
        "${FileManager_SOURCE_DIR}/include"
        "${Logger_SOURCE_DIR}/include"
        "${System_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Config_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${Timer_SOURCE_DIR}/include")

# install target
comm_install()