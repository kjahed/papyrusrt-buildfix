/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;

/**
 * This is the base class for model element generators. Generators for specific {@link XTUMLRT2CppCodeGenerator.Kind}
 * of elements should extend this class.
 * 
 * @author epp
 */
public abstract class AbstractElementGenerator {

	/** The {@link CppCodePattern}. This should be the same for all elements. */
	protected final CppCodePattern cpp;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 */
	protected AbstractElementGenerator(CppCodePattern cpp) {
		this.cpp = cpp;
	}

	/**
	 * @return The {@link Output} kind of the generator.
	 */
	protected abstract Output getOutputKind();

	/**
	 * A label used in reporting code generation results.
	 * 
	 * <p>
	 * Subclasses should override this implement to provide an appropriate label
	 * in the codegen results.
	 * 
	 * @return The label.
	 */
	public String getLabel() {
		return getClass().getSimpleName();
	}

	/**
	 * Perform the actual generation. This method should be implemented by subclasses.
	 * 
	 * @return {@code true} iff the generation was successful.
	 */
	public abstract boolean generate();

	/**
	 * Queries generated filenames for this generator. Subclass must override
	 * this function if filenames are generated from this generator.
	 *
	 * @return Generated filenames
	 */
	public List<FileName> getGeneratedFilenames() {
		return new ArrayList<>();
	}

	/**
	 * A factory that provides instances of the code generator for a particular element.
	 * 
	 * @param <T>
	 *            - The type of {@link NamedElement} to generate.
	 * @param <U>
	 *            - The type of {@link NamedElement} to use as context.
	 */
	public static interface Factory<T extends NamedElement, U extends NamedElement> {

		/**
		 * The factory method that creates code generator instances.
		 * 
		 * @param cpp
		 *            - The {@link CppCodePattern}.
		 * @param t
		 *            - The element to generate code for.
		 * @param context
		 *            - The context for generation of t.
		 * @return An instance of a subclass of {@link AbstractCppCodeGenerator}.
		 */
		AbstractElementGenerator create(CppCodePattern cpp, T t, U context);
	}

}
