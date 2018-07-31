/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrusrt.codegen.cpp.AbstractElementGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppGenerationProperties;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppArtifact;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.ElementList;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.xtumlrt.common.Artifact;

/**
 * Generator of {@link Artifact}s.
 * 
 * @author epp
 */
public class ArtifactGenerator extends AbstractElementGenerator {

	/** The {@link Artifact}. */
	protected final Artifact element;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param element
	 *            - The {@link Artifact}.
	 */
	public ArtifactGenerator(CppCodePattern cpp, Artifact element) {
		super(cpp);
		this.element = element;
	}

	@Override
	protected Output getOutputKind() {
		return Output.Artifact;
	}

	@Override
	public String getLabel() {
		return super.getLabel() + ' ' + element.getName();
	}

	@Override
	public boolean generate() {
		String includeFile = RTCppGenerationProperties.getArtifactPropIncludeFile(element);
		String sourceFile = RTCppGenerationProperties.getArtifactPropSourceFile(element);
		if (includeFile == null) {
			includeFile = "";
		}
		if (sourceFile == null) {
			sourceFile = "";
		}
		CppArtifact artifact = cpp.getWritableCppArtifact(CppCodePattern.Output.Artifact, element);
		ElementList list = (ElementList) artifact.getDefinedIn();
		list.addDeclEndingText(includeFile);
		list.addDefnEndingText(sourceFile);
		return true;
	}

	@Override
	public List<FileName> getGeneratedFilenames() {
		List<FileName> result = new ArrayList<>();
		ElementList el = cpp.getElementList(getOutputKind(), element);
		result.add(el.getName());
		return result;
	}
}
