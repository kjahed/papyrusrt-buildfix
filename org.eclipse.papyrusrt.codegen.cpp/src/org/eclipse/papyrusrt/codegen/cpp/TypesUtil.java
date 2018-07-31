/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppGenerationProperties;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.TypeBlob;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ElementAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ExpressionBlob;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.StandardLibrary;
import org.eclipse.papyrusrt.xtumlrt.aexpr.uml.XTUMLRTBoundsEvaluator;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.Entity;
import org.eclipse.papyrusrt.xtumlrt.common.Enumeration;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.common.Operation;
import org.eclipse.papyrusrt.xtumlrt.common.OperationSignature;
import org.eclipse.papyrusrt.xtumlrt.common.Parameter;
import org.eclipse.papyrusrt.xtumlrt.common.TypedMultiplicityElement;
import org.eclipse.papyrusrt.xtumlrt.util.QualifiedNames;
import org.eclipse.papyrusrt.xtumlrt.util.UndefinedValueException;

/**
 * This class contains factory methods to create Type instances in the C++ language model.
 */
public final class TypesUtil {

	/**
	 * This is a utility class, so it's constructor should be private to prevent being used as a normal object.
	 */
	private TypesUtil() {
	}

	/**
	 * Creates the C++ Type corresponding to an xtUMLrt Type. It takes into account the kind of xtUMLrt element,
	 * e.g. attribute, parameter, and the RtCppProperties stereotype specifying modifiers (such as pointsToConst, etc.)
	 * 
	 * @param cpp
	 *            The CppCodePattern
	 * @param element
	 *            The relevant model element (Attribute, Parameter, Operation)
	 * @param modelType
	 *            The xtUMLrt Type
	 * @return The corresponding C++ Type
	 */
	public static Type createCppType(final CppCodePattern cpp, final NamedElement element, final org.eclipse.papyrusrt.xtumlrt.common.Type modelType) {
		Type type = null;

		if (modelType != null) {
			type = createType(cpp, modelType);
		} else if (element == null) {
			type = PrimitiveType.VOID;
		}
		if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Attribute) {
			org.eclipse.papyrusrt.xtumlrt.common.Attribute attr = (org.eclipse.papyrusrt.xtumlrt.common.Attribute) element;
			if (RTCppGenerationProperties.getAttributeProperties(element) != null) {
				type = applyAttributeProperties(type, attr);
			} else if (attr.isReadOnly()) {
				type = type.const_();
			}
		} else if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Parameter) {
			if (modelType == null) {
				type = UMLRTRuntime.UMLRTObject.UMLRTTypedValue.getType();
			}
			org.eclipse.papyrusrt.xtumlrt.common.Parameter param = (org.eclipse.papyrusrt.xtumlrt.common.Parameter) element;
			if (param.eContainer() instanceof OperationSignature && RTCppGenerationProperties.getParameterProperties(param) != null) {
				type = applyParameterProperties(type, param);
			}
		} else if (element instanceof Operation
				&& RTCppGenerationProperties.getParameterProperties(((Operation) element).getReturnType()) != null) {
			type = applyParameterProperties(type, ((Operation) element).getReturnType());
		}
		if (type == null) {
			throw new RuntimeException("unable to create C++ type for " + QualifiedNames.fullName(element));
		}
		type = applyUpperBoundary(type, element);

		return type;
	}

	/**
	 * The RTS uses instances of UMLRTObject_class to manage serialization of types. This
	 * utility function uses the element's C++ type to create an expression that accesses
	 * the correct RTObject_class instance.
	 * 
	 * @param cpp
	 *            The CppCodePattern
	 * @param element
	 *            The relevant model element (Attribute, Parameter, Operation)
	 * @param modelType
	 *            The xtUMLrt Type
	 * @return The corresponding C++ access expression
	 */
	public static Expression createRTTypeAccess(final CppCodePattern cpp, final NamedElement element, final org.eclipse.papyrusrt.xtumlrt.common.Type modelType) {
		// Some types have descriptors provided by the RTS.
		Expression rtTypeAccess = UMLRTRuntime.UMLRTObject.UMLRTType(createCppType(cpp, element, modelType));
		if (rtTypeAccess == null) {
			// Only some system-defined types can be serialized.
			Type systemType = UMLRTRuntime.getSystemType(modelType);
			if (systemType != null) {
				rtTypeAccess = UMLRTRuntime.UMLRTObject.UMLRTType(systemType);
				if (rtTypeAccess == null) {
					throw new RuntimeException("attempt to serialize unsupported UMLRTS-defined type as part of " + element.getName());
				}
			} else {
				// Capsules cannot be serialized.
				if (element instanceof Capsule) {
					throw new RuntimeException("invalid attempt to serialize a <<Capsule>> " + element.getName());
				}

				// Otherwise return an expression to access the generated descriptor.
				Variable rtTypeVar = cpp.getVariable(CppCodePattern.Output.UMLRTTypeDescriptor, modelType);
				if (rtTypeVar == null) {
					throw new RuntimeException("invalid attempt to serialize non-serializable type " + modelType.getName());
				}

				rtTypeAccess = new ElementAccess(cpp.getVariable(CppCodePattern.Output.UMLRTTypeDescriptor, modelType));
			}
		}
		return rtTypeAccess;
	}

	/**
	 * Creates the C++ Type of a given xtUMLrt Type. Determines whether the type is a "system" type (i.e. a type from
	 * the model library), a primitive type, a structured type (struct, class, enumeration). It does not take into account
	 * modifiers given in stereotypes.
	 * 
	 * @param cpp
	 *            The CppCodePattern
	 * @param modelType
	 *            The xtUMLrt Type
	 * @return The C++ Type.
	 */
	private static Type createType(final CppCodePattern cpp, final org.eclipse.papyrusrt.xtumlrt.common.Type modelType) {
		// If this is a system-defined type then do not look at the model.
		Type type = UMLRTRuntime.getSystemType(modelType);
		if (type == null) {
			// Otherwise find an appropriate type using the content of the model.
			if (modelType instanceof org.eclipse.papyrusrt.xtumlrt.common.PrimitiveType) {
				type = createType((org.eclipse.papyrusrt.xtumlrt.common.PrimitiveType) modelType);
			} else if (modelType instanceof Entity) {
				type = createType(cpp, (Entity) modelType);
			} else if (modelType instanceof org.eclipse.papyrusrt.xtumlrt.common.StructuredType) {
				type = createType(cpp, (org.eclipse.papyrusrt.xtumlrt.common.StructuredType) modelType);
			} else if (modelType instanceof Enumeration) {
				type = createType(cpp, (Enumeration) modelType);
			} else {
				type = PrimitiveType.VOID.ptr();
			}
		}
		return type;
	}

	/**
	 * Creates a C++ struct for a given xtUMLrt StructuredType.
	 * 
	 * @param cpp
	 *            The CppCodePattern
	 * @param type
	 *            The xtUMLrt Type
	 * @return The C++ Type
	 */
	private static Type createType(final CppCodePattern cpp, final org.eclipse.papyrusrt.xtumlrt.common.StructuredType type) {
		CppClass cls = cpp.getCppClass(CppCodePattern.Output.BasicClass, type);
		cls.setKind(CppClass.Kind.STRUCT);
		return cls.getType();
	}

	/**
	 * Creates a C++ class for a given xtUMLrt Entity.
	 * 
	 * @param cpp
	 *            The CppCodePattern
	 * @param cls
	 *            The xtUMLrt Type
	 * @return The C++ Type
	 */
	private static Type createType(final CppCodePattern cpp, final Entity cls) {
		return cpp.getCppClass(CppCodePattern.Output.BasicClass, cls).getType();
	}

	/**
	 * Creates a C++ enum for a given xtUMLrt Enumeration.
	 * 
	 * @param cpp
	 *            The CppCodePattern
	 * @param element
	 *            The xtUMLrt Type
	 * @return The C++ Type
	 */
	private static Type createType(final CppCodePattern cpp, final Enumeration element) {
		return cpp.getCppEnum(CppCodePattern.Output.UserEnum, element).getType();
	}

	/**
	 * Creates a C++ built-in type given its C++ type name.
	 * 
	 * @param name
	 *            The name of the primitive type.
	 * @return The C++ Type
	 */
	private static Type createBuiltInType(final String name) {
		Type newType = null;
		if ("char".equals(name)) {
			newType = PrimitiveType.CHAR;
		} else if ("double".equals(name)) {
			newType = PrimitiveType.DOUBLE;
		} else if ("float".equals(name)) {
			newType = PrimitiveType.FLOAT;
		} else if ("int".equals(name)) {
			newType = PrimitiveType.INT;
		} else if ("void".equals(name)) {
			newType = PrimitiveType.VOID;
		} else if ("long".equals(name)) {
			newType = PrimitiveType.LONG;
		} else if ("long double".equals(name)) {
			newType = PrimitiveType.LONGDOUBLE;
		} else if ("short".equals(name)) {
			newType = PrimitiveType.SHORT;
		} else if ("unsigned int".equals(name)) {
			newType = PrimitiveType.UINT;
		} else if ("unsigned short".equals(name)) {
			newType = PrimitiveType.USHORT;
		} else if ("unsigned char".equals(name)) {
			newType = PrimitiveType.UCHAR;
		} else if ("unsigned long".equals(name)) {
			newType = PrimitiveType.ULONG;
		} else if ("bool".equals(name)) {
			newType = PrimitiveType.BOOL;
		}
		return newType;
	}

	/**
	 * Creates the C++ built-in type given a UML Primitive type name.
	 * 
	 * @param name
	 *            A UML Primitive type name.
	 * @return The corresponding C++ Type.
	 */
	private static Type createUMLType(final String name) {
		Type newType = null;
		if ("Boolean".equals(name)) {
			newType = PrimitiveType.BOOL;
		} else if ("Integer".equals(name)) {
			newType = PrimitiveType.INT;
		} else if ("Real".equals(name)) {
			newType = PrimitiveType.DOUBLE;
		} else if ("String".equals(name)) {
			newType = PrimitiveType.CHAR.ptr();
		} else if ("UnlimitedNatural".equals(name)) {
			newType = PrimitiveType.ULONGLONG;
		}
		return newType;
	}

	/**
	 * Create a C++ built-in type given the type name in the Ansi C Library.
	 * 
	 * @param name
	 *            The Ansi C Library type name.
	 * @return The C++ Type.
	 */
	private static Type createAnsiCLibraryType(final String name) {
		Type newType = null;
		if ("int8_t".equals(name)) {
			newType = StandardLibrary.int8_t;
		} else if ("int16_t".equals(name)) {
			newType = StandardLibrary.int16_t;
		} else if ("int32_t".equals(name)) {
			newType = StandardLibrary.int32_t;
		} else if ("int64_t".equals(name)) {
			newType = StandardLibrary.int64_t;
		} else if ("uint8_t".equals(name)) {
			newType = StandardLibrary.uint8_t;
		} else if ("uint16_t".equals(name)) {
			newType = StandardLibrary.uint16_t;
		} else if ("uint32_t".equals(name)) {
			newType = StandardLibrary.uint32_t;
		} else if ("uint64_t".equals(name)) {
			newType = StandardLibrary.uint64_t;
		} else if ("wchar_t".equals(name)) {
			newType = StandardLibrary.wchar_t;
		}
		return newType;
	}

	/**
	 * Creates a C++ built-in type given an xtUMLrt PrimitiveType.
	 * 
	 * @param xtumlrtType
	 *            The xtUMLrt primitive type
	 * @return The C++ Type
	 */
	private static Type createType(final org.eclipse.papyrusrt.xtumlrt.common.PrimitiveType xtumlrtType) {
		String name = xtumlrtType.getName();

		Type type = createBuiltInType(name);
		if (type == null) {
			type = createUMLType(name);
			if (type == null) {
				type = createAnsiCLibraryType(name);
				if (type == null) {
					type = PrimitiveType.VOID.ptr();
				}
			}
		}
		return type;

	}

	/**
	 * Given a C++ Type and an xtUMLrt element, it extends the C++ Type by applying the upper bound specified
	 * via modifiers on the element.
	 * 
	 * @param type
	 *            A C++ Type.
	 * @param element
	 *            An xtUMLrt element
	 * @return The modified C++ Type
	 */
	private static Type applyUpperBoundary(final Type type, final NamedElement element) {
		Type newType = type;
		// Apply pointer or array if it has an upper-bound > 1
		TypedMultiplicityElement typedMultElement = null;
		if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Operation) {
			typedMultElement = ((org.eclipse.papyrusrt.xtumlrt.common.Operation) element).getReturnType();
			try {
				int bound = XTUMLRTBoundsEvaluator.getBound(typedMultElement);
				if (bound > 1) {
					newType = newType.ptr();
				}
			} catch (final UndefinedValueException e) {
				// If we cannot find the bound, we assume it is a symbolic expression and therefore it is assumed to be larger than 1.
				newType = newType.ptr();
			}
		} else if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Parameter
				|| element instanceof org.eclipse.papyrusrt.xtumlrt.common.Attribute) {
			typedMultElement = (TypedMultiplicityElement) element;
			try {
				int bound = XTUMLRTBoundsEvaluator.getBound(typedMultElement);
				if (bound > 1) {
					newType = newType.arrayOf(new IntegralLiteral(bound));
				}
			} catch (final UndefinedValueException e) {
				// If we cannot find the bound, we assume it is a symbolic expression and therefore it is assumed to be larger than 1.
				newType = newType.arrayOf(new ExpressionBlob(e.getExpression()));
			}
		}
		return newType;
	}

	/**
	 * Given a C++ Type and an xtUMLrt Attribute, it extends the C++ Type by applying any modifiers specified in the
	 * RtCppProperties.AttributeProperties stereotype applied to the Attribute.
	 * 
	 * @param type
	 *            A C++ Type.
	 * @param element
	 *            An xtUMLrt Attribute.
	 * @return The modified C++ Type.
	 */
	private static Type applyAttributeProperties(final Type type, final org.eclipse.papyrusrt.xtumlrt.common.Attribute element) {
		Type newType = type;
		String typeStr = RTCppGenerationProperties.getAttributePropType(element);
		if (typeStr != null && !typeStr.trim().isEmpty()) {
			newType = new TypeBlob(typeStr);
		} else if (newType == null) {
			CodeGenPlugin.error(" Attribute '" + QualifiedNames.cachedFullName(element) + "' has no type");
		}

		Boolean isVolatileBool = RTCppGenerationProperties.getAttributePropVolatile(element);
		Boolean pointsToBool = RTCppGenerationProperties.getAttributePropPointsTo(element);
		Boolean pointsToConstBool = RTCppGenerationProperties.getAttributePropPointsToConst(element);
		Boolean pointsToVolatileBool = RTCppGenerationProperties.getAttributePropPointsToVolatile(element);

		boolean isConst = element.isReadOnly();
		boolean isVolatile = isVolatileBool != null && isVolatileBool.booleanValue();
		boolean pointsTo = pointsToBool != null && pointsToBool.booleanValue();
		boolean pointsToConst = pointsToConstBool != null && pointsToConstBool.booleanValue();
		boolean pointsToVolatile = pointsToVolatileBool != null && pointsToVolatileBool.booleanValue();
		boolean isPointer = pointsTo || pointsToConst || pointsToVolatile;

		if (pointsToConst) {
			if (pointsToVolatile) {
				newType = newType.constVolatile();
			} else {
				newType = newType.const_();
			}
		} else if (pointsToVolatile) {
			newType = newType.volatile_();
		}
		// if neither const nor volatile, it's a plain pointer
		if (isPointer && !isConst && !isVolatile) {
			newType = newType.ptr();
		}

		if (isConst) {
			if (isVolatile) {
				if (isPointer) {
					newType = newType.constVolatilePtr();
				} else {
					newType = newType.constVolatile();
				}
			} else {
				if (isPointer) {
					newType = newType.constPtr();
				} else {
					newType = newType.const_();
				}
			}
		} else if (isVolatile) {
			if (isPointer) {
				newType = newType.volatilePtr();
			} else {
				newType = newType.volatile_();
			}
		}

		return newType;
	}

	/**
	 * Given a C++ Type and an xtUMLrt Parameter or the (TypedMultiplicityElement) return element of an Operation,
	 * it extends the C++ Type by applying any modifiers specified in the RtCppProperties.ParameterProperties stereotype
	 * applied to the Parameter or element.
	 * 
	 * @param type
	 *            A C++ Type.
	 * @param element
	 *            An xtUMLrt Parameter or Operation return element.
	 * @return The modified C++ Type.
	 */
	private static Type applyParameterProperties(final Type type, final TypedMultiplicityElement element) {
		Type newType = type;
		String typeStr = RTCppGenerationProperties.getParameterPropType(element);
		if (typeStr != null && !typeStr.trim().equals("")) {
			newType = new TypeBlob(typeStr);
			// mustApplyUpperBoundary = false;
		} else if (newType == null) {
			if (element instanceof TypedMultiplicityElement) {
				// operation return type
				CodeGenPlugin.error(" No return type for operation '"
						+ QualifiedNames.cachedFullName((Operation) element.eContainer()) + "'");
			} else {
				CodeGenPlugin
						.error(" Parameter '" + QualifiedNames.cachedFullName((Parameter) element) + "' has no type");
			}
		}

		Boolean pointsToBool = RTCppGenerationProperties.getParameterPropPointsTo(element);
		Boolean pointsToConstBool = RTCppGenerationProperties.getParameterPropPointsToConst(element);
		Boolean pointsToVolatileBool = RTCppGenerationProperties.getParameterPropPointsToVolatile(element);

		boolean pointsTo = pointsToBool != null && pointsToBool.booleanValue();
		boolean pointsToConst = pointsToConstBool != null && pointsToConstBool.booleanValue();
		boolean pointsToVolatile = pointsToVolatileBool != null && pointsToVolatileBool.booleanValue();
		boolean isPointer = pointsTo || pointsToConst || pointsToVolatile;

		if (pointsToConst) {
			if (pointsToVolatile) {
				newType = newType.constVolatile();
			} else {
				newType = newType.const_();
			}
		} else if (pointsToVolatile) {
			newType = newType.volatile_();
		}

		if (isPointer) {
			newType = newType.ptr();
		}

		return newType;
	}

}

