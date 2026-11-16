add_library(WorkflowMgr
            WorkflowMgr.cpp
            WorkflowMgrWrapper.cpp)

target_include_directories(WorkflowMgr PUBLIC
        "${Logger_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include"
        "${Config_SOURCE_DIR}/include"
        "${Timer_SOURCE_DIR}/include"
        "${MD5_SOURCE_DIR}/include"
        "${cJSON_SOURCE_DIR}"
        "${WorkflowMgr_SOURCE_DIR}/include"
        "${Protocol_SOURCE_DIR}/include"
        "${CMAKE_THIRDLIB_OUTPUT_DIR}/include")

# install target
comm_install()