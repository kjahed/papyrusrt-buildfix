/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.internal.ArtifactGenerator;
import org.eclipse.papyrusrt.codegen.cpp.internal.CapsuleGenerator;
import org.eclipse.papyrusrt.codegen.cpp.internal.EclipseGeneratorManager;
import org.eclipse.papyrusrt.codegen.cpp.internal.EmptyStateMachineGenerator;
import org.eclipse.papyrusrt.codegen.cpp.internal.EnumGenerator;
import org.eclipse.papyrusrt.codegen.cpp.internal.ProtocolGenerator;
import org.eclipse.papyrusrt.codegen.cpp.internal.SerializableClassGenerator;
import org.eclipse.papyrusrt.xtumlrt.common.Artifact;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.Enumeration;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.StructuredType;
import org.eclipse.papyrusrt.xtumlrt.statemach.StateMachine;

/**
 * A generator manager provides instances of {@link AbstractElementGenerator} for particular elements.
 * 
 * <p>
 * This is intended to be tha base class for generator managers providing a default generator
 * factory method {@link GeneratorManager#getGenerator} which creates instances of the built-in
 * generators.
 */
public class GeneratorManager {

	/** The common instance of this class. */
	private static GeneratorManager INSTANCE;

	/**
	 * Default Constructor.
	 */
	public GeneratorManager() {
	}

	/**
	 * Set the common instance of this class.
	 * 
	 * @param instance
	 *            - The {@link GeneratorManager} instance.
	 */
	public static void setInstance(GeneratorManager instance) {
		if (INSTANCE != null) {
			throw new RuntimeException("Invalid attempt to replace GeneratorManager");
		}
		INSTANCE = instance;
	}

	/**
	 * @return The shared instance of this class. If it has not been explicitly set, return the {@link EclipseGeneratorManager}.
	 */
	public static GeneratorManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new EclipseGeneratorManager();
		}
		return INSTANCE;
	}

	/**
	 * Factory method to obtain an instance of a particular {@link Kind} of generator for a given element and context.
	 * 
	 * <p>
	 * This default implementation instantiates the built-in element generators.
	 * 
	 * @param kind
	 *            - The {@link Kind} of generator.
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param element
	 *            - The {@link NamedElement} for which code is to be generated.
	 * @param context
	 *            - The {@link NamedElement} to use as context.
	 * @return An instance of the appropriate subclass of {@link AbstractElementGenerator.
	 */
	public AbstractElementGenerator getGenerator(XTUMLRT2CppCodeGenerator.Kind kind, CppCodePattern cpp, NamedElement element, NamedElement context) {
		// Default behaviour is to fall-back to built in defaults where possible.
		switch (kind) {
		case BasicClass:
			if (element instanceof StructuredType) {
				return new SerializableClassGenerator(cpp, (StructuredType) element);
			}
			break;
		case Enum:
			if (element instanceof Enumeration) {
				return new EnumGenerator(cpp, (Enumeration) element);
			}
			break;
		case Protocol:
			if (element instanceof Protocol) {
				return new ProtocolGenerator(cpp, (Protocol) element);
			}
			break;
		case Capsule:
			if (element instanceof Capsule) {
				return new CapsuleGenerator(cpp, (Capsule) element);
			}
			break;
		case EmptyStateMachine:
			if (element instanceof StateMachine && context instanceof Capsule) {
				return new EmptyStateMachineGenerator(cpp, (StateMachine) element, (Capsule) context);
			}
			break;
		case Artifact:
			return new ArtifactGenerator(cpp, (Artifact) element);
		default:
			break;
		}

		CodeGenPlugin.error("ignoring request for unknown generator " + kind.id);
		throw new RuntimeException("cannot find generator id " + kind.id);
	}

}
