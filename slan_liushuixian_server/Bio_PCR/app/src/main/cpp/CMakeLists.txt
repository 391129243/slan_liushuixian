#so库的输出路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

#版本
cmake_minimum_required(VERSION 3.4.1)



#1. 添加自己的so库test-lib,设置一个名字，输出就是这个名字
add_library( # Sets the name of the library.
        BoweiJni

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        BoweiJni.cpp)


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

#add 库的名字和target的要一致，因为target链接add的文件
target_link_libraries( # Specifies the target library.
        BoweiJni

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})


