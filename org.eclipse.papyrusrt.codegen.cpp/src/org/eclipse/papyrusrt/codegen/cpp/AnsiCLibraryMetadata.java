/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.papyrusrt.xtumlrt.external.ExternalPackageMetadata;

/**
 * Meta-data for the Ansi C Library.
 * 
 * @see ExternalPackageMetadata
 * @author epp
 */
public final class AnsiCLibraryMetadata extends ExternalPackageMetadata {

	/** Common instance. */
	public static final AnsiCLibraryMetadata INSTANCE = new AnsiCLibraryMetadata();

	/** Profile id. */
	private static final String ID = "org.eclipse.papyrus.designer.languages.cpp.library";

	/** Name-space URI. */
	private static final String NS_URI = "http://www.eclipse.org/papyrus/C_Cpp/1";

	/** Pathmap. */
	private static final String BASEPATHMAP = "pathmap://PapyrusC_Cpp_LIBRARIES";

	/** Root node id. */
	// TODO: Find out if this can be extracted programatically
	private static final String ROOT_ID = "_DV8nkBv8EduZN5aJJITI5w";

	/** Subdirectory containing the model. */
	private static final String FOLDER = "models";

	/** File containing the model. */
	private static final String FILE = "AnsiCLibrary.uml";

	/** {@link EPackage}. */
	private static final EPackage EPACKAGE = null;

	/**
	 * Default constructor. Private as this class should not be extended.
	 *
	 */
	private AnsiCLibraryMetadata() {
		super(ID, Kind.Library, NS_URI, BASEPATHMAP, ROOT_ID, FOLDER, FILE, EPACKAGE);
	}

}
