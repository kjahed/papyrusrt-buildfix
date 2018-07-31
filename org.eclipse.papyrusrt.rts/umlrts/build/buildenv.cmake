# shared cmake include

# prevent in-source builds
if ("${PROJECT_SOURCE_DIR}" STREQUAL "${PROJECT_BINARY_DIR}")
  set(DO_REMOVE)
  foreach (ITEM CMakeFiles CMakeCache.txt rts-prefix)
    #if (EXISTS ${PROJECT_SOURCE_DIR}/${ITEM})
      set(DO_REMOVE ${DO_REMOVE} "    ${PROJECT_SOURCE_DIR}/${ITEM}\n")
    #endif ()
  endforeach ()
  if (DO_REMOVE)
    list(LENGTH DO_REMOVE REM_LEN)
    set(DO_REMOVE "\n    *** Move the following ${REM_LEN} directories/files out of the way, and try building from an empty directory ***\n" ${DO_REMOVE})
  endif ()
  message(FATAL_ERROR 
    "\n"
    "Build requires using an out-of-source directory:\n"
    "    source directory: ${PROJECT_SOURCE_DIR}\n"
    "     build directory: ${PROJECT_BINARY_DIR}\n"
    ${DO_REMOVE}
    "\n"
    )
endif ()

# set TRUE to dump cmake variables
if (FALSE)
  get_cmake_property(vars VARIABLES)
  foreach (var ${vars})
    list(LENGTH ${var} cnt)
    if (cnt LESS 2)
      message(STATUS "${var}=${${var}}")
    endif ()
  endforeach ()
endif ()

# debug/release config
if (NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Debug)
endif()
string(TOLOWER ${CMAKE_BUILD_TYPE} BUILD_CONFIG)
if (${BUILD_CONFIG} MATCHES "debug" AND NOT CMAKE_DEBUG_POSTFIX)
  set(CMAKE_DEBUG_POSTFIX "d")
endif()

# determine build environment
if (MSVC)
  set(ARCH ${CMAKE_VS_PLATFORM_NAME})
  string(TOLOWER ${ARCH} ARCH)
  if (${ARCH} STREQUAL "win32")
    set(ARCH x86)
  endif()
  set(BUILD_TOOLS ${ARCH}-msvc-${CMAKE_VS_PLATFORM_TOOLSET})
  set(TARGETOS windows)
elseif (NOT CMAKE_COMPILER_IS_GNUCXX)
  message(FATAL_ERROR "Unsupported build environment. Try setting CXX to g++.")
else ()
  if ("${CMAKE_CXX_PLATFORM_ID}" STREQUAL "Cygwin")
    string(REGEX REPLACE "\\\\" "/" UMLRTS_ROOT ${UMLRTS_ROOT})
    string(REGEX REPLACE "^([A-Za-z]):(.*)$" "/cygdrive/\\1\\2" UMLRTS_ROOT ${UMLRTS_ROOT})
    set(TARGETOS linux)
  elseif ("${CMAKE_CXX_PLATFORM_ID}" STREQUAL "Darwin")
    set(TARGETOS darwin)
  else ()
    set(TARGETOS linux)
  endif ()
  set(BASE_NAME g++)
  if (CMAKE_CXX_COMPILER_VERSION)
    set(COMPILER_VERSION ${CMAKE_CXX_COMPILER_VERSION})
  else ()
    execute_process(
      COMMAND ${BASE_NAME} --version
      COMMAND grep ^${BASE_NAME}
      COMMAND sed -e "s/^.* //g"
      OUTPUT_VARIABLE COMPILER_VERSION OUTPUT_STRIP_TRAILING_WHITESPACE)
  endif ()
  set(ARCH x86) # TODO detect
  set(BUILD_TOOLS ${ARCH}-${BASE_NAME}-${COMPILER_VERSION})
endif ()

# report RTS root and build environment
message(STATUS "RTS root: " ${UMLRTS_ROOT})
message(STATUS "Detected os, arch, compiler, version: ${TARGETOS}, ${BUILD_TOOLS}")

# include primary config
set(CONFIG_CMAKE config.cmake)
set(BUILD_CONFIG_CMAKE config-${BUILD_CONFIG}.cmake)
include(${UMLRTS_ROOT}/build/${CONFIG_CMAKE})
