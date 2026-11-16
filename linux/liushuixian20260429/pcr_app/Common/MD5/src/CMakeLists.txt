add_library(MD5
        md5.c)

target_include_directories(MD5 PUBLIC
        "${MD5_SOURCE_DIR}/include"
        "${Common_SOURCE_DIR}/include")

# install target
comm_install()
