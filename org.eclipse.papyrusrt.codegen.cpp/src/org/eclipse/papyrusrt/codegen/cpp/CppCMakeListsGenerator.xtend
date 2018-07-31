/*****************************************************************************
 * Copyright (c) 2016 Codics Corp and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   William Byrne - Initial API and implementation
 *
 *****************************************************************************/

package org.eclipse.papyrusrt.codegen.cpp

import java.util.List
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName
import java.text.SimpleDateFormat;
import java.util.Date;

class CppCMakeListsGenerator extends AbstractCppMakefileGenerator
{
    override protected String doGenerate(List<FileName> files, String main, String rtsPath)
    {
        '''
        # Generated «(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())»

        cmake_minimum_required(VERSION 2.8.7)
        set(TARGET «main»)
        project(${TARGET})

        # require location of supporting RTS
        if (NOT UMLRTS_ROOT)
          if (DEFINED ENV{UMLRTS_ROOT})
            set(UMLRTS_ROOT $ENV{UMLRTS_ROOT})
          else ()
            set(UMLRTS_ROOT «IF rtsPath == ""»${CMAKE_CURRENT_SOURCE_DIR}/umlrt.rts«ELSE»«rtsPath»«ENDIF»)
          endif ()
        endif ()

        # setup primary envars - provides tooling config
        include(${UMLRTS_ROOT}/build/buildenv.cmake)

        # model sources
        set(SRCS «main».cc «FOR f : files»«f.absolutePath».cc «ENDFOR»)

        # specify target
        add_executable(${TARGET} ${SRCS})

        # setup lib dependency support after defining TARGET
        include(${UMLRTS_ROOT}/build/rtslib.cmake)

        # compiler parameters
        set_target_properties(${TARGET} PROPERTIES COMPILE_OPTIONS "${COPTS}")
        set_target_properties(${TARGET} PROPERTIES COMPILE_DEFINITIONS "${CDEFS}")
        include_directories(${INCS})

        # linker parameters
        set_target_properties(${TARGET} PROPERTIES CMAKE_EXE_LINKER_FLAGS "${LOPTS}")
        target_link_libraries(${TARGET} ${LIBS})

        '''
    }

    override formatFilename(String component) {
        return "CMakeLists.txt";
    }
}
