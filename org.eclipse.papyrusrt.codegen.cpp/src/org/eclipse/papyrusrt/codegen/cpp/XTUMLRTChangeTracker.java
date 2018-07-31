/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.papyrus.designer.languages.common.base.codesync.ChangeObject;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator.GeneratorKey;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator.Kind;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;

/**
 * An implementation of {@link ChangeTracker} that records changes in an xtUML-RT model.
 * 
 * @author epp
 *
 */
public class XTUMLRTChangeTracker implements ChangeTracker {

	/** The common instance of this class. */
	private static ChangeTracker ACTIVE_INSTANCE = null;

	/** The {@link CppCodePattern}. */
	private CppCodePattern cpp;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 */
	public XTUMLRTChangeTracker(CppCodePattern cpp) {
		this.cpp = cpp;
	}

	public static void setActiveInstance(ChangeTracker trackerInstance) {
		ACTIVE_INSTANCE = trackerInstance;
	}

	public static ChangeTracker getActiveInstance() {
		return ACTIVE_INSTANCE;
	}

	@Override
	public void prune(Map<GeneratorKey, AbstractElementGenerator> generators) {
		// TODO Auto-generated method stub
	}

	@Override
	public void consumeChanges(Map<GeneratorKey, AbstractElementGenerator> generators) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addChanges(List<ChangeObject> changeList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addAlreadyGenerated(Kind kind, NamedElement object) {
		// TODO Auto-generated method stub
	}

	@Override
	public Collection<EObject> getAllChanged() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void closeResource(Resource resource) {
		// TODO Auto-generated method stub
	}

	@Override
	public void resetAll() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setTop(EObject top) {
		// TODO Auto-generated method stub
	}

}
