/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Parameter;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.xtumlrt.common.Behaviour;
import org.eclipse.papyrusrt.xtumlrt.common.Entity;

/**
 * Base class for generators of {@link Behaviour} elements.
 * 
 * @author epp
 *
 * @param <T>
 *            The (sub)type of {@link Behaviour}.
 * @param <U>
 *            The (sub)type of {@link Entity} that contains the {@link Behaviour}.
 */
public abstract class AbstractBehaviourGenerator<T extends Behaviour, U extends Entity> extends AbstractElementGenerator {

	/** The name used for the message parameter in the initialize and inject member functions. */
	private static final String MESSAGE_PARAMETER_NAME = "msg";

	/** The behaviour element for which code is to be generated. */
	private final T behaviourElement;

	/** The context element for the generator. Usually the {@link Entity} that contains the behaviour element. */
	private final U context;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param element
	 *            - The behaviour element for which code is to be generated.
	 * @param context
	 *            - The context element for the generator.
	 */
	public AbstractBehaviourGenerator(CppCodePattern cpp, T element, U context) {
		super(cpp);
		this.behaviourElement = element;
		this.context = context;
	}

	@Override
	protected Output getOutputKind() {
		return Output.BasicClass;
	}

	@Override
	public String getLabel() {
		return super.getLabel() + ' ' + behaviourElement.getName();
	}

	public T getElement() {
		return behaviourElement;
	}

	public U getContext() {
		return context;
	}

	@Override
	public boolean generate() {
		U entity = context;
		CppClass cls = cpp.getCppClass(CppCodePattern.Output.CapsuleClass, entity);

		generateInitialize(cls);
		generateInject(cls);
		generateAdditionalElements(cls, behaviourElement, entity);

		return true;
	}

	/**
	 * Create and add an 'initialize' member function to the {@link CppClass} where the behaviour belongs.
	 * 
	 * <p>
	 * The 'initialize' method should be executed when the class or capsule is created.
	 * 
	 * <p>
	 * This will invoke {@link #generateInitializeBody}(CppClass, MemberFunction, Behaviour, Entity) to build the
	 * actual body. Subclasses of this class will implement that method.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateInitialize(CppClass cls) {
		MemberFunction initializeFunc = new MemberFunction(PrimitiveType.VOID, "initialize");
		initializeFunc.setVirtual();
		initializeFunc.add(new Parameter(UMLRTRuntime.UMLRTMessage.getType().const_().ref(), MESSAGE_PARAMETER_NAME));
		cls.addMember(CppClass.Visibility.PUBLIC, initializeFunc);

		generateInitializeBody(cls, initializeFunc, behaviourElement, context);
	}

	/**
	 * Create and add an 'inject' member function to the {@link CppClass} where the behaviour belongs.
	 * 
	 * <p>
	 * The 'inject' method should be executed whenever the class or capsule receives a message.
	 * 
	 * <p>
	 * This will invoke {@link #generateInjectBody}(CppClass, MemberFunction, Behaviour, Entity) to build the
	 * actual body. Subclasses of this class will implement that method.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateInject(CppClass cls) {
		MemberFunction injectFunc = new MemberFunction(PrimitiveType.VOID, "inject");
		injectFunc.setVirtual();
		injectFunc.add(new Parameter(UMLRTRuntime.UMLRTMessage.getType().const_().ref(), MESSAGE_PARAMETER_NAME));
		cls.addMember(CppClass.Visibility.PUBLIC, injectFunc);

		generateInjectBody(cls, injectFunc, behaviourElement, context);
	}

	/**
	 * Create and add the body of the 'initialize' member function. Sub-classes must implement this method.
	 * 
	 * @param cls
	 *            - The {@link CppClass} that contains the behaviour.
	 * @param initializeFunc
	 *            - The {@link MemberFunction} representing 'initialize'.
	 * @param behaviourElement
	 *            - The {@link Behaviour} element.
	 * @param context
	 *            - The {@link Entity} context element.
	 */
	protected abstract void generateInitializeBody(CppClass cls, MemberFunction initializeFunc, T behaviourElement, U context);

	/**
	 * Create and add the body of the 'inject' member function. Sub-classes must implement this method.
	 * 
	 * @param cls
	 *            - The {@link CppClass} that contains the behaviour.
	 * @param injectFunc
	 *            - The {@link MemberFunction} representing 'inject'.
	 * @param behaviourElement
	 *            - The {@link Behaviour} element.
	 * @param context
	 *            - The {@link Entity} context element.
	 */
	protected abstract void generateInjectBody(CppClass cls, MemberFunction injectFunc, T behaviourElement, U context);

	/**
	 * Create and add additional elements, if necessary to the {@link CppClass}.
	 * 
	 * @param cls
	 *            - The {@link CppClass} that contains the behaviour.
	 * @param behaviourElement
	 *            - The {@link Behaviour} element.
	 * @param context
	 *            - The {@link Entity} context element.
	 */
	protected abstract void generateAdditionalElements(CppClass cls, T behaviourElement, U context);

}
