cmake_minimum_required(VERSION 3.1 FATAL_ERROR)
project(cJSON LANGUAGES C)

add_library(cJSON
        cJSON.c)

target_include_directories(cJSON PUBLIC
        "${cJSON_SOURCE_DIR}")

# install target
comm_install()
