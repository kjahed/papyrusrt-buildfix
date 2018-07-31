# windows common
#
# see  ../config.make

set(COPTS
  ${COPTS}
  -c
  -EHsc
  -nologo
  -W3
  -wd4101
  -wd4996
  )

set(CDEFS
  ${CDEFS}
  _CRT_SECURE_NO_WARNINGS
  _UNICODE
  )

set(LIBS
  ${LIBS}
  ws2_32.lib
  )
  
# check for struct timespec
include(CheckTypeSize)
set(HAS_STRUCT_TIMESPEC_T)
set(CMAKE_EXTRA_INCLUDE_FILES time.h)
check_type_size("struct timespec" HAS_STRUCT_TIMESPEC_T)
if (NOT HAS_STRUCT_TIMESPEC_T)
  set(CDEFS
    ${CDEFS}
    NEED_STRUCT_TIMESPEC_T
    )
endif ()
set(CMAKE_EXTRA_INCLUDE_FILES)
set(HAS_STRUCT_TIMESPEC_T)

# flag for non-empty arrays
if (${MSVC})
  set(CDEFS
    ${CDEFS}
    NEED_NON_FLEXIBLE_ARRAY
    )
endif ()

# determine user's home
if ($ENV{HOME})
  set(USER_HOME $ENV{HOME})
else ()
  set(USER_HOME $ENV{HOMEDRIVE}$ENV{HOMEPATH})
endif ()
