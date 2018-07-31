/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import org.eclipse.papyrusrt.codegen.cpp.AbstractBehaviourGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberFunction;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.statemach.StateMachine;

/**
 * This generator is used when a capsule doesn't specify a state machine.
 * 
 * @author epp
 */
public class EmptyStateMachineGenerator extends AbstractBehaviourGenerator<StateMachine, Capsule> {

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param stateMachine
	 *            - The model (empty) {@link StateMachine}.
	 * @param capsule
	 *            - The {@link Capsule}.
	 */
	public EmptyStateMachineGenerator(CppCodePattern cpp, StateMachine stateMachine, Capsule capsule) {
		super(cpp, stateMachine, capsule);
	}

	@Override
	protected void generateInitializeBody(CppClass cls, MemberFunction initializeFunc, StateMachine stateMachine, Capsule capsule) {
	}

	@Override
	protected void generateInjectBody(CppClass cls, MemberFunction injectFunc, StateMachine stateMachine, Capsule capsule) {
	}

	@Override
	protected void generateAdditionalElements(CppClass cls, StateMachine behaviourElement, Capsule contextU) {
	}

}
