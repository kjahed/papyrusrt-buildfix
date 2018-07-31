# primary config
#
# optionally loads config.cmake, config-debug.cmake, etc  in tooling hierarchy

# init variables
set(COPTS)
set(CDEFS)
set(LOPTS)
set(INCS)
set(LIBS)

if (${CMAKE_VERSION} VERSION_GREATER 2.8.10)
 string(TIMESTAMP NOW "%Y-%m-%dT%H:%M:%S")
else ()
 set(NOW "")
endif ()


## load optional config files

# rts
set(CONFIG_DIR ${UMLRTS_ROOT}/build)
include(${CONFIG_DIR}/${BUILD_CONFIG_CMAKE} OPTIONAL)
foreach (CONFIG_SDIR buildtools ${TARGETOS} ${BUILD_TOOLS})
  set(CONFIG_DIR ${CONFIG_DIR}/${CONFIG_SDIR})
  if (NOT EXISTS ${CONFIG_DIR})
    file(MAKE_DIRECTORY ${CONFIG_DIR})
  endif ()
  if (NOT EXISTS ${CONFIG_DIR}/${CONFIG_CMAKE})
    file(WRITE ${CONFIG_DIR}/${CONFIG_CMAKE} "# created by CMake ${NOW}\n#\n# custom settings can be added to this file\n")
  endif ()
  include(${CONFIG_DIR}/${CONFIG_CMAKE} OPTIONAL)
  include(${CONFIG_DIR}/${BUILD_CONFIG_CMAKE} OPTIONAL)
endforeach (CONFIG_SDIR)
set(CONFIG_DIR)
set(CONFIG_SDIR)

# user - tooling hierarchy can be determined in user's config file
include(${USER_HOME}/.papyrus-rt/build/${CONFIG_CMAKE} OPTIONAL)

