# UMLRT  library make script

cmake_minimum_required(VERSION 2.8.7)
set(LIBRARY rts)
project (${LIBRARY})

# require location of supporting RTS
if (NOT UMLRTS_ROOT)
  if (DEFINED ENV{UMLRTS_ROOT})
    set(UMLRTS_ROOT $ENV{UMLRTS_ROOT})
  else ()
    set(UMLRTS_ROOT ${CMAKE_CURRENT_SOURCE_DIR})
  endif ()
endif ()

# setup primary envars - provides tooling config
include(${UMLRTS_ROOT}/build/buildenv.cmake)

# setup lib dependency support before defining TARGET
include(${UMLRTS_ROOT}/build/rtslib.cmake)

# specify target
add_library(${LIBRARY} ${SRCS})

# compilere parameters
set_target_properties(${LIBRARY} PROPERTIES COMPILE_OPTIONS "${COPTS}")
set_target_properties(${LIBRARY} PROPERTIES COMPILE_DEFINITIONS "${CDEFS}")
include_directories(${INCS})

# linker parameters
set_target_properties(${LIBRARY} PROPERTIES CMAKE_EXE_LINKER_FLAGS "${LOPTS}")
set_target_properties(${LIBRARY} PROPERTIES CMAKE_MODULE_LINKER_FLAGS "${LOPTS}")
set_target_properties(${LIBRARY} PROPERTIES CMAKE_SHARED_LINKER_FLAGS "${LOPTS}")

#install paramters
install(TARGETS ${LIBRARY} DESTINATION ${UMLRTS_ROOT}/lib/${TARGETOS}/${BUILD_TOOLS}/${BUILD_CONFIG})
