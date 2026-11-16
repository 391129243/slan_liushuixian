cmake_minimum_required(VERSION 3.1 FATAL_ERROR)

include(./build-defaults/BuildDefaults.cmake)

project(pcr-app VERSION 0.0.1 LANGUAGES C)

add_subdirectory("ThirdParty")
add_subdirectory("Common")
add_subdirectory("Module")
add_subdirectory("App")

