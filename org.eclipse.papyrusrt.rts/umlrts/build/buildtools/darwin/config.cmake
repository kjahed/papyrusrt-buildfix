# darwin common
#
# see  ../config.make

# TODO revisit deprecated decls (e.g., sem_init)
set(COPTS
  ${COPTS}
  -Wno-deprecated-declarations
  )

# check for alternate compiler
if (${CMAKE_CXX_COMPILER} STREQUAL "/opt/local/bin/g++")
  set(INCS
    ${INCS}
    /opt/local/include
    )
  set(LOPTS
    ${LOPTS}
    -L/opt/local/lib
    )
endif ()

# using linux files
set(OS_FILES_SDIR linux)

# using linux config
include(${CONFIG_DIR}/../linux/config.cmake)
