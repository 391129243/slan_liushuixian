add_library(Timer
        Timer.c)

target_include_directories(Timer PUBLIC
        "${Timer_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include")

# install target
comm_install()
