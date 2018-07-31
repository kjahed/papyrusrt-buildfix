/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.papyrusrt.codegen.cpp.AbstractElementGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.cpp.TypesUtil;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime.UMLRTInOutSignal;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime.UMLRTInSignal;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime.UMLRTOutSignal;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Constructor;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppNamespace;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.ElementList;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Enumerator;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.LinkageSpec;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberField;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.OffsetOf;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Parameter;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.UserElement.GenerationTarget;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AbstractFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AddressOfExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BlockInitializer;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ConditionalDirective;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ConditionalDirective.Directive;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ConstructorCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ElementAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.Sizeof;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.StringLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.ReturnStatement;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.ProtocolBehaviourFeatureKind;
import org.eclipse.papyrusrt.xtumlrt.common.RedefinableElement;
import org.eclipse.papyrusrt.xtumlrt.common.Signal;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.RTSModelLibraryUtils;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;

/**
 * Generates C++ code for a given xtUMLrt Protocol.
 */
public class ProtocolGenerator extends AbstractElementGenerator {

	/** Name for the signal variable used in the generated signal functions. */
	private static final String SIGNAL_VARIABLE_NAME = "signal";

	/** The Protocol examined. */
	private final Protocol protocol;
	/**
	 * A map associating each model Signal to a C++ Variable that contains the
	 * payload information for the Signal.
	 */
	private final Map<Signal, Variable> payloadVariables = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            The CppCodePattern.
	 * @param protocol
	 *            The Protocol to be examined.
	 */
	public ProtocolGenerator(final CppCodePattern cpp, final Protocol protocol) {
		super(cpp);
		this.protocol = protocol;
	}

	@Override
	protected Output getOutputKind() {
		return Output.ProtocolClass;
	}

	@Override
	public String getLabel() {
		return super.getLabel() + ' ' + protocol.getName();
	}

	@Override
	public boolean generate() {
		ElementList elements = cpp.getElementList(CppCodePattern.Output.ProtocolClass, protocol);

		// Mark the protocol's containing namespace as writable.
		CppNamespace cppProtocol = cpp.getWritableCppNamespace(CppCodePattern.Output.ProtocolClass, protocol);
		CppClass baseRole = getRole(CppCodePattern.Output.ProtocolBaseRole);
		CppClass conjRole = getRole(CppCodePattern.Output.ProtocolConjugateRole);

		for (Signal signal : RTSModelLibraryUtils.getAllUserSignals(protocol)) {
			// Within the generated protocol class the signalId enumerator is
			// accessed directly.
			// Using the code pattern's access factory function would produce a
			// fully qualified
			// access expression.
			Enumerator sigEnumerator = cpp.getEnumerator(CppCodePattern.Output.SignalId, signal, XTUMLRTUtil.getOwner(signal));
			if (hasParameterWithStarAsType(signal)) {
				addSignalFunctionsStarAsType(elements, cppProtocol, baseRole, conjRole, signal, sigEnumerator);
			} else {
				addSignalFunctions(elements, cppProtocol, baseRole, conjRole, signal, sigEnumerator);
			}
		}

		return true;
	}

	/**
	 * Add signal member functions to the base and conjugate role classes for
	 * the protocol.
	 * 
	 * @param elements
	 *            ElementList where the protocol elements are added.
	 * @param cppProtocol
	 *            The generated C++ protocol namespace.
	 * @param baseRole
	 *            The generated C++ class for base role of the protocol.
	 * @param conjRole
	 *            The generated C++ class for conjugated role of the protocol.
	 * @param signal
	 *            The signal whose function is being generated.
	 * @param sigEnumerator
	 *            The signal's generated enumerator.
	 */
	private void addSignalFunctions(final ElementList elements, final CppNamespace cppProtocol,
			final CppClass baseRole, final CppClass conjRole, final Signal signal, final Enumerator sigEnumerator) {
		switch (signal.getKind()) {
		case IN: {
			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.IN));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.OUT));
			break;
		}
		case OUT: {
			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.OUT));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.IN));
			break;
		}
		case INOUT: {
			if (signal.getParameters().isEmpty()) {
				baseRole.addMember(CppClass.Visibility.PUBLIC,
						getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.INOUT));
				conjRole.addMember(CppClass.Visibility.PUBLIC,
						getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.INOUT));
			} else {
				baseRole.addMember(CppClass.Visibility.PUBLIC,
						getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.IN));
				conjRole.addMember(CppClass.Visibility.PUBLIC,
						getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.OUT));
				baseRole.addMember(CppClass.Visibility.PUBLIC,
						getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.OUT));
				conjRole.addMember(CppClass.Visibility.PUBLIC,
						getSignalFunction(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.IN));
			}
			break;
		}
		default:
			break;
		}
	}

	/**
	 * Add signal member functions to the base and conjugate role classes for
	 * the protocol when the type of a parameter is "star" (*) or left null.
	 * 
	 * @param elements
	 *            ElementList where the protocol elements are added.
	 * @param cppProtocol
	 *            The generated C++ protocol namespace.
	 * @param baseRole
	 *            The generated C++ class for base role of the protocol.
	 * @param conjRole
	 *            The generated C++ class for conjugated role of the protocol.
	 * @param signal
	 *            The signal whose function is being generated.
	 * @param sigEnumerator
	 *            The signal's generated enumerator.
	 */
	private void addSignalFunctionsStarAsType(final ElementList elements, final CppNamespace cppProtocol,
			final CppClass baseRole, final CppClass conjRole, final Signal signal, final Enumerator sigEnumerator) {
		switch (signal.getKind()) {
		case IN: {
			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithNoParam(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.IN));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithNoParam(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.OUT));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithRTTypedValueParam(elements, cppProtocol, signal,
							sigEnumerator));
			break;
		}
		case OUT: {
			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithNoParam(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.OUT));
			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithRTTypedValueParam(elements, cppProtocol, signal,
							sigEnumerator));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithNoParam(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.IN));
			break;
		}
		case INOUT: {
			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithNoParam(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.INOUT));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithNoParam(elements, cppProtocol, signal, sigEnumerator, ProtocolBehaviourFeatureKind.INOUT));

			baseRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithRTTypedValueParam(elements, cppProtocol, signal,
							sigEnumerator));
			conjRole.addMember(CppClass.Visibility.PUBLIC,
					getSignalFunctionWithRTTypedValueParam(elements, cppProtocol, signal,
							sigEnumerator));
			break;
		}
		default:
			break;
		}
	}

	@Override
	public List<FileName> getGeneratedFilenames() {
		List<FileName> result = new ArrayList<>();
		ElementList el = cpp.getElementList(CppCodePattern.Output.ProtocolClass, protocol);
		result.add(el.getName());
		return result;
	}

	/**
	 * Creates the C++ class for a Base or Conjugated Protocol role.
	 * 
	 * @param kind
	 *            Either ProtocolBaseRole or ProtocolConjugateRole.
	 * @return The C++ class.
	 */
	protected CppClass getRole(final CppCodePattern.Output kind) {
		CppClass cls = cpp.getCppClass(kind, protocol);
		AbstractFunctionCall baseCtorCall = generateBase(cls, kind);

		Constructor ctor = cpp.getConstructor(kind, protocol);
		Parameter param = new Parameter(UMLRTRuntime.UMLRTCommsPort.getType().ptr().const_().ref(), "srcPort");
		ctor.add(param);

		baseCtorCall.addArgument(new ElementAccess(param));
		ctor.addBaseInitializer(baseCtorCall);

		return cls;
	}

	/**
	 * Generate the base class for the given protocol role class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param kind
	 *            - The {@link CppCodePattern.Output} kind: ProtocolBaseRole or ProtocolConjugateRole
	 * @return The {@link AbstractFunctionCall} to the base constructor.
	 */
	protected AbstractFunctionCall generateBase(CppClass cls, CppCodePattern.Output kind) {
		AbstractFunctionCall baseCtorCall = null;
		RedefinableElement parent = protocol.getRedefines();
		if (parent instanceof Protocol && !RTSModelLibraryUtils.isBaseCommProtocol((Protocol) parent)) {
			CppClass base = cpp.getCppClass(kind, parent);
			cls.addBase(CppClass.Access.PUBLIC, base);
			baseCtorCall = new ConstructorCall(cpp.getConstructor(kind, parent));
		} else {
			cls.addBase(CppClass.Access.PUBLIC, UMLRTRuntime.UMLRTProtocol.Element);
			baseCtorCall = UMLRTRuntime.UMLRTProtocol.Ctor();
		}
		return baseCtorCall;
	}

	/**
	 * Creates the signal member function for a given model Signal in the given
	 * C++ protocol class.
	 * 
	 * @param elements
	 *            The C++ ElementList (compilation unit) where the protocol is
	 *            generated.
	 * @param cppProtocol
	 *            The C++ name space where the function is to be generated.
	 * @param signal
	 *            The model Signal
	 * @param sigEnumerator
	 *            The signal Id enumerator
	 * @return The new C++ MemberFunction.
	 */
	protected MemberFunction getSignalFunction(final ElementList elements, final CppNamespace cppProtocol,
			final Signal signal, final Enumerator sigEnumerator, final ProtocolBehaviourFeatureKind kind) {
		Type returnType = getSignalReturnType(kind);
		MemberFunction f = new MemberFunction(returnType, signal.getName(), Type.CVQualifier.CONST);

		Variable signalVar = new Variable(returnType, SIGNAL_VARIABLE_NAME);
		f.add(signalVar);

		Variable payload = getPayloadDescriptor(elements, cppProtocol, signal);

		AbstractFunctionCall initialize = UMLRTRuntime.UMLRTSignal
				.initialize(new ElementAccess(signalVar), new StringLiteral(signal.getName()),
						new ElementAccess(sigEnumerator), UMLRTRuntime.UMLRTProtocol.srcPort(),
						new AddressOfExpr(new ElementAccess(payload)));
		f.add(initialize);

		if (kind != ProtocolBehaviourFeatureKind.IN) {
			for (org.eclipse.papyrusrt.xtumlrt.common.Parameter param : signal.getParameters()) {
				Parameter p = createSignalParameter(param);
				f.add(p);
				initialize.addArgument(new AddressOfExpr(new ElementAccess(p)));
			}
		}

		f.add(new ReturnStatement(new ElementAccess(signalVar)));
		return f;
	}

	/**
	 * Returns the (Java representation) of the runtime type of the signal kind, a subclass of {@code UMLRTSignal}.
	 * 
	 * @param kind
	 *            - A {@link ProtocolBehaviourFeatureKind}, either {@code IN}, {@code OUT} or {@code INOUT}.
	 * @return A {@link Type}; the corresponding C++ meta-model representation of the runtime type for the signal kind,
	 *         this is, either a {@link UMLRTInSignal} (for {@code IN}), {@link UMLRTOutSignal} (for {@code OUT}) or
	 *         a {@link UMLRTInOutSignal} (for {@code INOUT}).
	 */
	private Type getSignalReturnType(final ProtocolBehaviourFeatureKind kind) {
		Type returnType = null;
		switch (kind) {
		case IN:
			returnType = UMLRTRuntime.UMLRTInSignal.getType();
			break;
		case OUT:
			returnType = UMLRTRuntime.UMLRTOutSignal.getType();
			break;
		case INOUT:
			returnType = UMLRTRuntime.UMLRTInOutSignal.getType();
		}
		return returnType;
	}

	/**
	 * Create a signal member function with no parameters for the case where the
	 * model Signal was declared with an untyped parameter.
	 * 
	 * @param elements
	 *            The C++ ElementList (compilation unit) where the protocol is
	 *            generated.
	 * @param cppProtocol
	 *            The C++ name space where the function is to be generated.
	 * @param signal
	 *            The model Signal
	 * @param sigEnumerator
	 *            The signal Id enumerator
	 * @return The new C++ MemberFunction.
	 */
	protected MemberFunction getSignalFunctionWithNoParam(final ElementList elements, final CppNamespace cppProtocol,
			final Signal signal, final Enumerator sigEnumerator, final ProtocolBehaviourFeatureKind kind) {
		Type returnType = getSignalReturnType(kind);
		MemberFunction f = new MemberFunction(returnType, signal.getName(), Type.CVQualifier.CONST);

		Variable signalVar = new Variable(returnType, SIGNAL_VARIABLE_NAME);
		f.add(signalVar);

		AbstractFunctionCall initialize = UMLRTRuntime.UMLRTSignal.initialize(new ElementAccess(signalVar),
				new StringLiteral(signal.getName()),
				new ElementAccess(sigEnumerator));
		f.add(initialize);

		f.add(new ReturnStatement(new ElementAccess(signalVar)));
		return f;
	}

	/**
	 * Create a signal member function with no parameters for the case where the
	 * model Signal was declared with an untyped parameter.
	 * 
	 * @param elements
	 *            The C++ ElementList (compilation unit) where the protocol is
	 *            generated.
	 * @param cppProtocol
	 *            The C++ name space where the function is to be generated.
	 * @param signal
	 *            The model Signal
	 * @param sigEnumerator
	 *            The signal Id enumerator
	 * @return The new C++ MemberFunction.
	 */
	protected MemberFunction getSignalFunctionWithRTTypedValueParam(final ElementList elements,
			final CppNamespace cppProtocol, final Signal signal, final Enumerator sigEnumerator) {
		MemberFunction f = new MemberFunction(UMLRTRuntime.UMLRTOutSignal.getType(), signal.getName(), Type.CVQualifier.CONST);

		String paramName = signal == null || signal.getParameters().get(0).getName() == null ? "data"
				: signal.getParameters().get(0).getName();
		Parameter data = new Parameter(UMLRTRuntime.UMLRTObject.UMLRTTypedValue.getType().const_().ref(), paramName);
		f.add(data);

		Variable signalVar = new Variable(UMLRTRuntime.UMLRTOutSignal.getType(), SIGNAL_VARIABLE_NAME);
		f.add(signalVar);

		AbstractFunctionCall initialize = UMLRTRuntime.UMLRTSignal
				.initialize(new ElementAccess(signalVar), new StringLiteral(signal.getName()),
						new ElementAccess(sigEnumerator), UMLRTRuntime.UMLRTProtocol.srcPort(),
						new MemberAccess(new ElementAccess(data),
								UMLRTRuntime.UMLRTObject.UMLRTTypedValue.type),
						new MemberAccess(new ElementAccess(data),
								UMLRTRuntime.UMLRTObject.UMLRTTypedValue.data));
		f.add(initialize);

		f.add(new ReturnStatement(new ElementAccess(signalVar)));
		return f;
	}

	/**
	 * Creates the C++ Parameter for a given xtUMLrt Parameter of a model
	 * Signal.
	 * 
	 * @param param
	 *            The xtUMLrt Parameter.
	 * @return The C++ Parameter.
	 */
	protected Parameter createSignalParameter(final org.eclipse.papyrusrt.xtumlrt.common.Parameter param) {
		Type paramType = TypesUtil.createCppType(cpp, param, param.getType());

		// Non-primitive types should be passed to the signal function as const
		// references.
		if (!(paramType instanceof PrimitiveType) && !paramType.isIndirect()) {
			paramType = paramType.const_().ref();
		}

		return new Parameter(paramType, param.getName());
	}

	/**
	 * Creates a payload descriptor C++ variable for a given model Signal,
	 * containing information about the number of parameters and their field
	 * descriptors.
	 * 
	 * @param elements
	 *            The C++ ElementList (compilation unit) where the protocol is
	 *            generated.
	 * @param cppProtocol
	 *            The C++ name space where the function is to be generated.
	 * @param signal
	 *            The model Signal
	 * @return The payload descriptor C++ Variable.
	 */
	protected Variable getPayloadDescriptor(final ElementList elements, final CppNamespace cppProtocol,
			final Signal signal) {
		Variable payload = payloadVariables.get(signal);
		if (payload == null) {

			BlockInitializer fieldsInit = new BlockInitializer(UMLRTRuntime.UMLRTObject.getFieldType().arrayOf(null));
			Variable fields = new Variable(LinkageSpec.STATIC, fieldsInit.getType(), "fields_" + signal.getName(), fieldsInit);
			elements.insertElement(fields, cppProtocol);

			Expression sizeofPayload = null;
			int signalParamCount = signal.getParameters().size();
			if (signalParamCount == 0) { // TODO - solves empty static array compiler error for MSVC. Emulating count of 1 with nulls
				sizeofPayload = addEmptyParameterSignalField(signal, fieldsInit);
			} else if (signalParamCount == 1) {
				sizeofPayload = addSingleParameterSignalField(signal, fieldsInit);
			} else if (signalParamCount > 1) {
				sizeofPayload = addMultiParameterSignalFields(elements, signal, fieldsInit, fields);
			}

			payload = new Variable(LinkageSpec.STATIC, UMLRTRuntime.UMLRTObject.getObjectType(),
					"payload_" + signal.getName(),
					new BlockInitializer(UMLRTRuntime.UMLRTObject.getObjectType(),
							sizeofPayload == null ? new IntegralLiteral(
									0)
									: sizeofPayload,
							getNumFields(fields, signalParamCount),
							new ElementAccess(fields)));
			elements.insertElement(payload, cppProtocol);

			payloadVariables.put(signal, payload);
		}

		return payload;
	}

	/**
	 * @param fields
	 *            - The {@link Variable} that contains the fields array.
	 * @return The {@link Expression} representing the number of fields.
	 */
	private Expression getNumFields(Variable fields, int signalParamCount) {
		Expression initInstances = new IntegralLiteral(fields.getNumInitializedInstances());
		if (signalParamCount == 0) {
			ConditionalDirective conditionalDirective = new ConditionalDirective(Directive.IFDEF, "NEED_NON_FLEXIBLE_ARRAY", initInstances);
			conditionalDirective.defaultBlock().add(new IntegralLiteral(0));
			return conditionalDirective;
		} else {
			return initInstances;
		}
	}

	private Expression addEmptyParameterSignalField(final Signal signal, final BlockInitializer fieldsInit) {
		ConditionalDirective conditionalDirective = new ConditionalDirective(Directive.IFDEF, "NEED_NON_FLEXIBLE_ARRAY",
				new BlockInitializer(
						UMLRTRuntime.UMLRTObject.getFieldType(),
						new IntegralLiteral(0),
						new IntegralLiteral(0),
						new IntegralLiteral(0),
						new IntegralLiteral(0),
						new IntegralLiteral(0)));
		fieldsInit.addExpression(conditionalDirective);
		return null;
	}

	/**
	 * Creates the field descriptors for each signal parameter and a "packing"
	 * struct consisting of all those parameters.
	 * 
	 * This method is used only for signals with multiple parameters.
	 * 
	 * @param elements
	 *            ElementList where the protocol elements are added.
	 * @param signal
	 *            The signal whose function is being generated.
	 * @param fieldsInit
	 *            The field descriptors array block initializer where the
	 *            parameter's field descriptors will be added.
	 * @param fields
	 *            The C++ variable with the field descriptors
	 * @return The sizeof expression for the packing struct.
	 */
	private Expression addMultiParameterSignalFields(final ElementList elements, final Signal signal,
			final BlockInitializer fieldsInit, final Variable fields) {
		Expression sizeofPayload = null;
		CppClass packingStruct = new CppClass(GenerationTarget.DEFN_ONLY, CppClass.Kind.STRUCT, "params_" + signal.getName());
		elements.insertElement(packingStruct, fields);

		for (org.eclipse.papyrusrt.xtumlrt.common.Parameter param : signal.getParameters()) {
			String fieldName = param.getName();

			packingStruct
					.addMember(CppClass.Visibility.PUBLIC,
							new MemberField(TypesUtil.createCppType(cpp, param, param.getType()), fieldName));

			fieldsInit.addExpression(new BlockInitializer(UMLRTRuntime.UMLRTObject.getFieldType(),
					new StringLiteral(fieldName),
					new AddressOfExpr(TypesUtil.createRTTypeAccess(cpp, param, param.getType())),
					new OffsetOf(packingStruct, fieldName),
					new IntegralLiteral(1), new IntegralLiteral(0)));

			sizeofPayload = new Sizeof(packingStruct);
		}
		return sizeofPayload;
	}

	/**
	 * Creates the field descriptors for the signal parameter.
	 * 
	 * This method is used only for signals with one parameter.
	 * 
	 * @param signal
	 *            The signal whose function is being generated.
	 * @param fieldsInit
	 *            The field descriptors array block initializer where the
	 *            parameter's field descriptors will be added.
	 * @return The sizeof expression of the parameter's type.
	 */
	private Expression addSingleParameterSignalField(final Signal signal, final BlockInitializer fieldsInit) {
		Expression sizeofPayload;
		org.eclipse.papyrusrt.xtumlrt.common.Parameter param = signal.getParameters().get(0);

		fieldsInit.addExpression(new BlockInitializer(UMLRTRuntime.UMLRTObject.getFieldType(),
				new StringLiteral(param.getName()),
				new AddressOfExpr(TypesUtil.createRTTypeAccess(cpp, param, param.getType())),
				new IntegralLiteral(0), new IntegralLiteral(1),
				new IntegralLiteral(0)));

		sizeofPayload = new Sizeof(TypesUtil.createCppType(cpp, param, param.getType()));
		return sizeofPayload;
	}

	/**
	 * Returns whether the given model Signal has at least one Parameter with no
	 * type.
	 * 
	 * @param signal
	 *            An xtUMLrt Signal
	 * @return True if the Signal is not null and has at least one parameter
	 *         with null type.
	 */
	protected boolean hasParameterWithStarAsType(final Signal signal) {
		boolean result = false;
		if (signal != null) {
			EList<org.eclipse.papyrusrt.xtumlrt.common.Parameter> parameters = signal.getParameters();
			if (parameters != null && !(parameters.isEmpty())) {
				org.eclipse.papyrusrt.xtumlrt.common.Parameter param = parameters.get(0);
				result = param.getType() == null;
			}
		}
		return result;
	}

}
