#
# Setup the build type and compiler options.
#
# To set the build type, run the following command with a build type of DEBUG, RELEASE:
#     cmake <path-to-source> -DCMAKE_BUILD_TYPE=<build-type>
#

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Choose the type of build, options are: DEBUG, RELEASE, or MINSIZEREL." FORCE)
    message("No build type specified, defaulting to Debug.")
endif()

# Verify the build type is valid.
set(buildTypes DEBUG RELEASE)

string(TOUPPER "${CMAKE_BUILD_TYPE}" buildType)

list(FIND buildTypes "${buildType}" buildTypeFound)
if (buildTypeFound EQUAL -1)
    string(LENGTH "${CMAKE_BUILD_TYPE}" buildTypeLen)
    math(EXPR buildTypePadLen "72 - ${buildTypeLen}")
    if (buildTypePadLen GREATER 0)
        string(RANDOM LENGTH "${buildTypePadLen}" ALPHABET " " buildTypePad)
    endif()

    message("###############################################################################")
    message("#                                                                             #")
    message("# ERROR:                                                                      #")
    message("# Unknown build type selected. Please select from DEBUG, RELEASE              #")
    message("#                                                                             #")
    message("# Build Type:                                                                 #")
    message("#     ${buildType}${buildTypePad}#")
    message("#                                                                             #")
    message("###############################################################################")

    message(FATAL_ERROR "Unknown build type ${buildType}. Please select from DEBUG, RELEASE. Quitting!")
else()
    message("Creating the build directory for the ${PROJECT_NAME} with build type: ${buildType}")
endif()

# Set up the compiler.


if (PLATFORM_LINUX_HOST)
    message("${PROJECT_NAME} PLATFORM_LINUX_HOST compile.")
elseif (PLATFORM_NVIDIA_NX)
    message("PLATFORM_NVIDIA_NX compile.")
    set(FLAGS_DEBUG  "-DDEBUG -Wall -Werror -Wno-format-truncation -Wno-deprecated-declarations -g -fPIC")
    set(FLAGS_RELEASE    "-DNDEBUG -Wall -Wno-format-truncation -Wno-deprecated-declarations -g -fPIC")
elseif (PLATFORM_H618)
    message("PLATFORM_H618 compile.")
    set(FLAGS_DEBUG  "-DDEBUG -Wall -Werror -Wno-format-truncation -Wno-deprecated-declarations -g -fPIC")
    set(FLAGS_RELEASE    "-DNDEBUG -Wall -Wno-format-truncation -Wno-deprecated-declarations -g -fPIC")
endif()

# Debug build, default.
#set(FLAGS_DEBUG  "-DDEBUG -Wall -Werror -Wno-format-truncation -Wno-deprecated-declarations -g -fPIC -mcpu=ck860")
set(CMAKE_CXX_FLAGS_DEBUG ${FLAGS_DEBUG} CACHE INTERNAL "Flags used for DEBUG builds" FORCE)
set(CMAKE_C_FLAGS_DEBUG ${FLAGS_DEBUG} CACHE INTERNAL "Flags used for DEBUG builds" FORCE)

# Release build.
#set(FLAGS_RELEASE    "-DNDEBUG -Wall -Wno-format-truncation -Wno-deprecated-declarations -g -fPIC -mcpu=ck860")
set(CMAKE_CXX_FLAGS_RELEASE ${FLAGS_RELEASE} CACHE INTERNAL "Flags used for RELEASE builds" FORCE)
set(CMAKE_C_FLAGS_RELEASE ${FLAGS_RELEASE} CACHE INTERNAL "Flags used for RELEASE builds" FORCE)
