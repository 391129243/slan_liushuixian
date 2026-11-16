cmake_minimum_required(VERSION 3.1 FATAL_ERROR)
project(App LANGUAGES C)

add_subdirectory("RootDaemonApp")
add_subdirectory("SysDaemonApp")
add_subdirectory("PcrMgrApp")
add_subdirectory("LoggerApp")
add_subdirectory("UpgradeApp")
