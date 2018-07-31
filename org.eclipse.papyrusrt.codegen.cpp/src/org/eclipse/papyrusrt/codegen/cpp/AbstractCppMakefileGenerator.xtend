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

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.List
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTSUtil
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName

abstract class AbstractCppMakefileGenerator {

    def String formatFilename(String component);

    def generate(String path, List<FileName> files, String main)
    {
        val file = new File(path);
        val writer = new BufferedWriter(new FileWriter(file));
        writer.write(doGenerate(files, main, UMLRTSUtil.getRTSRootPath ).toString)
        writer.close
    }

    def protected String doGenerate(List<FileName> files, String main, String rtsPath)
}