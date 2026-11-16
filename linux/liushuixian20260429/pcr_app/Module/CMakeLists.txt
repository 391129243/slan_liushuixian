cmake_minimum_required(VERSION 3.1 FATAL_ERROR)
project(Module LANGUAGES C)

add_subdirectory("Config")
add_subdirectory("DataComm")
add_subdirectory("Protocol")
add_subdirectory("WorkflowMgr")
add_subdirectory("FileManager")
