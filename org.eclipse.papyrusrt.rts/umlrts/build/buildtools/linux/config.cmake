# linux common
#
# see  ../config.make

set(COPTS
  ${COPTS}
  )

set(CDEFS
  ${CDEFS}
  )

set(RTS_THREAD_LIB pthread)
set(LIBS
  ${LIBS}
  ${RTS_THREAD_LIB}
  )

# check for rt library
find_library(LIBRT rt)
if (LIBRT)
  set(LIBS
    ${LIBS}
    ${LIBRT}
    )
endif ()

# check for sys/socket.h
include(CheckIncludeFile)
set(HAS_SYS_SOCKET_H)
check_include_file(sys/socket.h HAS_SYS_SOCKET_H)
if (HAS_SYS_SOCKET_H)
  set(CDEFS
    ${CDEFS}
    HAS_SYS_SOCKET_H)
endif ()

# require pthread support
find_package(Threads REQUIRED)

# check for pthread_mutex_timedlock()
include(CheckLibraryExists)
set(HAS_PTHREAD_MUTEX_TIMEDLOCK)
check_library_exists(${RTS_THREAD_LIB} pthread_mutex_timedlock "" HAS_PTHREAD_MUTEX_TIMEDLOCK)
if (NOT HAS_PTHREAD_MUTEX_TIMEDLOCK)
  set(CDEFS
    ${CDEFS}
    NEED_PTHREAD_MUTEX_TIMEDLOCK)
endif ()

# check for sem_timedwait
set(HAS_SEM_TIMEDWAIT)
check_library_exists(pthread sem_timedwait "semaphore.h" HAS_SEM_TIMEDWAIT)
if (NOT HAS_SEM_TIMEDWAIT)
  set(CDEFS
    ${CDEFS}
    NEED_SEM_TIMEDWAIT)
endif ()

# check for sem_init
set(HAS_SEM_INIT)
check_library_exists(pthread sem_init "semaphore.h" HAS_SEM_INIT)
if (NOT HAS_SEM_INIT)
  set(CDEFS
    ${CDEFS}
    NEED_SEM_INIT)
endif ()

# check for clock_gettime
set(HAS_CLOCK_GETTIME)
check_library_exists(c clock_gettime "time.h" HAS_CLOCK_GETTIME)
if ((NOT HAS_CLOCK_GETTIME) AND LIBRT)
  unset(HAS_CLOCK_GETTIME CACHE)
  check_library_exists(rt clock_gettime "time.h" HAS_CLOCK_GETTIME)
endif ()
if (NOT HAS_CLOCK_GETTIME)
  set(CDEFS
    ${CDEFS}
    NEED_CLOCK_GETTIME)
endif ()

# determine user's home
set(USER_HOME $ENV{HOME})
