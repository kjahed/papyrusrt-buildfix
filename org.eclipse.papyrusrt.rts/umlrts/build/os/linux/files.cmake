# linux sources

# os specific include directories
set(INCS
  ${INCS}
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/include
  )
set(SRCS
  ${SRCS}
  ${UMLRTS_ROOT}/util/basedebug.cc
  ${UMLRTS_ROOT}/util/basefatal.cc
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/osbasicthread.cc
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/osmutex.cc
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/osnotify.cc
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/ossemaphore.cc
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/ostime.cc
  ${UMLRTS_ROOT}/os/${OS_FILES_SDIR}/ostimespec.cc
  )
