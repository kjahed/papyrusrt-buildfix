/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import org.eclipse.papyrusrt.codegen.cpp.AbstractElementGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppEnum;
import org.eclipse.papyrusrt.xtumlrt.common.Enumeration;
import org.eclipse.papyrusrt.xtumlrt.common.EnumerationLiteral;

/**
 * Generator for model enums.
 * 
 * @author epp
 */
public class EnumGenerator extends AbstractElementGenerator {

	/** The model {@link Enumeration} element. */
	private final Enumeration element;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param element
	 *            - The model {@link Enumeration} element.
	 */
	public EnumGenerator(CppCodePattern cpp, Enumeration element) {
		super(cpp);
		this.element = element;
	}

	@Override
	protected Output getOutputKind() {
		return Output.UserEnum;
	}

	@Override
	public boolean generate() {
		CppEnum enm = cpp.getWritableCppEnum(CppCodePattern.Output.UserEnum, element);
		for (EnumerationLiteral enumerator : element.getLiterals()) {
			enm.add(enumerator.getName());
		}

		return true;
	}

}
