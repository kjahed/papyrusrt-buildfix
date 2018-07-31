/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class CppDefaultMakefileGenerator
{

    def generate( String path, String targetMakefile )
    {

        val file = new File(path);
        
        val writer = new BufferedWriter( new FileWriter( file ) );
        writer.write( doGenerate( targetMakefile ).toString )
        writer.close
    }

    def private doGenerate( String targetMakefile ) {
        '''
		##################################################
		# Default makefile
		# Redirect make to target makefile
		##################################################
		
		all:
			make -f «targetMakefile» all
		clean:
			make -f «targetMakefile» clean
		.PHONY: 
			make -f «targetMakefile» all clean
        '''
    }

}
