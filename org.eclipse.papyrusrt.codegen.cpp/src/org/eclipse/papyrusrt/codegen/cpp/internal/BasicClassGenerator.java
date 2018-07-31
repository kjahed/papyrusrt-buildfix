/*******************************************************************************
 * Copyright (c) 2014-2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.UserEditableRegion;
import org.eclipse.papyrusrt.codegen.cpp.AbstractElementGenerator;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern.Output;
import org.eclipse.papyrusrt.codegen.cpp.TypesUtil;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.AttributeKind;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.AttributeProperties;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.CapsuleProperties;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.ClassKind;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.ClassProperties;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.DependencyKind;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.InitializationKind;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.OperationKind;
import org.eclipse.papyrusrt.codegen.cpp.profile.RTCppProperties.OperationProperties;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppGenerationProperties;
import org.eclipse.papyrusrt.codegen.lang.cpp.Element;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type;
import org.eclipse.papyrusrt.codegen.lang.cpp.dep.Dependency;
import org.eclipse.papyrusrt.codegen.lang.cpp.dep.Dependency.Kind;
import org.eclipse.papyrusrt.codegen.lang.cpp.dep.ElementDependency;
import org.eclipse.papyrusrt.codegen.lang.cpp.dep.TypeDependency;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.AbstractFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.BitField;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Constructor;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass.Visibility;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.DeclarationBlob;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.ElementList;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Function;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.LinkageSpec;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Macro;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberField;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Parameter;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AbstractFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AddressOfExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BinaryOperation;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BinaryOperation.Operator;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BooleanLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.CharacterLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ConstructorCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.DereferenceExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ElementAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ExpressionBlob;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IndexExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.LogicalComparison;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.This;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.UnaryOperation;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.StandardLibrary;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.CodeBlock;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.ConditionalStatement;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.ExpressionStatement;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.ForStatement;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.ReturnStatement;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.UserCode;
import org.eclipse.papyrusrt.xtumlrt.common.AbstractAction;
import org.eclipse.papyrusrt.xtumlrt.common.ActionCode;
import org.eclipse.papyrusrt.xtumlrt.common.Attribute;
import org.eclipse.papyrusrt.xtumlrt.common.CommonPackage;
import org.eclipse.papyrusrt.xtumlrt.common.Entity;
import org.eclipse.papyrusrt.xtumlrt.common.Generalization;
import org.eclipse.papyrusrt.xtumlrt.common.Model;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.common.Operation;
import org.eclipse.papyrusrt.xtumlrt.common.StructuredType;
import org.eclipse.papyrusrt.xtumlrt.common.TypedMultiplicityElement;
import org.eclipse.papyrusrt.xtumlrt.common.ValueSpecification;
import org.eclipse.papyrusrt.xtumlrt.common.VisibilityKind;
import org.eclipse.papyrusrt.xtumlrt.util.QualifiedNames;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTExtensions;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;
import org.eclipse.uml2.uml.util.UMLUtil;

/**
 * Generator for basic class elements ({@link org.eclipse.uml2.uml.Class} in UML and {@link StructuredType})
 * in xtUML-RT.
 * 
 * @author Ernesto Posse
 */
public class BasicClassGenerator extends AbstractElementGenerator {

	/** The model element for which code will be generated. */
	protected final StructuredType element;

	/** A map from each class attribute to the corresponding C++ member field. */
	protected final Map<Attribute, MemberField> fields = new LinkedHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param element
	 *            - The model element (a subtype of {@link StructuredType}.
	 */
	public BasicClassGenerator(CppCodePattern cpp, org.eclipse.papyrusrt.xtumlrt.common.Type element) {
		super(cpp);
		assert element instanceof StructuredType;
		this.element = (StructuredType) element;
	}

	@Override
	protected Output getOutputKind() {
		return Output.BasicClass;
	}

	@Override
	public String getLabel() {
		return super.getLabel() + ' ' + element.getName();
	}

	/**
	 * Converts visibility kinds from xtUML-RT to C++.
	 * 
	 * @param visibility
	 *            - A {@link VisibilityKind}
	 * @return The corresponding {@link CppClass.Visibility}
	 */
	protected CppClass.Visibility getVisibility(VisibilityKind visibility) {
		switch (visibility) {
		case PUBLIC:
			return CppClass.Visibility.PUBLIC;
		case PROTECTED:
			return CppClass.Visibility.PROTECTED;
		case PRIVATE:
			return CppClass.Visibility.PRIVATE;
		default:
			break;
		}

		return null;
	}

	/**
	 * Returns the C++ type created from the return type of the operation.
	 * The operation's returnType reference is a TypedMultiplicityElement which
	 * itself has a type. This is the type used to obtain the corresponding
	 * C++ type.
	 * 
	 * @param operation
	 *            - An {@link Operation}.
	 * @return The return C++ {@link Type}.
	 */
	protected Type getReturnType(Operation operation) {
		TypedMultiplicityElement returnTypedElement = operation.getReturnType();
		if (returnTypedElement == null) {
			return PrimitiveType.VOID;
		}

		org.eclipse.papyrusrt.xtumlrt.common.Type returnType = returnTypedElement.getType();
		if (returnType == null) {
			return PrimitiveType.VOID;
		}

		Type type = TypesUtil.createCppType(cpp, operation, returnType);
		return type;
	}

	@Override
	public boolean generate() {
		if (!(element instanceof StructuredType)) {
			throw new RuntimeException("Element '" + QualifiedNames.cachedFullName(element) + "' is not a StructuredType (Class or Capsule)");
		}
		CppClass cls = cpp.getWritableCppClass(CppCodePattern.Output.BasicClass, element);
		generateBases(cls);
		return generate(cls);
	}

	/**
	 * Generate the receiver model object's values into the given C++ class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @return {@code true} if successful.
	 */
	protected boolean generate(CppClass cls) {
		generateAttributes(cls);
		generateOperations(cls);
		generateExtraDeclarations(cls);
		generateExtraDependencies();

		processRTGenerationProperties(cls);

		return true;
	}

	/**
	 * Gets the generation properties from the {@link ClassProperities} or {@link CapsuleProperties}
	 * stereotype of the RTCppProperties set and invokes the relevant generation methods enabled by those
	 * properties.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void processRTGenerationProperties(CppClass cls) {
		ClassKind structKind = RTCppGenerationProperties.getPassiveClassPropKind(element);
		if (structKind != null) {
			switch (structKind) {
			case CLASS:
				cls.setKind(CppClass.Kind.CLASS);
				break;
			case STRUCT:
				cls.setKind(CppClass.Kind.STRUCT);
				break;
			case UNION:
				cls.setKind(CppClass.Kind.UNION);
				break;
			case TYPEDEF:
				CodeGenPlugin.warning("'" + QualifiedNames.cachedFullName(element) + "' has 'typedef' declaration type, but this is not supported yet.");
				break;
			default:
				break;
			}
		} else if (element.eClass() == CommonPackage.eINSTANCE.getStructuredType()) {
			cls.setKind(CppClass.Kind.STRUCT);
		}

		Boolean genAssignmentOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateAssignmentOperator(element);
		Boolean genCopyConstructorBool = RTCppGenerationProperties.getClassGenerationPropGenerateCopyConstructor(element);
		Boolean genDefaultConstructorBool = RTCppGenerationProperties.getClassGenerationPropGenerateDefaultConstructor(element);
		Boolean genDestructorBool = RTCppGenerationProperties.getClassGenerationPropGenerateDestructor(element);
		Boolean genEqualityOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateEqualityOperator(element);
		Boolean genInequalityOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateInequalityOperator(element);
		Boolean genExtractionOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateExtractionOperator(element);
		Boolean genInsertionOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateInsertionOperator(element);
		Boolean genStateMachineBool = RTCppGenerationProperties.getClassGenerationPropGenerateStateMachine(element);

		if (genDefaultConstructorBool != null && genDefaultConstructorBool.booleanValue()) {
			generateDefaultConstructor(cls);
		}
		if (genCopyConstructorBool != null && genCopyConstructorBool.booleanValue()) {
			generateCopyConstructor(cls);
		}
		if (genDestructorBool != null && genDestructorBool.booleanValue()) {
			generateDestructor(cls);
		}
		if (genAssignmentOperatorBool != null && genAssignmentOperatorBool.booleanValue()) {
			generateAssignmentOperator(cls);
		}
		if (genEqualityOperatorBool != null && genEqualityOperatorBool.booleanValue()) {
			generateEqualityOperator(cls);
		}
		if (genExtractionOperatorBool != null && genExtractionOperatorBool.booleanValue()) {
			generateExtractionOperator(cls);
		}
		if (genInequalityOperatorBool != null && genInequalityOperatorBool.booleanValue()) {
			generateInequalityOperator(cls);
		}
		if (genInsertionOperatorBool != null && genInsertionOperatorBool.booleanValue()) {
			generateInsertionOperator(cls);
		}
		if (genStateMachineBool != null && genStateMachineBool.booleanValue()) {
			generateStateMachine(cls);
		}
	}

	/**
	 * Generate code for all operations in the class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @return {@code true} if successful.
	 */
	protected boolean generateOperations(CppClass cls) {
		for (Operation operation : element.getOperations()) {
			generate(cls, operation);
		}
		return true;
	}

	/**
	 * Generate code for the given operation.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param operation
	 *            - The {@link Operation}.
	 * @return The resulting function. An instance of a subclass of {@link AbstractFunction}.
	 *         It could be a {@link MemberFunction}, or a global {@link Function}, depending on the
	 *         {@link OperationProperties#getKind()} attribute.
	 */
	protected AbstractFunction generate(CppClass cls, Operation operation) {
		CppClass.Visibility cppVisibility = getVisibility(operation.getVisibility());
		if (cppVisibility == null) {
			return null;
		}

		Type returnType = getReturnType(operation);
		if (returnType == null) {
			throw new RuntimeException("could not determine return type for " + operation.toString());
		}

		AbstractFunction function = null;

		OperationKind opKind = RTCppGenerationProperties.getOperationPropKind(operation);
		if (opKind == null) {
			function = generateMemberFunction(cls, operation, cppVisibility, returnType);
		} else {
			// the operation has an OperationProperties stereotype
			switch (opKind) {
			case MEMBER:
				function = generateMemberFunction(cls, operation, cppVisibility, returnType);
				break;
			case FRIEND:
				function = generateFriendFunction(cls, operation, returnType);
				break;
			case GLOBAL:
				function = generateGlobalFunction(operation, returnType);
				break;
			default:
				throw new RuntimeException("Operation '" + QualifiedNames.cachedFullName(operation) + "' has an unrecognized kind: '" + opKind.toString() + "'");
			}
		}

		Boolean genDefBool = RTCppGenerationProperties.getOperationPropGenDef(operation);
		if (genDefBool != null && !genDefBool.booleanValue()) {
			function.setOnlyDecl();
		}

		return function;
	}

	/**
	 * Generate a member function for a given model operation.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param operation
	 *            - The model {@link Operation} element.
	 * @param cppVisibility
	 *            - The {@link CppClass.Visibility}.
	 * @param returnType
	 *            - The operation's return {@link Type}.
	 * @return The {@link MemberFunction}.
	 */
	protected MemberFunction generateMemberFunction(CppClass cls, Operation operation, CppClass.Visibility cppVisibility, org.eclipse.papyrusrt.codegen.lang.cpp.Type returnType) {
		MemberFunction function = new MemberFunction(returnType, operation.getName());
		generateParameters(operation, function);
		generateBody(operation, function);

		Boolean isInlineBool = RTCppGenerationProperties.getOperationPropInline(operation);
		Boolean isVirtualBool = RTCppGenerationProperties.getOperationPropPolymorphic(operation);
		boolean isInline = isInlineBool != null && isInlineBool.booleanValue();
		boolean isVirtual = isVirtualBool != null && isVirtualBool.booleanValue();

		if (isInline) {
			function.setInline();
		}
		if (operation.isStatic()) {
			cls.addStaticMember(cppVisibility, function);
		} else {
			if (isVirtual) {
				function.setVirtual();
			}
			cls.addMember(cppVisibility, function);
		}
		if (operation.isQuery()) {
			function.setQuery();
		}

		return function;
	}

	/**
	 * Generate a friend function.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param operation
	 *            - The model {@link Operation} element.
	 * @param returnType
	 *            - The operation's return {@link Type}.
	 * @return The top-level {@link Function}.
	 */
	protected Function generateFriendFunction(CppClass cls, Operation operation, Type returnType) {
		Function function = generateTopLevelFunction(operation, returnType);
		cls.addFriendFunction(function);
		return function;
	}

	/**
	 * Generate a global function.
	 * 
	 * @param operation
	 *            - The model {@link Operation} element.
	 * @param returnType
	 *            - The operation's return {@link Type}.
	 * @return The top-level {@link Function}.
	 */
	protected Function generateGlobalFunction(Operation operation, Type returnType) {
		Function function = generateTopLevelFunction(operation, returnType);
		ElementList elementList = cpp.getElementList(getOutputKind(), element);
		if (elementList == null) {
			return function;
		}
		elementList.addElement(function);
		return function;
	}

	/**
	 * Generate a top-level function.
	 * 
	 * @param operation
	 *            - The model {@link Operation} element.
	 * @param returnType
	 *            - The operation's return {@link Type}.
	 * @return The top-level {@link Function}.
	 */
	protected Function generateTopLevelFunction(Operation operation, Type returnType) {
		LinkageSpec linkageSpec = operation.isStatic() ? LinkageSpec.STATIC : LinkageSpec.EXTERN;
		Function function = new Function(linkageSpec, returnType, operation.getName());
		generateParameters(operation, function);
		generateBody(operation, function);

		Boolean isInlineBool = RTCppGenerationProperties.getOperationPropInline(operation);
		boolean isInline = isInlineBool != null && isInlineBool.booleanValue();

		if (isInline) {
			function.setInline();
		}

		return function;
	}

	/**
	 * Generate parameters of a C++ function from the model operation parameters.
	 * 
	 * @param operation
	 *            - The {@link Operation}.
	 * @param function
	 *            - The {@link AbstractFunction} to which parameters will be added.
	 */
	protected void generateParameters(Operation operation, AbstractFunction function) {
		for (org.eclipse.papyrusrt.xtumlrt.common.Parameter param : operation.getParameters()) {
			switch (param.getDirection()) {
			case OUT:
				break;
			case IN:
				Type type = TypesUtil.createCppType(cpp, param, param.getType());
				function.add(new Parameter(type, param.getName()));
				break;
			default:
				throw new RuntimeException("unhandled paramater direction for " + param.toString());
			}
		}
	}

	/**
	 * Generate the body of an operation.
	 * 
	 * @param operation
	 *            - The {@link Operation}.
	 * @param function
	 *            - The {@link AbstractFunction} to which the body will be added.
	 */
	protected void generateBody(Operation operation, AbstractFunction function) {
		// Find some C++ code that is associated with this Operation
		AbstractAction act = operation.getBody();
		ActionCode code = act instanceof ActionCode ? (ActionCode) act : null;
		if (code != null) {
			List<String> qNames = new ArrayList<>();
			EObject container = operation.eContainer();
			while (container != null) {
				if (container instanceof Entity || container instanceof Package || container instanceof Model) {
					qNames.add(0, ((NamedElement) container).getName());
				}
				container = container.eContainer();
			}
			UserEditableRegion.Label label = new UserEditableRegion.Label();
			label.setQualifiedName(String.join("::", qNames));
			label.setType(operation.eClass().getName().toLowerCase());
			label.setDetails(operation.getName());
			org.eclipse.uml2.uml.Element srcElement = cpp.getTranslator().getSource(operation);
			if (srcElement != null) {
				label.setUri(srcElement.eResource().getURI().toString());
			} else {
				label.setUri(operation.eResource().getURI().toString());
			}
			StringBuilder result = new StringBuilder();
			result.append(UserEditableRegion.COMMENT_START_STRING + UserEditableRegion.userEditBegin(label) + UserEditableRegion.COMMENT_END_STRING).append(System.lineSeparator());

			String source = code.getSource();
			if (!UMLUtil.isEmpty(source)) {
				result.append(source);
			}
			result.append(UserEditableRegion.COMMENT_START_STRING + UserEditableRegion.userEditEnd() + UserEditableRegion.COMMENT_END_STRING).append(System.lineSeparator());

			function.add(new UserCode(result.toString()));
		}
	}


	/**
	 * Generate the attributes of the class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @return {@code true} if successful.
	 */
	protected boolean generateAttributes(CppClass cls) {
		for (Attribute attr : XTUMLRTExtensions.getAllAttributes(element)) {
			generate(cls, attr);
		}
		return true;
	}

	/**
	 * Generate code for a given attribute.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param attr
	 *            - The {@link Attribute}.
	 * @return The C++ {@link MemberField}.
	 */
	protected MemberField generate(CppClass cls, Attribute attr) {
		CppClass.Visibility cppVisibility = getVisibility(attr.getVisibility());
		if (cppVisibility == null) {
			return null;
		}

		Type type = TypesUtil.createCppType(cpp, attr, attr.getType());
		MemberField field = null;

		AttributeKind attrKind = RTCppGenerationProperties.getAttributePropKind(attr);
		if (attrKind == null) {
			field = generateMemberAttribute(cls, attr, cppVisibility, type, null);
		} else {
			// the attribute has an AttributeProperties stereotype
			switch (attrKind) {
			case MEMBER:
			case MUTABLE_MEMBER:
				field = generateMemberAttribute(cls, attr, cppVisibility, type, attrKind);
				break;
			case GLOBAL:
				generateGlobalVariable(cls, attr, type);
				break;
			case DEFINE:
				generateMacroDefinition(cls, attr);
				break;
			default:
				throw new RuntimeException("Attribute '" + QualifiedNames.cachedFullName(attr) + "' has an unrecognized kind: '" + attrKind.toString() + "'");
			}
		}
		return field;
	}

	/**
	 * Generate a macro definition for an attribute that has been marked as a macro in the
	 * {@link AttributeProperties} stereotype.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param attr
	 *            - The {@link Attribute}.
	 */
	protected void generateMacroDefinition(CppClass cls, Attribute attr) {
		ValueSpecification defVal = attr.getDefault();
		if (defVal != null) {
			String replacement = XTUMLRTUtil.getStringValue(defVal);
			if (replacement != null && !replacement.isEmpty()) {
				ElementList elementList = cpp.getElementList(getOutputKind(), element);
				if (elementList == null) {
					return;
				}
				Macro macro = new Macro(attr.getName(), new ExpressionBlob(replacement));
				elementList.addElement(macro);
			}
		} else {
			CodeGenPlugin.warning("Default value for macro attribute '" + QualifiedNames.cachedFullName(attr) + "' is null");
		}
	}

	/**
	 * Generate a global variable.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param attr
	 *            - The {@link Attribute}.
	 * @param type
	 *            - The C++ {@link Type}.
	 */
	protected void generateGlobalVariable(CppClass cls, Attribute attr, Type type) {
		ElementList elementList = cpp.getElementList(getOutputKind(), element);
		if (elementList == null) {
			return;
		}

		LinkageSpec linkageSpec = attr.isStatic() ? LinkageSpec.STATIC : LinkageSpec.UNSPECIFIED;
		Variable var = new Variable(linkageSpec, type, attr.getName());

		elementList.addElement(var);
	}

	/**
	 * Generate a member field for a given attribute.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param attr
	 *            - The {@link Attribute}.
	 * @param cppVisibility
	 *            - The {@link CppClass.Visibility}
	 * @param type
	 *            - The C++ {@link Type}.
	 * @param attrKind
	 *            - The {@link AttributeKind}
	 * @return The C++ {@link MemberField}.
	 */
	protected MemberField generateMemberAttribute(CppClass cls, Attribute attr, CppClass.Visibility cppVisibility, Type type, AttributeKind attrKind) {
		MemberField field = null;
		Expression initExpr = null;
		String sizeStr = RTCppGenerationProperties.getAttributePropSize(attr);
		ValueSpecification defVal = attr.getDefault();
		if (sizeStr != null && !sizeStr.trim().isEmpty()) {
			field = new BitField(type, attr.getName(), new ExpressionBlob(sizeStr));
		} else if (defVal == null || XTUMLRTUtil.getStringValue(defVal).isEmpty()) {
			field = new MemberField(type, attr.getName());
		} else {
			String strVal = XTUMLRTUtil.getStringValue(defVal);
			initExpr = new ExpressionBlob(strVal);
			field = new MemberField(type, attr.getName(), initExpr);
		}

		generateAttributeInitialization(cls, attr, field, initExpr);

		if (attr.isStatic()) {
			cls.addStaticMember(cppVisibility, field);
		} else {
			if (!attr.isReadOnly() && attrKind == AttributeKind.MUTABLE_MEMBER) {
				field.setMutable();
			}
			cls.addMember(cppVisibility, field);
		}

		fields.put(attr, field);

		return field;
	}

	/**
	 * Generate the list of base classes for the given class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateBases(CppClass cls) {
		StructuredType type = element;
		Iterable<Generalization> generalizations = type.getGeneralizations();
		if (generalizations != null) {
			for (Generalization generalization : generalizations) {
				StructuredType superType = generalization.getSuper();
				if (superType == null) {
					continue;
				}
				CppClass base = cpp.getWritableCppClass(CppCodePattern.Output.BasicClass, superType);
				Boolean isVirtualBool = RTCppGenerationProperties.getGeneralizationPropVirtual(generalization);
				boolean isVirtual = isVirtualBool != null && isVirtualBool.booleanValue();
				if (isVirtual) {
					cls.addVirtualBase(CppClass.Access.PUBLIC, base);
				} else {
					cls.addBase(CppClass.Access.PUBLIC, base);
				}
			}
		}
	}

	/**
	 * Generate the list of dependencies for the class.
	 */
	protected void generateExtraDependencies() {
		ElementList elementList = cpp.getElementList(getOutputKind(), element);
		Iterable<org.eclipse.papyrusrt.xtumlrt.common.Dependency> dependencies = ((NamedElement) element).getDependencies();
		if (dependencies != null) {
			for (org.eclipse.papyrusrt.xtumlrt.common.Dependency dependency : dependencies) {
				DependencyKind depKindInHead = RTCppGenerationProperties.getDependencyPropKindInHeader(dependency);
				DependencyKind depKindInImpl = RTCppGenerationProperties.getDependencyPropKindInImplementation(dependency);
				NamedElement supplier = dependency.getSupplier();
				if (supplier != null) {
					Dependency cppDependencyInHead = null;
					Dependency cppDependencyInImpl = null;
					Element cppElement = cpp.getCppElement(supplier);
					if (cppElement != null) {
						if (depKindInHead == null && depKindInImpl == null) {
							depKindInHead = DependencyKind.INCLUSION;
						}
						cppDependencyInHead = getDependency(cppElement, supplier, depKindInHead);
						cppDependencyInImpl = getDependency(cppElement, supplier, depKindInImpl);

						if (cppDependencyInHead != null) {
							elementList.addDeclDependency(cppDependencyInHead);
						}
						if (cppDependencyInImpl != null) {
							elementList.addDefnDependency(cppDependencyInImpl);
						}
					}
				}
			}
		}
	}

	/**
	 * Get a C++ dependency to a given supplier element.
	 * 
	 * @param cppElement
	 *            - The C++ {@link Element} to depend on (represents the supplier at the C++ level).
	 * @param supplier
	 *            - The xtUML-RT {@link NamedElement} (the supplier at the model level).
	 * @param kind
	 *            - The {@link Kind} of dependency.
	 * @return The C++ {@link Dependency}.
	 */
	protected Dependency getDependency(Element cppElement, NamedElement supplier, DependencyKind kind) {
		if (kind == null) {
			return null;
		}
		Dependency cppDependency = null;
		if (supplier instanceof org.eclipse.papyrusrt.xtumlrt.common.Type) {
			cppDependency = new TypeDependency(cppElement.getType());
		} else {
			cppDependency = new ElementDependency(cppElement);
		}
		// We use the (user-provided) 'kind' to override the kind given by the Dependency constructor
		switch (kind) {
		case FORWARD_REFERENCE:
			cppDependency.setKind(Dependency.Kind.Reference);
			break;
		case INCLUSION:
			cppDependency.setKind(Dependency.Kind.Use);
			break;
		case NONE:
		default:
			cppDependency.setKind(Dependency.Kind.None);
			break;
		}
		return cppDependency;
	}

	/**
	 * Generate additional class declarations, if provided by the {@link ClassProperties} stereotype.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateExtraDeclarations(CppClass cls) {
		String privDecl = RTCppGenerationProperties.getClassPropPrivateDeclarations(element);
		String protDecl = RTCppGenerationProperties.getClassPropProtectedDeclarations(element);
		String publDecl = RTCppGenerationProperties.getClassPropPublicDeclarations(element);

		if (privDecl != null && !privDecl.trim().isEmpty()) {
			cls.addDeclarationBlob(Visibility.PRIVATE, new DeclarationBlob(privDecl));
		}
		if (protDecl != null && !protDecl.trim().isEmpty()) {
			cls.addDeclarationBlob(Visibility.PROTECTED, new DeclarationBlob(protDecl));
		}
		if (publDecl != null && !publDecl.trim().isEmpty()) {
			cls.addDeclarationBlob(Visibility.PUBLIC, new DeclarationBlob(publDecl));
		}
	}

	/**
	 * Generate the assignment operator for the class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateAssignmentOperator(CppClass cls) {
		MemberFunction func = new MemberFunction(cls.getType().ref(), AbstractFunction.OverloadedOperator.ASSIGNMENT);
		Parameter param = new Parameter(cls.getType().const_().ref(), "other");
		func.add(param);

		// Make self identity check
		Expression condition = new LogicalComparison(
				new AddressOfExpr(new ElementAccess(param)),
				LogicalComparison.Operator.EQUIVALENT,
				new This(cls));
		ConditionalStatement cond = new ConditionalStatement();
		CodeBlock codeBlock = cond.add(condition);
		codeBlock.add(new ReturnStatement(new DereferenceExpr(new This(cls))));
		func.add(cond);

		// call assignment operators for base classes
		StructuredType type = element;
		Iterable<Generalization> generalizations = type.getGeneralizations();
		if (generalizations != null) {
			for (Generalization generalization : generalizations) {
				StructuredType superType = generalization.getSuper();
				if (superType == null) {
					continue;
				}
				CppClass base = cpp.getWritableCppClass(CppCodePattern.Output.BasicClass, superType);
				// call base assignment operator
				MemberFunction baseFunc = new MemberFunction(base.getType().ref(), AbstractFunction.OverloadedOperator.ASSIGNMENT);
				MemberFunctionCall call = new MemberFunctionCall(base, baseFunc);
				call.addArgument(new ElementAccess(param));
				func.add(call);
			}
		}

		// Add attribute assignments
		for (Map.Entry<Attribute, MemberField> entry : fields.entrySet()) {
			MemberField field = entry.getValue();

			Attribute a = entry.getKey();
			Expression bound = GeneratorUtils.generateBoundExpression(a);

			if (a.isReadOnly()) {
				continue;
			}
			if (field.getType().isArray()) {
				Variable i = new Variable(PrimitiveType.INT, "i", new IntegralLiteral(0));
				Expression c = new LogicalComparison(
						new ElementAccess(i),
						LogicalComparison.Operator.LESS_THAN,
						bound);
				ForStatement forStmt = new ForStatement(
						i,
						c,
						new UnaryOperation(
								UnaryOperation.Operator.PRE_INCREMENT,
								new ElementAccess(i)));

				// add the body of for loop
				forStmt.add(new ExpressionStatement(
						new BinaryOperation(
								new IndexExpr((new MemberAccess(cls, field)), new ExpressionBlob("i")),
								BinaryOperation.Operator.ASSIGN,
								new IndexExpr(new MemberAccess(new ElementAccess(param), field), new ExpressionBlob("i")))));

				func.add(forStmt);

			} else {
				func.add(
						new BinaryOperation(
								new MemberAccess(cls, field),
								BinaryOperation.Operator.ASSIGN,
								new MemberAccess(new ElementAccess(param), field)));

			}
		}

		// Add return
		func.add(new ReturnStatement(new DereferenceExpr(new This(cls))));

		cls.addMember(CppClass.Visibility.PUBLIC, func);
	}

	/**
	 * Generate the class' copy constructor.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateCopyConstructor(CppClass cls) {
		Constructor copyCtor = cpp.getCopyConstructor(getOutputKind(), element);
		Parameter param = copyCtor.setCopyConstructor(cls);

		// call copy constructors for base classes
		StructuredType type = element;
		Iterable<Generalization> generalizations = type.getGeneralizations();
		if (generalizations != null) {
			for (Generalization generalization : generalizations) {
				StructuredType superType = generalization.getSuper();
				if (superType == null) {
					continue;
				}

				Constructor superCtor = null;
				superCtor = new Constructor();
				CppClass base = cpp.getWritableCppClass(CppCodePattern.Output.BasicClass, superType);
				superCtor.setCopyConstructor(base);

				AbstractFunctionCall copyBaseCtorCall = null;
				copyBaseCtorCall = new ConstructorCall(superCtor);
				copyBaseCtorCall.addArgument(new ElementAccess(param));
				copyCtor.addBaseInitializer(copyBaseCtorCall);
			}
		}

		// Add attribute assignments
		for (Map.Entry<Attribute, MemberField> entry : fields.entrySet()) {
			if (!entry.getKey().isStatic()) {
				MemberField field = entry.getValue();

				Attribute a = entry.getKey();
				Expression bound = GeneratorUtils.generateBoundExpression(a);

				if (field.getType().isArray()) {
					Variable i = new Variable(PrimitiveType.INT, "i", new IntegralLiteral(0));
					Expression c = new LogicalComparison(
							new ElementAccess(i),
							LogicalComparison.Operator.LESS_THAN,
							bound);
					ForStatement forStmt = new ForStatement(
							i,
							c,
							new UnaryOperation(
									UnaryOperation.Operator.PRE_INCREMENT,
									new ElementAccess(i)));

					// add the body of for loop
					forStmt.add(new ExpressionStatement(
							new BinaryOperation(
									new IndexExpr((new MemberAccess(cls, field)), new ExpressionBlob("i")),
									BinaryOperation.Operator.ASSIGN,
									new IndexExpr(new MemberAccess(new ElementAccess(param), field), new ExpressionBlob("i")))));
					copyCtor.add(forStmt);
				} else {
					copyCtor.add(
							new BinaryOperation(
									new MemberAccess(cls, field),
									BinaryOperation.Operator.ASSIGN,
									new MemberAccess(new ElementAccess(param), field)));
				}
			}
		}
	}

	/**
	 * Generate the default constructor.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateDefaultConstructor(CppClass cls) {
		cpp.getConstructor(getOutputKind(), element);
	}

	/**
	 * Generate the class' destructor.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateDestructor(CppClass cls) {
		cpp.getDestructor(getOutputKind(), element);
	}

	/**
	 * Generate the equality operator for the class.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateEqualityOperator(CppClass cls) {
		MemberFunction func = new MemberFunction(PrimitiveType.BOOL, AbstractFunction.OverloadedOperator.EQUALITY);
		Parameter param = new Parameter(cls.getType().const_().ref(), "other");
		func.add(param);

		// call equality operators for base classes
		StructuredType type = element;
		Iterable<Generalization> generalizations = type.getGeneralizations();
		if (generalizations != null) {
			for (Generalization generalization : generalizations) {
				StructuredType superType = generalization.getSuper();
				if (superType == null) {
					continue;
				}

				// Check that equality operator defined in super class, if not continue
				Boolean genEqualityOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateEqualityOperator(superType);
				if (genEqualityOperatorBool == null ||
						(genEqualityOperatorBool != null && !genEqualityOperatorBool.booleanValue())) {
					continue;
				}

				// call base equality operator
				CppClass base = cpp.getWritableCppClass(CppCodePattern.Output.BasicClass, superType);
				MemberFunction baseFunc = new MemberFunction(base.getType().ref(), AbstractFunction.OverloadedOperator.EQUALITY);
				MemberFunctionCall call = new MemberFunctionCall(base, baseFunc);
				call.addArgument(new ElementAccess(param));

				ConditionalStatement attrCond = new ConditionalStatement();
				Expression attrEquals = new UnaryOperation(
						UnaryOperation.Operator.LOGICAL_NOT,
						call);
				CodeBlock action = attrCond.add(attrEquals);
				action.add(new ReturnStatement(BooleanLiteral.FALSE()));
				func.add(attrCond);
			}
		}

		// Check attributes
		for (Map.Entry<Attribute, MemberField> entry : fields.entrySet()) {
			if (!entry.getKey().isStatic()) {
				MemberField field = entry.getValue();
				Attribute a = entry.getKey();
				Expression bound = GeneratorUtils.generateBoundExpression(a);

				if (field.getType().isArray()) {
					Variable i = new Variable(PrimitiveType.INT, "i", new IntegralLiteral(0));
					Expression c = new LogicalComparison(
							new ElementAccess(i),
							LogicalComparison.Operator.LESS_THAN,
							bound);
					ForStatement forStmt = new ForStatement(
							i,
							c,
							new UnaryOperation(
									UnaryOperation.Operator.PRE_INCREMENT,
									new ElementAccess(i)));

					// add the body of for loop
					ConditionalStatement attrCond = new ConditionalStatement();
					Expression attrEquals = new UnaryOperation(
							UnaryOperation.Operator.LOGICAL_NOT,
							new LogicalComparison(
									new IndexExpr((new MemberAccess(cls, field)), new ExpressionBlob("i")),
									LogicalComparison.Operator.EQUIVALENT,
									new IndexExpr(new MemberAccess(new ElementAccess(param), field), new ExpressionBlob("i"))));
					CodeBlock action = attrCond.add(attrEquals);
					action.add(new ReturnStatement(BooleanLiteral.FALSE()));
					forStmt.add(attrCond);
					func.add(forStmt);
				} else {
					ConditionalStatement attrCond = new ConditionalStatement();
					Expression attrEquals = new UnaryOperation(
							UnaryOperation.Operator.LOGICAL_NOT,
							new LogicalComparison(
									new MemberAccess(cls, field),
									LogicalComparison.Operator.EQUIVALENT,
									new MemberAccess(new ElementAccess(param), field)));
					CodeBlock action = attrCond.add(attrEquals);
					action.add(new ReturnStatement(BooleanLiteral.FALSE()));
					func.add(attrCond);
				}
			}
		}

		func.add(new ReturnStatement(BooleanLiteral.TRUE()));
		cls.addMember(CppClass.Visibility.PUBLIC, func);
	}

	/**
	 * Generate the extraction operator for the class. This is the dual of the insertion operator.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateExtractionOperator(CppClass cls) {
		Function func = new Function(LinkageSpec.UNSPECIFIED, StandardLibrary.std_istream.ref(), AbstractFunction.OverloadedOperator.INSERTION);
		Parameter param1 = new Parameter(StandardLibrary.std_istream.ref(), "is");
		Parameter param2 = new Parameter(cls.getType().const_().ref(), "obj");
		func.add(param1);
		func.add(param2);

		for (Map.Entry<Attribute, MemberField> entry : fields.entrySet()) {
			MemberField field = entry.getValue();
			Expression[] fieldExtraction = {
					new ElementAccess(param1),
					new MemberAccess(new ElementAccess(param2), field)
			};
			func.add(BinaryOperation.chain(BinaryOperation.Operator.RIGHT_SHIFT, fieldExtraction));
		}

		func.add(new ReturnStatement(new ElementAccess(param1)));

		cls.addFriendFunction(func);
	}

	/**
	 * Generate the inequality operator.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateInequalityOperator(CppClass cls) {
		MemberFunction func = new MemberFunction(PrimitiveType.BOOL, AbstractFunction.OverloadedOperator.INEQUALITY);
		Parameter param = new Parameter(cls.getType().const_().ref(), "other");
		func.add(param);

		// call equality operators for base classes
		StructuredType type = element;
		Iterable<Generalization> generalizations = type.getGeneralizations();
		if (generalizations != null) {
			for (Generalization generalization : generalizations) {
				StructuredType superType = generalization.getSuper();
				if (superType == null) {
					continue;
				}

				// Check that inequality operator defined in super class, if not continue
				Boolean genInequalityOperatorBool = RTCppGenerationProperties.getClassGenerationPropGenerateInequalityOperator(superType);
				if (genInequalityOperatorBool == null ||
						(genInequalityOperatorBool != null && !genInequalityOperatorBool.booleanValue())) {
					continue;
				}

				// call base inequality operator
				CppClass base = cpp.getWritableCppClass(CppCodePattern.Output.BasicClass, superType);
				MemberFunction baseFunc = new MemberFunction(base.getType().ref(), AbstractFunction.OverloadedOperator.INEQUALITY);
				MemberFunctionCall call = new MemberFunctionCall(base, baseFunc);
				call.addArgument(new ElementAccess(param));

				ConditionalStatement attrCond = new ConditionalStatement();
				CodeBlock action = attrCond.add(call);
				action.add(new ReturnStatement(BooleanLiteral.TRUE()));
				func.add(attrCond);
			}
		}

		// Check attributes
		for (Map.Entry<Attribute, MemberField> entry : fields.entrySet()) {
			if (!entry.getKey().isStatic()) {
				MemberField field = entry.getValue();
				ConditionalStatement attrCond = new ConditionalStatement();
				Expression attrNotEquals = new LogicalComparison(
						new MemberAccess(cls, field),
						LogicalComparison.Operator.NOT_EQUIVALENT,
						new MemberAccess(new ElementAccess(param), field));
				CodeBlock action = attrCond.add(attrNotEquals);
				action.add(new ReturnStatement(BooleanLiteral.TRUE()));
				func.add(attrCond);
			}
		}

		func.add(new ReturnStatement(BooleanLiteral.FALSE()));

		cls.addMember(CppClass.Visibility.PUBLIC, func);
	}

	/**
	 * Generate the insertion operator for the class. This is the dual of the extraction operator.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateInsertionOperator(CppClass cls) {
		Function func = new Function(LinkageSpec.UNSPECIFIED, StandardLibrary.std_ostream.ref(), AbstractFunction.OverloadedOperator.EXTRACTION);
		Parameter param1 = new Parameter(StandardLibrary.std_ostream.ref(), "os");
		Parameter param2 = new Parameter(cls.getType().const_().ref(), "obj");
		func.add(param1);
		func.add(param2);

		for (Map.Entry<Attribute, MemberField> entry : fields.entrySet()) {
			MemberField field = entry.getValue();
			Expression[] fieldInsertion = {
					new ElementAccess(param1),
					new MemberAccess(new ElementAccess(param2), field),
					new CharacterLiteral("\\n")
			};
			func.add(BinaryOperation.chain(BinaryOperation.Operator.LEFT_SHIFT, fieldInsertion));
		}

		func.add(new ReturnStatement(new ElementAccess(param1)));

		cls.addFriendFunction(func);
	}

	/**
	 * Generate code for the class's state machine.
	 * 
	 * <p>
	 * This is not currently supported for passive classes, only for {@link Capsules}.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 */
	protected void generateStateMachine(CppClass cls) {
		CodeGenPlugin.warning("'" + QualifiedNames.cachedFullName(element) + "' has stereotype attribute 'generateStateMachine' set to true, but this is not yet supported.");
	}

	/**
	 * Generate the attribute initialization of a given attribute.
	 * 
	 * @param cls
	 *            - The {@link CppClass}.
	 * @param attr
	 *            - The model {@link Attribute}.
	 * @param field
	 *            - The corresponding C++ {@link MemberField}.
	 * @param initExpr
	 *            - The C++ initializing {@link Expression}.
	 */
	protected void generateAttributeInitialization(CppClass cls, Attribute attr, MemberField field, Expression initExpr) {
		InitializationKind attrInitKind = RTCppGenerationProperties.getAttributePropInitialization(attr);

		// Static variable should be initialized in the .cpp file
		if (attr.isStatic() && !attr.isReadOnly()) {
			field.setInitKind(MemberField.InitKind.ASSIGNMENT);
			return;
		}

		if (attrInitKind == null && initExpr != null) {
			if (attr.isStatic()) {
				// if static read-only then initialize in the header file
				field.setInitKind(MemberField.InitKind.CONSTANT);
			} else {
				// default
				field.setInitKind(MemberField.InitKind.CONSTRUCTOR);
			}
			return;
		}

		if (attrInitKind != null && !attr.isStatic()) {
			switch (attrInitKind) {
			case ASSIGNMENT:
				field.setInitKind(MemberField.InitKind.ASSIGNMENT);
				break;
			case CONSTANT:
				field.setInitKind(MemberField.InitKind.CONSTANT);
				break;
			case CONSTRUCTOR:
				field.setInitKind(MemberField.InitKind.CONSTRUCTOR);
				break;
			default:
				break;
			}
		}
		if (!attr.isStatic() && attrInitKind != null && initExpr != null) {
			Constructor ctor = null;
			switch (attrInitKind) {
			case ASSIGNMENT:
				ctor = cpp.getConstructor(getOutputKind(), element);
				ctor.add(
						new BinaryOperation(
								new MemberAccess(cls, field),
								Operator.ASSIGN,
								initExpr));
				break;
			case CONSTANT:
				break;
			case CONSTRUCTOR:
				ctor = cpp.getConstructor(getOutputKind(), element);
				ctor.addFieldInitializer(field, initExpr);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public List<FileName> getGeneratedFilenames() {
		List<FileName> result = new ArrayList<>();
		ElementList el = cpp.getElementList(getOutputKind(), element);
		result.add(el.getName());
		return result;
	}

}

