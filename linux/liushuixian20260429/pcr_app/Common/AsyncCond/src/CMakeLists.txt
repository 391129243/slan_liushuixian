add_library(AsyncCond
        AsyncCond.c)

target_include_directories(AsyncCond PUBLIC
        "${AsyncCond_SOURCE_DIR}/include")

# install target
comm_install()
