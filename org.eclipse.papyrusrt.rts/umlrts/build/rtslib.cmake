# rts library

# RTS value must match LIBRARY value in library's CMakelists.txt file
set(RTS rts)

# building library
if (RTS STREQUAL LIBRARY)

  # library sources
  set(SRCS)
  if (NOT OS_FILES_SDIR)
    set(OS_FILES_SDIR ${TARGETOS})
  endif ()
  set(FILES_CONFIG_DIR ${UMLRTS_ROOT}/build/os)
  include(${FILES_CONFIG_DIR}/files.cmake)
  set(FILES_CONFIG_DIR ${UMLRTS_ROOT}/build/os/${OS_FILES_SDIR})
  include(${FILES_CONFIG_DIR}/files.cmake)

else ()

  # model project depends

  # add external project support
  include(ExternalProject)

  # RTS build/install
  ExternalProject_Add(${RTS}
    SOURCE_DIR ${UMLRTS_ROOT}
    DOWNLOAD_COMMAND ""
    BUILD_COMMAND ""
    UPDATE_COMMAND ""
    INSTALL_COMMAND
            ${CMAKE_COMMAND}
            --build .
            --target install
            --config ${configuration}
    )
  add_dependencies(${TARGET} ${RTS})

  # Destination directory for the RTS services library.
  set(RTS_NAME ${CMAKE_STATIC_LIBRARY_PREFIX}${RTS}${CMAKE_DEBUG_POSTFIX}${CMAKE_STATIC_LIBRARY_SUFFIX})
  set(RTS_LIB ${UMLRTS_ROOT}/lib/${TARGETOS}/${BUILD_TOOLS}/${BUILD_CONFIG}/${RTS_NAME})
  set(INCS
    ${INCS}
    ${UMLRTS_ROOT}/include
    )
  set(LIBS
    ${LIBS}
    ${RTS_LIB}
    )

endif ()

# reorder lib list placing object files upfront
set(OLIST)
set(SLIST)
foreach(ITEM  ${LIBS})
  if (${ITEM} MATCHES "\\.[ao]$")
    list(APPEND OLIST ${ITEM})
  else ()
    list(APPEND SLIST ${ITEM})
  endif ()
endforeach (ITEM)
set(LIBS ${OLIST} ${SLIST})
set(OLIST)
set(SLIST)
