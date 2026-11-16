function(comm_install)
    install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION app/bin
        LIBRARY DESTINATION app/lib
        ARCHIVE DESTINATION lib_static)
endfunction()

# Set library and header files install directory
#SET(CMAKE_INSTALL_PREFIX     "${CMAKE_INSTALL_PREFIX}/lib")#
