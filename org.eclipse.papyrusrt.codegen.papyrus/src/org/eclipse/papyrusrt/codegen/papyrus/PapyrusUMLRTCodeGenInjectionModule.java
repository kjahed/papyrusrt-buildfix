/*****************************************************************************
 * Copyright (c) Zeligsoft (2009) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Ernesto Posse - Initial API and implementation
 *****************************************************************************/

package org.eclipse.papyrusrt.codegen.papyrus;

import org.eclipse.papyrusrt.codegen.UMLRTCodeGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodeGenInjectionModule;

/**
 * @author epp
 */
public class PapyrusUMLRTCodeGenInjectionModule extends CppCodeGenInjectionModule {

	@Override
	protected void configure() {
		super.configure();
		bind(UMLRTCodeGenerator.class).to(PapyrusUMLRT2CppCodeGenerator.class);
	}

}
