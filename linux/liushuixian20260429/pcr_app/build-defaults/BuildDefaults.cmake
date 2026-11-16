# Append custom CMake modules.
list(APPEND CMAKE_MODULE_PATH ${CMAKE_CURRENT_LIST_DIR}/cmake)

# Setup platform dependant variables.
include(Platforms)

# Setup default build options, like compiler flags and build type.
include(BuildOptions)

include(PrepareInstall)

include(Version)

