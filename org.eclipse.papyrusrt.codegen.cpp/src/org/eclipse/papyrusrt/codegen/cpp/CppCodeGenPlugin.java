/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.papyrusrt.codegen.config.CodeGenProvider;

/**
 * Activator for this plugin.
 */
public class CppCodeGenPlugin extends Plugin {

	/** Plugin ID. */
	public static final String ID = "org.eclipse.papyrusrt.codegen.cpp";

	/** Language. */
	public static final String LANGUAGE = "C++";

	/**
	 * Constructor.
	 */
	public CppCodeGenPlugin() {
		CodeGenProvider.getDefault().setModule(new CppCodeGenInjectionModule());
	}

}

