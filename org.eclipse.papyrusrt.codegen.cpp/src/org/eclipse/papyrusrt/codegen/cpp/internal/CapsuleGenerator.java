/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.instance.model.CapsuleInstance;
import org.eclipse.papyrusrt.codegen.instance.model.ICapsuleInstance;
import org.eclipse.papyrusrt.codegen.instance.model.IPortInstance;
import org.eclipse.papyrusrt.codegen.instance.model.IPortInstance.IFarEnd;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Constructor;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.ElementList;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Enumerator;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Function;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.LinkageSpec;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberField;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Parameter;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AbstractFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AddressOfExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BinaryOperation;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BlockInitializer;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BooleanLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ConstructorCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.CppEnumOrderedInitializer;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ElementAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ExpressionBlob;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IndexExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.NewExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.StringLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.StandardLibrary;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.CodeBlock;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.ConditionalStatement;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.SwitchClause;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.SwitchStatement;
import org.eclipse.papyrusrt.xtumlrt.aexpr.uml.XTUMLRTBoundsEvaluator;
import org.eclipse.papyrusrt.xtumlrt.common.Attribute;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.CapsuleKind;
import org.eclipse.papyrusrt.xtumlrt.common.CapsulePart;
import org.eclipse.papyrusrt.xtumlrt.common.Port;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.RedefinableElement;
import org.eclipse.papyrusrt.xtumlrt.common.ValueSpecification;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.RTSModelLibraryUtils;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTExtensions;

/**
 * The {@link Capsule} generator.
 * 
 * @author Ernesto Posse
 */
public class CapsuleGenerator extends BasicClassGenerator {

	/** The model {@link Capsule} element for which code is to be generated. */
	protected final Capsule capsule;

	/** Whether internal ports have been found. */
	protected Boolean internalPortsFound;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param capsule
	 *            - The {@link Capsule}.
	 */
	public CapsuleGenerator(CppCodePattern cpp, Capsule capsule) {
		super(cpp, capsule);
		this.capsule = capsule;
	}

	@Override
	protected Output getOutputKind() {
		return Output.CapsuleClass;
	}

	@Override
	public String getLabel() {
		return super.getLabel() + ' ' + capsule.getName();
	}

	@Override
	public boolean generate() {
		CppClass cls = cpp.getWritableCppClass(CppCodePattern.Output.CapsuleClass, capsule);
		RedefinableElement element = capsule.getRedefines();
		Capsule modelSuperClass = null;
		if (element != null && element instanceof Capsule) {
			modelSuperClass = (Capsule) element;
		}
		CppClass superClass = null;
		if (modelSuperClass == null) {
			cls.addBase(CppClass.Access.PUBLIC, UMLRTRuntime.UMLRTCapsule.Element);
		} else {
			// In UML-RT a capsule may have one super-class at most.
			superClass = cpp.getWritableCppClass(CppCodePattern.Output.CapsuleClass, modelSuperClass);
			cls.addBase(CppClass.Access.PUBLIC, superClass);
		}

		Parameter param_cd = new Parameter(UMLRTRuntime.UMLRTCapsuleClass.getType().const_().ptr(), "cd");
		Parameter param_st = new Parameter(UMLRTRuntime.UMLRTSlot.getType().ptr(), "st");
		Parameter param_bp = new Parameter(UMLRTRuntime.UMLRTCommsPort.getType().ptr().ptr().const_(), "border");
		Parameter param_internal = new Parameter(UMLRTRuntime.UMLRTCommsPort.getType().ptr().ptr().const_(), "internal");
		Parameter param_stat = new Parameter(PrimitiveType.BOOL, "isStat");

		Constructor ctor = cpp.getConstructor(CppCodePattern.Output.CapsuleClass, capsule);
		ctor.add(param_cd);
		ctor.add(param_st);
		ctor.add(param_bp);
		ctor.add(param_internal);
		ctor.add(param_stat);

		AbstractFunctionCall baseCtorCall = null;
		if (modelSuperClass == null) {
			baseCtorCall = UMLRTRuntime.UMLRTCapsule.Ctor(
					StandardLibrary.NULL(), // we don't use the RTS interface
					new ElementAccess(param_cd),
					new ElementAccess(param_st),
					new ElementAccess(param_bp),
					new ElementAccess(param_internal),
					new ElementAccess(param_stat));
		} else {
			baseCtorCall = new ConstructorCall(cpp.getConstructor(CppCodePattern.Output.CapsuleClass, modelSuperClass));
			baseCtorCall.addArgument(new ElementAccess(param_cd));
			baseCtorCall.addArgument(new ElementAccess(param_st));
			baseCtorCall.addArgument(new ElementAccess(param_bp));
			baseCtorCall.addArgument(new ElementAccess(param_internal));
			baseCtorCall.addArgument(new ElementAccess(param_stat));
		}
		ctor.addBaseInitializer(baseCtorCall);

		// Connect all ports using a shallowly connected capsule instance.
		CapsuleInstance instance = new CapsuleInstance(capsule);
		instance.connect(null, true);

		// If the Capsule generation has been successful then add on the operations and attributes
		// in the base generator.
		return generatePorts(cls, ctor)
				&& generateParts(cls, instance, ctor)
				&& generateRTSFunctions(cls, instance)
				&& super.generate(cls);
	}

	@Override
	protected MemberField generate(CppClass cls, Attribute attr) {
		MemberField field = super.generate(cls, attr);
		if (field == null) {
			return null;
		}

		Constructor ctor = cpp.getConstructor(CppCodePattern.Output.CapsuleClass, capsule);
		if (ctor == null) {
			return field;
		}

		// Add initializers for all non-static attributes that have initial values.
		if (!attr.isStatic()) {
			ValueSpecification defVal = attr.getDefault();
			if (defVal != null) {
				String def = XTUMLRTUtil.getStringValue(defVal);
				if (def != null && !def.isEmpty()) {
					ctor.addFieldInitializer(field, new ExpressionBlob(def));
				}
			}
		}

		return field;
	}

	/**
	 * Generate member functions for each part. The generated function looks roughly like this:
	 * 
	 * {@code
	 * const UMLRTCapsuleRole * role_name() const
	 * {
	 * return slot->capsuleClass->subcapsuleRoles[...];
	 * }
	 * }
	 * 
	 * @param capsuleClass
	 *            - The {@link CppClass} for the capsule.
	 * @param instance
	 *            - The {@link CapsuleInstance} of the capsule.
	 * @param ctor
	 *            - The {@link Constructor} of the class.
	 * @return {@code true} if successful.
	 */
	protected boolean generateParts(CppClass capsuleClass, CapsuleInstance instance, Constructor ctor) {
		CppEnumOrderedInitializer subCapsuleInit = new CppEnumOrderedInitializer(
				cpp.getIdEnum(CppCodePattern.Output.PartId, capsule),
				UMLRTRuntime.UMLRTCapsuleRole.getType().arrayOf(null));
		Variable subCapsules = new Variable(LinkageSpec.STATIC, UMLRTRuntime.UMLRTCapsuleRole.getType().arrayOf(null).const_(), "roles", subCapsuleInit);

		for (CapsulePart part : XTUMLRTExtensions.getAllCapsuleParts(capsule)) {
			// The RTS does not allow an optional plugin role.
			boolean isPlugin = part.getKind() == CapsuleKind.PLUGIN;
			subCapsuleInit.putExpression(
					cpp.getEnumerator(CppCodePattern.Output.PartId, part, capsule),
					new BlockInitializer(
							UMLRTRuntime.UMLRTCapsulePart.getType(),
							new StringLiteral(part.getName()),
							new AddressOfExpr(new ElementAccess(cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, part.getType()))),
							new IntegralLiteral(XTUMLRTBoundsEvaluator.getLowerBound(part)),
							new IntegralLiteral(XTUMLRTBoundsEvaluator.getUpperBound(part)),
							new BooleanLiteral(!isPlugin && XTUMLRTBoundsEvaluator.getUpperBound(part) > XTUMLRTBoundsEvaluator.getLowerBound(part)),
							new BooleanLiteral(isPlugin)));

			MemberField field = new MemberField(
					UMLRTRuntime.UMLRTCapsulePart.getType().const_().constPtr(),
					part.getName());

			capsuleClass.addMember(CppClass.Visibility.PROTECTED, field);

			Enumerator partId = cpp.getEnumerator(CppCodePattern.Output.PartId, part, capsule);
			Expression exp = new AddressOfExpr(
					new IndexExpr(
							new MemberAccess(UMLRTRuntime.UMLRTCapsule.slot(), UMLRTRuntime.UMLRTSlot.parts),
							new ElementAccess(partId)));
			ctor.addFieldInitializer(field, exp);

		}

		if (subCapsuleInit.getNumInitializers() > 0) {
			cpp.getElementList(CppCodePattern.Output.UMLRTCapsuleClass, capsule).addElement(subCapsules);
		} else {
			subCapsules = null;
		}

		generateUMLRTCapsuleClass(subCapsules, instance);
		return true;
	}

	/**
	 * An enum to represent the different kinds of ports known by the runtime.
	 */
	protected enum PortKind {
		Border(CppCodePattern.Output.BorderPortId, UMLRTRuntime.UMLRTCapsule.borderPortsField), Internal(CppCodePattern.Output.InternalPortId, UMLRTRuntime.UMLRTCapsule.internalPortsField);

		/** The kind of {@link CppCodePattern.Output}. */
		public final CppCodePattern.Output cppOutput;

		/** The C++ {@link org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement} field to be inherited in the generated code. */
		protected final org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement inheritedField;

		/**
		 * Constructor.
		 *
		 * @param cppOutput
		 *            - The {@link CppCodePattern.Output}.
		 * @param inheritedField
		 *            - The C++ {@link rg.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement} field to be inherited in the generated code.
		 */
		PortKind(CppCodePattern.Output cppOutput, org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement inheritedField) {
			this.cppOutput = cppOutput;
			this.inheritedField = inheritedField;
		}

		/**
		 * Generate an access to the port id enumerator.
		 * 
		 * @param cpp
		 *            - The {@link CppCodePattern}.
		 * @param capsule
		 *            - The {@link Capsule}.
		 * @param port
		 *            - The {@link Port}
		 * @return The {@link Expression} with the enumerator access.
		 */
		public Expression generatePortIdAccess(CppCodePattern cpp, Capsule capsule, Port port) {
			return cpp.getEnumeratorAccess(cppOutput, port, capsule);
		}

		/**
		 * Generates an expression to access the specified port. This overrides the default case
		 * to access the given array variable. This is used when generating non-member functions
		 * like instantiate.
		 * 
		 * @param cpp
		 *            - The {@link CppCodePattern}.
		 * @param arrayVarAccess
		 *            - The {@link Expression} representing the access to the array variable.
		 * @param capsule
		 *            - The model {@link Capsule}.
		 * @param port
		 *            - The model {@link Port}.
		 * @return The {@link IndexExpr} representing the port access.
		 */
		public Expression generatePortAccess(CppCodePattern cpp, Expression arrayVarAccess, Capsule capsule, Port port) {
			return new IndexExpr(arrayVarAccess, generatePortIdAccess(cpp, capsule, port));
		}

		/**
		 * @return An {@link ElementAccess} to {@link #inheritedField}.
		 */
		public Expression createArrayVarAccess() {
			return new ElementAccess(inheritedField);
		}

		/**
		 * Generates an expression to access the specified port. This default case uses the
		 * port array variable that is inherited from the UMLRTCapsule base class.
		 * 
		 * @param cpp
		 *            - The {@link CppCodePattern}.
		 * @param capsule
		 *            - The {@link Capsule}.
		 * @param port
		 *            - The {@link Port}.
		 * @return The port access {@link Expression}.
		 */
		public Expression generatePortAccess(CppCodePattern cpp, Capsule capsule, Port port) {
			return generatePortAccess(cpp, createArrayVarAccess(), capsule, port);
		}
	}

	/**
	 * Generates an expression to access the given port. It delegates to {@link PortKind#generatePortIdAccess(CppCodePattern, Capsule, Port)} depending on
	 * whether the port is a border port or an internal port.
	 * 
	 * @param port
	 *            - A {@link Port}.
	 * @return The {@link Expression} to access the port.
	 */
	protected Expression generatePortAccess(Port port) {
		return (XTUMLRTUtil.isNonBorderPort(port) ? PortKind.Internal : PortKind.Border).generatePortAccess(cpp, capsule, port);
	}

	/**
	 * Generates an expression to access the slot that contains or will contain a given capsule instance
	 * given the expression for a capsule's slot. The given slot is the capsule instance whose type
	 * is a capsule with a part to be filled with the given capsule instance. The resulting expression
	 * looks like this:
	 * 
	 * <p>
	 * {@code slot->parts[<part-id>].slots[<index>]}
	 * 
	 * @param slot
	 *            - The {@link Expression} for the slot of the containing capsule instance.
	 * @param capsuleInstance
	 *            - The sub-{@link ICapsuleInstance}.
	 * @return The slot access {@link Expression}.
	 */
	protected Expression generateSlotAccess(Expression slot, ICapsuleInstance capsuleInstance) {
		Expression partArrayAccess = new IndexExpr(
				new MemberAccess(slot, UMLRTRuntime.UMLRTSlot.parts),
				cpp.getEnumeratorAccess(CppCodePattern.Output.PartId, capsuleInstance.getCapsulePart(), capsule));
		Expression slotAccess = new IndexExpr(
				new MemberAccess(partArrayAccess, UMLRTRuntime.UMLRTCapsulePart.slots),
				new IntegralLiteral(capsuleInstance.getIndex()));
		return slotAccess;
	}

	/**
	 * Generates an expression that accesses a port instance. This usually would be something like
	 * the following for border ports:
	 * 
	 * <p>
	 * {@code &(slot->parts[<part-id>].slots[<part-index>]->ports[<port-id>])}
	 * 
	 * @param slot
	 *            - The {@link Expression} for the slot of the containing capsule instance.
	 * @param capsuleInstance
	 *            - The sub-{@link ICapsuleInstance}.
	 * @param port
	 *            - The {@link Port}
	 * @return The access {@link Expression}.
	 */
	protected Expression generatePortAccess(Expression slot, ICapsuleInstance capsuleInstance, Port port) {
		if (XTUMLRTUtil.isNonBorderPort(port)) {
			return PortKind.Internal.generatePortAccess(cpp, capsule, port);
		}

		Expression portsAccess = new MemberAccess(
				generateSlotAccess(slot, capsuleInstance),
				UMLRTRuntime.UMLRTSlot.ports);
		return new AddressOfExpr(PortKind.Border.generatePortAccess(cpp, portsAccess, capsuleInstance.getType(), port));
	}

	/**
	 * Delegates to {@link PortKind#generatePortAccess(CppCodePattern, Expression, Capsule, Port)}.
	 * 
	 * @param portKind
	 *            - The {@link PortKind}.
	 * @param arrayVar
	 *            - The {@link org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement} array variable.
	 * @param port
	 *            - The {@link IPortInstance}.
	 * @return The port access {@link Expression}.
	 */
	protected Expression generatePortAccess(PortKind portKind, org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement arrayVar, IPortInstance port) {
		return portKind.generatePortAccess(cpp, new ElementAccess(arrayVar), port.getContainer().getType(), port.getType());
	}

	/**
	 * Delegates to {@link #generatePortAccess(PortKind, org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement, IPortInstance)}.
	 * 
	 * @param portKind
	 *            - The {@link PortKind}.
	 * @param borderPorts
	 *            - The {@link org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement} borderPorts variable.
	 * @param port
	 *            - The {@link IPortInstance}.
	 * @return The port access {@link Expression}.
	 */
	protected Expression generateBorderPortAccess(org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement borderPorts, IPortInstance port) {
		return generatePortAccess(PortKind.Border, borderPorts, port);
	}

	/**
	 * Generates member fields and field initializers for each port in the capsule.
	 * 
	 * @param capsuleClass
	 *            - The {@link CppClass} where the ports will be added.
	 * @param ctor
	 *            - The C++ {@link Constructor} where the ports will be initialized.
	 * @return {@code true} iff successful.
	 */
	protected boolean generatePorts(CppClass capsuleClass, Constructor ctor) {
		for (Port port : XTUMLRTExtensions.getAllRTPorts(capsule)) {
			Protocol protocol = port.getType();
			if (protocol == null) {
				continue;
			}

			CppCodePattern.Output roleKind = port.isConjugate() ? CppCodePattern.Output.ProtocolConjugateRole : CppCodePattern.Output.ProtocolBaseRole;

			// Discover the type of the protocol role based on whether it is system
			// or user defined.
			Type protocolRoleType = RTSModelLibraryUtils.isSystemElement(protocol)
					? UMLRTRuntime.getSystemProtocolRole(protocol, roleKind == Output.ProtocolBaseRole)
					: cpp.getCppClass(roleKind, protocol).getType();

			// Some system-defined protocols may not provide an appropriate role. In this
			// case the type will be null -- the port should be ignored.
			// For example, the conjugate role of the Frame protocol does not exist.
			if (protocolRoleType != null) {
				MemberField field = new MemberField(protocolRoleType, port.getName());
				capsuleClass.addMember(CppClass.Visibility.PROTECTED, field);
				if (UMLRTRuntime.needsUMLRTCommsPort(protocol)) {
					ctor.addFieldInitializer(field, generatePortAccess(port));
				}
			}
		}

		return true;
	}

	/**
	 * @return {@code true} if the {@link #capsule} has internal ports.
	 */
	protected boolean hasInternalPorts() {
		if (internalPortsFound != null) {
			return internalPortsFound.booleanValue();
		}

		for (Port port : XTUMLRTExtensions.getAllRTPorts(capsule)) {
			if (XTUMLRTUtil.isNonBorderPort(port)) {
				internalPortsFound = Boolean.TRUE;
				return internalPortsFound;
			}
		}
		internalPortsFound = Boolean.FALSE;
		return internalPortsFound;
	}

	/**
	 * Generate the "instantiate" function for this capsule.
	 * 
	 * <p>
	 * The instantiate function creates a new instance of the user's capsule.
	 * 
	 * @param instance
	 *            - The {@link CapsuleInstance}.
	 * @return The {@link Function}.
	 */
	protected Function generateInstantiate(CapsuleInstance instance) {
		ElementList elementList = cpp.getElementList(CppCodePattern.Output.UMLRTCapsuleClass, capsule);

		Parameter slot = new Parameter(UMLRTRuntime.UMLRTSlot.getType().ptr(), "slot");
		Parameter borderPorts = new Parameter(UMLRTRuntime.UMLRTCommsPort.getType().ptr().ptr().const_(), "borderPorts");

		Function instantiate = new Function(LinkageSpec.STATIC, PrimitiveType.VOID, "instantiate_" + capsule.getName());
		instantiate.add(new Parameter(UMLRTRuntime.UMLRTRtsInterface.getType().const_().ptr(), "rts"));
		instantiate.add(slot);
		instantiate.add(borderPorts);

		Variable internalPorts = null;
		if (hasInternalPorts()) {
			Variable capsuleClass = cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, capsule);
			internalPorts = new Variable(
					UMLRTRuntime.UMLRTCommsPort.getType().ptr().ptr().const_(),
					"internalPorts",
					UMLRTRuntime.UMLRTFrameService.createInternalPorts(
							new ElementAccess(slot),
							new AddressOfExpr(new ElementAccess(capsuleClass))));
			instantiate.add(internalPorts);
		}

		ConstructorCall ctorCall = new ConstructorCall(cpp.getConstructor(CppCodePattern.Output.CapsuleClass, capsule));
		ctorCall.addArgument(
				new AddressOfExpr(
						new ElementAccess(cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, capsule))));
		ctorCall.addArgument(new ElementAccess(slot));
		ctorCall.addArgument(new ElementAccess(borderPorts));
		ctorCall.addArgument(internalPorts == null ? StandardLibrary.NULL() : new ElementAccess(internalPorts));
		// instantiate produces only non-static instances
		ctorCall.addArgument(BooleanLiteral.FALSE());

		Set<Port> passThroughPorts = new HashSet<>();
		// Connect all border and internal ports as needed.
		for (IPortInstance port : instance.getPorts()) {
			int localIndex = 0;
			if (XTUMLRTUtil.isNonBorderPort(port.getType())) {
				for (IPortInstance.IFarEnd far : port.getFarEnds()) {
					instantiate.add(
							UMLRTRuntime.UMLRTFrameService.connectPorts(
									generatePortAccess(PortKind.Internal, internalPorts, port),
									new IntegralLiteral(localIndex++),
									generatePortAccess(new ElementAccess(slot), far.getContainer(), far.getType()),
									new IntegralLiteral(far.getIndex())));
				}
			} else if (XTUMLRTUtil.isRelayPort(port.getType())) {
				for (IPortInstance.IFarEnd far : port.getFarEnds()) {
					IPortInstance farEndOwner = far.getOwner();
					if (isRelayBorderPortInstance(farEndOwner)) {
						if (passThroughPorts.contains(port.getType())) {
							continue;
						}
						passThroughPorts.add(farEndOwner.getType());
						Expression p1 = generatePortAccess(port.getType());
						Expression p1Index = new IntegralLiteral(localIndex++);
						Expression p2 = generatePortAccess(farEndOwner.getType());
						Expression p2Index = new IntegralLiteral(far.getIndex());
						instantiate.add(
								UMLRTRuntime.UMLRTFrameService.connectFarEnds(p1, p1Index, p2, p2Index));
					} else {
						instantiate.add(UMLRTRuntime.UMLRTFrameService.connectRelayPort(
								generateBorderPortAccess(borderPorts, port),
								new IntegralLiteral(localIndex++),
								far.getContainer().getCapsulePart() == null
										? generateBorderPortAccess(borderPorts, far.getOwner())
										: generatePortAccess(new ElementAccess(slot), far.getContainer(), far.getType()),
								new IntegralLiteral(far.getIndex())));
					}
				}
			} else {
				for (IPortInstance.IFarEnd far : port.getFarEnds()) {
					instantiate.add(
							UMLRTRuntime.UMLRTFrameService.connectPorts(
									generateBorderPortAccess(borderPorts, port),
									new IntegralLiteral(localIndex++),
									far.getContainer().getCapsulePart() == null
											? generateBorderPortAccess(borderPorts, far.getOwner())
											: generatePortAccess(new ElementAccess(slot), far.getContainer(), far.getType()),
									new IntegralLiteral(far.getIndex())));
				}
			}
		}

		// Connect all part border ports to each other.
		Set<ICapsuleInstance> connected = new HashSet<>();
		connected.add(instance);
		for (ICapsuleInstance sub : instance.getContained()) {
			for (IPortInstance port : sub.getPorts()) {
				if (XTUMLRTUtil.isNonBorderPort(port.getType())) {
					continue;
				}

				int localId = -1;
				for (IPortInstance.IFarEnd far : port.getFarEnds()) {
					// This is incremented first so that it will allow for farEnds that are skipped.
					++localId;

					// Ignore farEnds that are not connected ones that are for ports on parts that
					// have already been considered.
					if (connected.contains(far.getContainer())) {
						continue;
					}

					AbstractFunctionCall connectCall = UMLRTRuntime.UMLRTFrameService.connectPorts(
							generatePortAccess(new ElementAccess(slot), sub, port.getType()),
							new IntegralLiteral(localId),
							generatePortAccess(new ElementAccess(slot), far.getContainer(), far.getType()),
							new IntegralLiteral(far.getIndex()));

					instantiate.add(connectCall);
				}
			}

			connected.add(sub);
		}

		// Instantiate all non-optional instances.
		for (ICapsuleInstance sub : instance.getContained()) {
			if (sub.isDynamic()) {
				continue;
			}

			AbstractFunctionCall instantiateCall = new MemberFunctionCall(
					new ElementAccess(cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, sub.getType())),
					UMLRTRuntime.UMLRTCapsuleClass.instantiate);
			// we don't use the rts interface
			instantiateCall.addArgument(StandardLibrary.NULL());
			instantiateCall.addArgument(
					new IndexExpr(
							new MemberAccess(
									new IndexExpr(
											new MemberAccess(new ElementAccess(slot), UMLRTRuntime.UMLRTSlot.parts),
											cpp.getEnumeratorAccess(CppCodePattern.Output.PartId, sub.getCapsulePart(), capsule)),
									UMLRTRuntime.UMLRTCapsulePart.slots),
							new IntegralLiteral(sub.getIndex())));
			instantiateCall.addArgument(
					UMLRTRuntime.UMLRTFrameService.createBorderPorts(
							new IndexExpr(
									new MemberAccess(
											new IndexExpr(
													new MemberAccess(new ElementAccess(slot), UMLRTRuntime.UMLRTSlot.parts),
													cpp.getEnumeratorAccess(CppCodePattern.Output.PartId, sub.getCapsulePart(), capsule)),
											UMLRTRuntime.UMLRTCapsulePart.slots),
									new IntegralLiteral(sub.getIndex())),
							new MemberAccess(
									new ElementAccess(cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, sub.getCapsulePart().getType())),
									UMLRTRuntime.UMLRTCapsuleClass.numPortRolesBorder)));

			instantiate.add(instantiateCall);
		}

		instantiate.add(
				new BinaryOperation(
						new MemberAccess(new ElementAccess(slot), UMLRTRuntime.UMLRTSlot.capsule),
						BinaryOperation.Operator.ASSIGN,
						new NewExpr(ctorCall)));

		elementList.addElement(instantiate);

		return instantiate;
	}

	/**
	 * Generate the "bindPort" function.
	 * 
	 * @param cls
	 *            - The {@link CppClass} where the function will be added.
	 * @param instance
	 *            - The {@link CapsuleInstance}.
	 * @return {@code true} iff successful.
	 */
	protected boolean generateBindPortFunction(CppClass cls, CapsuleInstance instance) {
		MemberFunction func = new MemberFunction(PrimitiveType.VOID, "bindPort");
		func.setVirtual();
		cls.addMember(CppClass.Visibility.PUBLIC, func);

		Parameter isBorder = new Parameter(PrimitiveType.BOOL, "isBorder");
		Parameter portId = new Parameter(PrimitiveType.INT, "portId");
		Parameter index = new Parameter(PrimitiveType.INT, "index");
		func.add(isBorder);
		func.add(portId);
		func.add(index);

		ConditionalStatement isBorderCond = new ConditionalStatement();

		CodeBlock isBorderBlock = isBorderCond.add(new ElementAccess(isBorder));
		SwitchStatement isBorderSwitch = new SwitchStatement(new ElementAccess(portId));
		isBorderBlock.add(isBorderSwitch);

		CodeBlock isNotBorderBlock = null;

		boolean hasContent = false;
		Set<Port> passThroughPorts = new HashSet<>();

		for (IPortInstance port : instance.getPorts()) {
			if (!XTUMLRTUtil.isWired(port.getType())) {
				continue;
			}

			if (XTUMLRTUtil.isNonBorderPort(port.getType())) {
				hasContent = true;

				if (isNotBorderBlock == null) {
					isNotBorderBlock = isBorderCond.defaultBlock();
				}

				isNotBorderBlock.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnbound(
								PortKind.Internal.createArrayVarAccess(),
								PortKind.Internal.generatePortIdAccess(cpp, capsule, port.getType()),
								new ElementAccess(index),
								BooleanLiteral.TRUE()));
			} else if (XTUMLRTUtil.isRelayPort(port.getType())) {
				SwitchClause clause = generateBindRelayPortClause(port, portId, index, passThroughPorts);
				if (clause != null) {
					hasContent = true;
					isBorderSwitch.add(clause);
				}
			} else {
				SwitchClause clause = new SwitchClause(cpp.getEnumeratorAccess(CppCodePattern.Output.BorderPortId, port.getType(), capsule));
				clause.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnbound(
								PortKind.Border.createArrayVarAccess(),
								PortKind.Border.generatePortIdAccess(cpp, capsule, port.getType()),
								new ElementAccess(index),
								BooleanLiteral.TRUE()));

				hasContent = true;
				isBorderSwitch.add(clause);
			}
		}

		if (hasContent) {
			func.add(isBorderCond);
		}

		return true;
	}

	/**
	 * Generated the switch clause for binding a border relay port in the 'bindPort' function.
	 * 
	 * @param port
	 *            - The {@link IPortInstance}.
	 * @param portId
	 *            - The port id {@link Parameter} of the 'bindPort' function.
	 * @param index
	 *            - The port index {@link Parameter} of the 'bindPort' function.
	 * @param passThroughPorts
	 *            - The {@link Set} of "pass-through" {@link Port}s (relay border ports connected to other relay border ports).
	 * @return The {@link SwitchClause}.
	 */
	protected SwitchClause generateBindRelayPortClause(IPortInstance port, Parameter portId, Parameter index, Set<Port> passThroughPorts) {
		Iterable<? extends IFarEnd> farEnds = port.getFarEnds();

		if (farEnds == null || farEnds.iterator() == null || !farEnds.iterator().hasNext()) {
			return null;
		}

		SwitchClause clause;
		SwitchClause subClause;
		SwitchStatement portInstanceSwitch = new SwitchStatement(new ElementAccess(index));
		IPortInstance farEndOwner;

		clause = new SwitchClause(cpp.getEnumeratorAccess(CppCodePattern.Output.BorderPortId, port.getType(), capsule));
		clause.add(portInstanceSwitch);

		int farEndIndex = 0;
		for (IPortInstance.IFarEnd far : port.getFarEnds()) {
			farEndOwner = far.getOwner();
			subClause = new SwitchClause(new IntegralLiteral(farEndIndex++));
			if (isRelayBorderPortInstance(farEndOwner)) {
				if (passThroughPorts.contains(port.getType())) {
					continue;
				}
				passThroughPorts.add(farEndOwner.getType());
				Expression p1 = generatePortAccess(port.getType());
				Expression p1Index = new ElementAccess(index);
				Expression p2 = generatePortAccess(farEndOwner.getType());
				Expression p2Index = new IntegralLiteral(far.getIndex());
				subClause.add(
						UMLRTRuntime.UMLRTFrameService.connectFarEnds(p1, p1Index, p2, p2Index));
				subClause.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnboundFarEnd(p1, p1Index, BooleanLiteral.TRUE()));
				subClause.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnboundFarEnd(p2, p2Index, BooleanLiteral.TRUE()));
			} else {
				subClause.add(UMLRTRuntime.UMLRTFrameService.connectRelayPort(
						generatePortAccess(port.getType()),
						new ElementAccess(index),
						generatePortAccess(UMLRTRuntime.UMLRTCapsule.slot(),
								far.getContainer(),
								farEndOwner.getType()),
						new IntegralLiteral(far.getIndex())));
				subClause.add(UMLRTRuntime.UMLRTFrameService.bindSubcapsulePort(
						BooleanLiteral.TRUE(), // isBorder
						new MemberAccess(
								generateSlotAccess(UMLRTRuntime.UMLRTCapsule.slot(), far.getContainer()),
								UMLRTRuntime.UMLRTSlot.capsule),
						new ElementAccess(portId),
						new ElementAccess(index)));
			}
			portInstanceSwitch.add(subClause);
		}
		return clause;
	}

	/**
	 * Generate the "unbindPort" function.
	 * 
	 * @param cls
	 *            - The {@link CppClass} where the function will be added.
	 * @param instance
	 *            - The {@link CapsuleInstance}.
	 * @return {@code true} iff successful.
	 */
	protected boolean generateUnbindPortFunction(CppClass cls, CapsuleInstance instance) {
		MemberFunction func = new MemberFunction(PrimitiveType.VOID, "unbindPort");
		func.setVirtual();
		cls.addMember(CppClass.Visibility.PUBLIC, func);

		Parameter isBorder = new Parameter(PrimitiveType.BOOL, "isBorder");
		Parameter portId = new Parameter(PrimitiveType.INT, "portId");
		Parameter index = new Parameter(PrimitiveType.INT, "index");
		func.add(isBorder);
		func.add(portId);
		func.add(index);

		ConditionalStatement isBorderCond = new ConditionalStatement();

		CodeBlock isBorderBlock = isBorderCond.add(new ElementAccess(isBorder));
		SwitchStatement isBorderSwitch = new SwitchStatement(new ElementAccess(portId));
		isBorderBlock.add(isBorderSwitch);

		CodeBlock isNotBorderBlock = null;

		boolean hasContent = false;

		for (IPortInstance port : instance.getPorts()) {
			if (!XTUMLRTUtil.isWired(port.getType())) {
				continue;
			}

			if (XTUMLRTUtil.isNonBorderPort(port.getType())) {
				hasContent = true;

				if (isNotBorderBlock == null) {
					isNotBorderBlock = isBorderCond.defaultBlock();
				}

				isNotBorderBlock.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnbound(
								PortKind.Internal.createArrayVarAccess(),
								PortKind.Internal.generatePortIdAccess(cpp, capsule, port.getType()),
								new ElementAccess(index),
								BooleanLiteral.FALSE()));
				isNotBorderBlock.add(
						UMLRTRuntime.UMLRTFrameService.disconnectPort(
								generatePortAccess(port.getType()),
								new ElementAccess(index)));
			} else if (XTUMLRTUtil.isRelayPort(port.getType())) {
				SwitchClause clause = generateUnbindRelayPortClause(port, portId, index);
				if (clause != null) {
					hasContent = true;
					isBorderSwitch.add(clause);
				}
			} else {
				SwitchClause clause = new SwitchClause(cpp.getEnumeratorAccess(CppCodePattern.Output.BorderPortId, port.getType(), capsule));
				clause.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnbound(
								PortKind.Border.createArrayVarAccess(),
								PortKind.Border.generatePortIdAccess(cpp, capsule, port.getType()),
								new ElementAccess(index),
								BooleanLiteral.FALSE()));
				clause.add(
						UMLRTRuntime.UMLRTFrameService.disconnectPort(
								generatePortAccess(port.getType()),
								new ElementAccess(index)));

				hasContent = true;
				isBorderSwitch.add(clause);
			}
		}

		if (hasContent) {
			func.add(isBorderCond);
		}

		return true;
	}

	/**
	 * Generated the switch clause for unbinding a border relay port in the 'unbindPort' function.
	 * 
	 * @param port
	 *            - The {@link IPortInstance}.
	 * @param portId
	 *            - The port id {@link Parameter} of the 'bindPort' function.
	 * @param index
	 *            - The port index {@link Parameter} of the 'bindPort' function.
	 * @return The {@link SwitchClause}.
	 */
	protected SwitchClause generateUnbindRelayPortClause(IPortInstance port, Parameter portId, Parameter index) {
		Iterable<? extends IFarEnd> farEnds = port.getFarEnds();

		if (farEnds == null || farEnds.iterator() == null || !farEnds.iterator().hasNext()) {
			return null;
		}

		SwitchClause clause;
		SwitchClause subClause;
		SwitchStatement portInstanceSwitch = new SwitchStatement(new ElementAccess(index));
		IPortInstance farEndOwner;

		clause = new SwitchClause(cpp.getEnumeratorAccess(CppCodePattern.Output.BorderPortId, port.getType(), capsule));
		clause.add(portInstanceSwitch);

		int farEndIndex = 0;
		for (IPortInstance.IFarEnd far : port.getFarEnds()) {
			farEndOwner = far.getOwner();
			subClause = new SwitchClause(new IntegralLiteral(farEndIndex++));

			if (isRelayBorderPortInstance(farEndOwner)) {
				Expression p = generatePortAccess(port.getType());
				Expression pIndex = new ElementAccess(index);
				subClause.add(
						UMLRTRuntime.UMLRTFrameService.sendBoundUnboundForPortIndex(p, pIndex, BooleanLiteral.FALSE()));
				subClause.add(
						UMLRTRuntime.UMLRTFrameService.disconnectPort(p, pIndex));
			} else if (!far.getOwner().isRelay()) {
				subClause.add(UMLRTRuntime.UMLRTFrameService.unbindSubcapsulePort(
						BooleanLiteral.TRUE(), // isBorder
						new MemberAccess(
								generateSlotAccess(UMLRTRuntime.UMLRTCapsule.slot(), far.getContainer()),
								UMLRTRuntime.UMLRTSlot.capsule),
						new ElementAccess(portId),
						new ElementAccess(index)));
			}
			portInstanceSwitch.add(subClause);
		}
		return clause;
	}

	/**
	 * Generated the "bindPort" and "unbindPort" functions for the {@link #capsule}.
	 * 
	 * @param cls
	 *            - The generated {@link CppClass} where the functions will be added.
	 * @param instance
	 *            - The {@link CapsuleInstance}.
	 * @return {@code true} iff successful.
	 */
	protected boolean generateRTSFunctions(CppClass cls, CapsuleInstance instance) {
		return generateBindPortFunction(cls, instance)
				&& generateUnbindPortFunction(cls, instance);
	}

	/**
	 * Generate the UMLRTCapsuleClass information for the {@link Capsule}.
	 * 
	 * @param subCapsules
	 *            - The "roles" array {@link Variable} of type UMLRTCapsuleRole that contains the information of the capsule's parts.
	 * @param instance
	 *            - The {@link CapsuleInstance}.
	 * @return The {@link Variable} of type UMLRTCapsuleClass to export.
	 */
	protected Variable generateUMLRTCapsuleClass(Variable subCapsules, CapsuleInstance instance) {
		CppEnumOrderedInitializer border_init = null;
		CppEnumOrderedInitializer internal_init = null;
		for (Port port : XTUMLRTExtensions.getAllRTPorts(capsule)) {
			boolean isSystemPort = RTSModelLibraryUtils.isSystemElement(port.getType());
			String regOverride = XTUMLRTUtil.getRegistrationOverride(port);

			// TODO some fields need values
			BlockInitializer init = new BlockInitializer(UMLRTRuntime.UMLRTCommsPortRole.getType());
			init.addExpression(cpp.getEnumeratorAccess(CppCodePattern.Output.PortId, port, capsule));
			init.addExpression(new StringLiteral(port.getType().getName()));
			init.addExpression(new StringLiteral(port.getName()));
			init.addExpression(regOverride == null ? StandardLibrary.NULL() : new StringLiteral(regOverride));
			init.addExpression(isSystemPort ? new IntegralLiteral(0) : GeneratorUtils.generateBoundExpression(port));
			init.addExpression(BooleanLiteral.from(XTUMLRTUtil.isAutomatic(port) && !isSystemPort));
			init.addExpression(BooleanLiteral.from(port.isConjugate()));
			init.addExpression(BooleanLiteral.from(XTUMLRTUtil.isAutomaticLocked(port)));
			init.addExpression(BooleanLiteral.from(XTUMLRTUtil.isNotification(port)));

			// unsigned sap : 1; // True if the port is an SAP.
			init.addExpression(BooleanLiteral.from(XTUMLRTUtil.isSAP(port)));
			// unsigned spp : 1; // True if the port is an SPP.
			init.addExpression(BooleanLiteral.from(XTUMLRTUtil.isSPP(port)));

			init.addExpression(BooleanLiteral.from(XTUMLRTUtil.isWired(port) && !isSystemPort));

			if (!XTUMLRTUtil.isNonBorderPort(port)) {
				if (border_init == null) {
					border_init = new CppEnumOrderedInitializer(
							cpp.getIdEnum(CppCodePattern.Output.BorderPortId, capsule),
							UMLRTRuntime.UMLRTCommsPortRole.getType().arrayOf(null));
				}

				border_init.putExpression(
						cpp.getEnumerator(CppCodePattern.Output.BorderPortId, port, capsule),
						init);
			} else {
				if (internal_init == null) {
					internal_init = new CppEnumOrderedInitializer(
							cpp.getIdEnum(CppCodePattern.Output.InternalPortId, capsule),
							UMLRTRuntime.UMLRTCommsPortRole.getType().arrayOf(null));
				}

				internal_init.putExpression(
						cpp.getEnumerator(CppCodePattern.Output.InternalPortId, port, capsule),
						init);
			}
		}

		Variable portroles_border = null;
		if (border_init != null
				&& border_init.getNumInitializers() > 0) {
			portroles_border = new Variable(
					LinkageSpec.STATIC,
					UMLRTRuntime.UMLRTCommsPortRole.getType().const_().arrayOf(null),
					"portroles_border",
					border_init);
			cpp.getElementList(CppCodePattern.Output.UMLRTCapsuleClass, capsule).addElement(portroles_border);
		}

		Variable portroles_internal = null;
		if (internal_init != null
				&& internal_init.getNumInitializers() > 0) {
			portroles_internal = new Variable(
					LinkageSpec.STATIC,
					UMLRTRuntime.UMLRTCommsPortRole.getType().const_().arrayOf(null),
					"portroles_internal",
					internal_init);
			cpp.getElementList(CppCodePattern.Output.UMLRTCapsuleClass, capsule).addElement(portroles_internal);
		}

		RedefinableElement element = capsule.getRedefines();
		Capsule baseClass = null;
		if (element != null) {
			baseClass = (Capsule) element;
		}
		BlockInitializer init = new BlockInitializer(
				UMLRTRuntime.UMLRTCapsuleClass.getType(),
				new StringLiteral(capsule.getName()),
				baseClass == null
						? StandardLibrary.NULL()
						: new AddressOfExpr(new ElementAccess(cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, baseClass))),
				new ElementAccess(generateInstantiate(instance)),
				new IntegralLiteral(subCapsules == null ? 0 : subCapsules.getNumInitializedInstances()),
				subCapsules == null ? StandardLibrary.NULL() : new ElementAccess(subCapsules),
				new IntegralLiteral(border_init == null ? 0 : border_init.getNumInitializers()),
				portroles_border == null ? StandardLibrary.NULL() : new ElementAccess(portroles_border),
				new IntegralLiteral(internal_init == null ? 0 : internal_init.getNumInitializers()),
				portroles_internal == null ? StandardLibrary.NULL() : new ElementAccess(portroles_internal));

		Variable var = cpp.getVariable(CppCodePattern.Output.UMLRTCapsuleClass, capsule);
		cpp.getElementList(CppCodePattern.Output.UMLRTCapsuleClass, capsule).addElement(var);
		var.setInitializer(init);
		return var;
	}

	/**
	 * @param portInstance
	 *            - A {@link IPortInstance}.
	 * @return {@code true} iff the port instance's type (a {@link Port}) is a relay port, a top-level
	 *         port of this {@link #capsule}.
	 */
	protected boolean isRelayBorderPortInstance(IPortInstance portInstance) {
		return XTUMLRTUtil.isRelayPort(portInstance.getType())
				&& portInstance.isTopLevelPort()
				&& capsule == (Capsule) portInstance.getType().eContainer()
				&& capsule == portInstance.getContainer().getType();
	}

	// Overriding methods that do not apply for capsules
	@Override
	protected void generateAssignmentOperator(CppClass cls) {
	}

	@Override
	protected void generateCopyConstructor(CppClass cls) {
	}

	@Override
	protected void generateEqualityOperator(CppClass cls) {
	}

	@Override
	protected void generateExtractionOperator(CppClass cls) {
	}

	@Override
	protected void generateInequalityOperator(CppClass cls) {
	}

	@Override
	protected void generateInsertionOperator(CppClass cls) {
	}

}

