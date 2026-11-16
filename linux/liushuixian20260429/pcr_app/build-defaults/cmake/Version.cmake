

string(TOUPPER "${CMAKE_BUILD_TYPE}" buildType)

if ("${buildType}" STREQUAL "DEBUG")
    message("SOFTWARE_VERSION=${SOFTWARE_VERSION}")
    add_definitions(
            -DSOFTWARE_VERSION="${SOFTWARE_VERSION}"
    )
endif()

if ("${buildType}" STREQUAL "RELEASE")
    message("SOFTWARE_VERSION=${SOFTWARE_VERSION}")
    add_definitions(
            -DSOFTWARE_VERSION="${SOFTWARE_VERSION}"
    )
endif()
