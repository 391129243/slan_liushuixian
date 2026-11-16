add_library(System
        System.c)

target_include_directories(System PUBLIC
        "${System_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${CMAKE_THIRDLIB_OUTPUT_DIR}/include")

# install target
comm_install()