/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import org.eclipse.papyrusrt.codegen.cpp.CppCodePattern;
import org.eclipse.papyrusrt.codegen.cpp.TypesUtil;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppGenerationProperties;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.ElementList;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberField;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.OffsetOf;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AddressOfExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.BlockInitializer;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.Sizeof;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.StringLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.StandardLibrary;
import org.eclipse.papyrusrt.xtumlrt.common.Attribute;
import org.eclipse.papyrusrt.xtumlrt.common.StructuredType;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTExtensions;

/**
 * Serializable classes are basic classes that can be encoded and sent across
 * the wire. This means a deep copy that can be decoded in an independent
 * memory space.
 */
public class SerializableClassGenerator extends BasicClassGenerator {

	/** The {@link StructuredType} (struct or class). */
	private final StructuredType data;

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 * @param type
	 *            - The {@link StructuredType}.
	 */
	public SerializableClassGenerator(CppCodePattern cpp, StructuredType type) {
		super(cpp, type);
		data = type;
	}

	@Override
	protected boolean generate(CppClass cls) {
		if (!super.generate(cls)) {
			return false;
		}

		ElementList elements = cpp.getElementList(CppCodePattern.Output.BasicClass, data);

		BlockInitializer fieldsInit = new BlockInitializer(UMLRTRuntime.UMLRTObject.getFieldType().const_().arrayOf(null));
		for (Attribute attr : XTUMLRTExtensions.getAllAttributes(data)) {
			// Bug 470881: Static fields should not be serialized.
			if (!attr.isStatic()) {
				String typeStr = RTCppGenerationProperties.getAttributePropType(attr);
				if (typeStr != null) {
					continue;
				}

				Expression arraySize = GeneratorUtils.generateBoundExpression(attr);

				fieldsInit.addExpression(
						new BlockInitializer(
								UMLRTRuntime.UMLRTObject.getFieldType().const_(),
								new StringLiteral(attr.getName()),
								new AddressOfExpr(TypesUtil.createRTTypeAccess(cpp, attr, attr.getType())),
								new OffsetOf(cls, attr.getName()),
								arraySize,
								new IntegralLiteral(0))); // ptrIndirection
			}
		}

		MemberField fields = new MemberField(fieldsInit.getType(), "fields", fieldsInit);
		cls.addStaticMember(CppClass.Visibility.PUBLIC, fields);

		BlockInitializer descInit = new BlockInitializer(UMLRTRuntime.UMLRTObject.getType().const_());
		descInit.addExpression(UMLRTRuntime.UMLRTObject.UMLRTObjectGeneric_initialize(cls.getName()));
		descInit.addExpression(UMLRTRuntime.UMLRTObject.UMLRTObjectGeneric_copy(cls.getName()));
		descInit.addExpression(UMLRTRuntime.UMLRTObject.UMLRTObject_decode());
		descInit.addExpression(UMLRTRuntime.UMLRTObject.UMLRTObject_encode());
		descInit.addExpression(UMLRTRuntime.UMLRTObject.UMLRTObjectGeneric_destroy(cls.getName()));
		// descInit.addExpression( UMLRTRuntime.UMLRTObject.UMLRTObject_getSize() );
		descInit.addExpression(UMLRTRuntime.UMLRTObject.UMLRTObject_fprintf());

		descInit.addExpression(new StringLiteral(data.getName()));
		descInit.addExpression(StandardLibrary.NULL()); // TODO super (Base type)
		descInit.addExpression(
				new BlockInitializer(
						UMLRTRuntime.UMLRTObject.Object.getType(),
						new Sizeof(TypesUtil.createCppType(cpp, data, data)),
						new IntegralLiteral(fieldsInit.getNumInitializers()),
						new MemberAccess(cls, fields)));
		descInit.addExpression(UMLRTRuntime.UMLRTObject.DEFAULT_VERSION());
		descInit.addExpression(UMLRTRuntime.UMLRTObject.DEFAULT_BACKWARDS());

		// Variable desc = new Variable( LinkageSpec.STATIC, descInit.getType(), "desc", descInit );
		// elements.addElement( desc );

		Variable rttype = cpp.getVariable(CppCodePattern.Output.UMLRTTypeDescriptor, data);
		rttype.setInitializer(descInit);
		elements.addElement(rttype);

		return true;
	}

}
