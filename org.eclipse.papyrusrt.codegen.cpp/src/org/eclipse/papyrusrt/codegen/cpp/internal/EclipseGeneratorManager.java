/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.AbstractElementGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.GeneratorManager;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;

/**
 * A {@link GeneratorManager} that loads extensions.
 * 
 * @author epp
 */
public class EclipseGeneratorManager extends GeneratorManager {

	/** Name of the extension point. */
	private static final String EXTENSION_POINT = "generator"; //$NON-NLS-1$

	/** Map from generator types to {@link GeneratorDescriptor}s. */
	private Map<String, GeneratorDescriptor> generators;

	/**
	 * Constructor.
	 */
	public EclipseGeneratorManager() {
		generators = loadExtensions();
	}

	/**
	 * Load extensions and record additional generators.
	 * 
	 * @return A map from generator types to {@link GeneratorDescriptor}s.
	 */
	private static Map<String, GeneratorDescriptor> loadExtensions() {
		Map<String, GeneratorDescriptor> generators = new HashMap<>();

		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(CppCodeGenPlugin.ID, EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			GeneratorDescriptor desc = new GeneratorDescriptor(element);
			if (generators.containsKey(desc.getType())) {
				GeneratorDescriptor gen = generators.get(desc.getType());
				if (desc.getPriority().getValue() > gen.getPriority().getValue()) {
					generators.put(desc.getType(), desc);
					CodeGenPlugin.info("Ignoring lower priority generator for " + gen.getType() + ":"
							+ gen.getFactory().toString());
				} else {
					CodeGenPlugin.info("Ignoring lower priority generator for " + desc.getType() + ":"
							+ desc.getFactory().toString());
				}
			} else {
				generators.put(desc.getType(), desc);
			}
		}

		return generators;
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractElementGenerator getGenerator(XTUMLRT2CppCodeGenerator.Kind kind, CppCodePattern cpp, NamedElement element, NamedElement context) {
		// Look for a registered generator for the given kind.
		GeneratorDescriptor desc = generators.get(kind.id);
		if (desc != null) {
			return desc.getFactory().create(cpp, element, context);
		}

		// If a generator has not been registered, then fall-back to built in defaults where possible.
		return super.getGenerator(kind, cpp, element, context);
	}
}
