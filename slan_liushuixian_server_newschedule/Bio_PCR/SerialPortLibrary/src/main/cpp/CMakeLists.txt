cmake_minimum_required(VERSION 3.5.0)

add_library(SerialPort SHARED
            SerialPort.c)

# Include libraries needed for libserial_port lib
target_link_libraries(SerialPort
                      android
                      log)