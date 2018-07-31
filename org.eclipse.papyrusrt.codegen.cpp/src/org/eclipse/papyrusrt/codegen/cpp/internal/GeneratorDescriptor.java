/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.AbstractElementGenerator;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator;
import org.eclipse.uml2.uml.util.UMLUtil;

/**
 * A generator descriptor stores the type of a generator and the {@link IConfigurationElement} of the
 * extension that provides the generator.
 * 
 * <p>
 * It also includes a factory method to obtain an instance of the generator.
 * 
 * @author epp
 */
@SuppressWarnings("rawtypes")
public class GeneratorDescriptor {

	/** The extension's 'type' attribute name. */
	private static final String ATTR_TYPE = "type";

	/** The extension's 'class' attribute name. */
	private static final String ATTR_CLASS = "class";

	/** The extension's 'priority' attribute name. */
	private static final String ATTR_PRIORITY = "priority";

	/** The {@link IConfigurationElement} of the extension which provides the generator. */
	private final IConfigurationElement element;

	/** Generator type. It is the same as the id of a {@link XTUMLRT2CppCodeGenerator.Kind}. */
	private String type;

	/** The priority of the generator with respect to other generators provided. */
	private Priority priority;

	/** The {@link AbstractElementGenerator.Factory} of the generator. */
	private AbstractElementGenerator.Factory factory;

	/**
	 * The kinds of priority of the generator with respect to other generators provided.
	 */
	public enum Priority {
		Low(1), Medium(2), High(3), Highest(4);

		/** The priority's value. */
		private int priority;

		/**
		 * Constructor.
		 *
		 * @param p
		 *            - The priority's value.
		 */
		Priority(int p) {
			priority = p;
		}

		int getValue() {
			return priority;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param element
	 *            - The {@link IConfigurationElement} of the extension that provides the generator.
	 */
	public GeneratorDescriptor(IConfigurationElement element) {
		this.element = element;
		this.type = element.getAttribute(ATTR_TYPE);
		String pt = element.getAttribute(ATTR_PRIORITY);
		if (UMLUtil.isEmpty(pt)) {
			pt = "Low";
		}
		switch (pt) {
		case "Low":
			priority = Priority.Low;
			break;
		case "Medium":
			priority = Priority.Medium;
			break;
		case "High":
			priority = Priority.High;
			break;
		case "Highest":
			priority = Priority.Highest;
			break;
		default:
			priority = Priority.Low;
			break;
		}
	}

	public String getType() {
		return type;
	}

	public Priority getPriority() {
		return priority;
	}

	/**
	 * Create an factory that produces generators provided by the extension.
	 * 
	 * @return An {@link AbstractElementGenerator.Factory}.
	 */
	public AbstractElementGenerator.Factory getFactory() {
		if (factory == null) {
			synchronized (this) {
				if (factory == null) {
					try {
						factory = (AbstractElementGenerator.Factory) element.createExecutableExtension(ATTR_CLASS);
					} catch (CoreException e) {
						String id = element.getDeclaringExtension().getNamespaceIdentifier() + '.' + element.getDeclaringExtension().getSimpleIdentifier();
						CodeGenPlugin.error("Error in class attribute of " + id, e);
					}
				}
			}
		}

		return factory;
	}
}
