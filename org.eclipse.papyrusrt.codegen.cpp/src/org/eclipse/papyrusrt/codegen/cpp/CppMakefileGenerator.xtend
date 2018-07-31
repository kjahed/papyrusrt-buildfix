/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp

import java.util.List
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName

class CppMakefileGenerator extends AbstractCppMakefileGenerator
{
    override protected String doGenerate(List<FileName> files, String main, String rtsPath)
    {
        '''
        # set default value for TARGETOS if is it not defined
        ifeq ($(TARGETOS), )
        $(warning warning: TARGETOS not defined. Choosing linux)
        TARGETOS=linux
        endif

        # set default value for BUILDTOOLS if is it not defined
        ifeq ($(BUILDTOOLS), )
        $(warning warning: BUILDTOOLS not defined. Choosing x86-gcc-4.6.3)
        BUILDTOOLS=x86-gcc-4.6.3
        endif

        # Location of RTS root.
        UMLRTS_ROOT ?= «IF rtsPath == ""»./umlrt.rts«ELSE»«rtsPath»«ENDIF»

        CONFIG=$(TARGETOS).$(BUILDTOOLS)

        # Destination directory for the RTS services library.
        LIBDEST=$(UMLRTS_ROOT)/lib/$(CONFIG)

        include $(UMLRTS_ROOT)/build/host/host.mk
        include $(UMLRTS_ROOT)/build/buildtools/$(BUILDTOOLS)/buildtools.mk

        LD_PATHS=$(LIBDEST)
        CC_INCLUDES+=$(UMLRTS_ROOT)/include

        CC_DEFINES:=$(foreach d, $(CC_DEFINES), $(CC_DEF)$d)
        CC_INCLUDES:=$(foreach i, $(CC_INCLUDES), $(CC_INC)$i)
        LD_LIBS:=$(foreach i, $(LD_LIBS), $(LD_LIB)$i)
        LD_PATHS:=$(foreach i, $(LD_PATHS), $(LD_LIBPATH)$i)

        SRCS = «main».cc «FOR f : files»«f.absolutePath».cc «ENDFOR»
        OBJS = $(subst $(CC_EXT),$(OBJ_EXT),$(SRCS))

        MAIN = «main»$(EXE_EXT)

        all: $(MAIN)

        $(MAIN): $(OBJS) $(UMLRTS_ROOT)/lib/$(CONFIG)/$(LIB_PRFX)rts$(LIB_EXT)
        	$(LD) $(LD_FLAGS) $(OBJS) $(LD_PATHS) $(LD_LIBS) $(LD_OUT)$@

        %$(OBJ_EXT) : %$(CC_EXT)
        	$(CC) $< $(CC_FLAGS) $(CC_DEFINES) $(CC_INCLUDES) $(CC_OUT)$@

        clean :
        	@echo $(RM) main$(EXE_EXT) *$(OBJ_EXT) *$(DEP_EXT) $(DBG_FILES)
        	@$(RM) main$(EXE_EXT) *$(OBJ_EXT) *$(DEP_EXT) $(DBG_FILES)

        .PHONY: all clean
        '''
    }

  override formatFilename(String component) {
    return "Makefile" + component + ".mk";
  }

}
