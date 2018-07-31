/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.papyrusrt.codegen.UserEditableRegion;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.AbstractFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Macro;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Parameter;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.CastExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.DereferenceExpr;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ElementAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.Comment;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.DefineDirective;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.UndefDirective;
import org.eclipse.papyrusrt.codegen.lang.cpp.stmt.UserCode;
import org.eclipse.papyrusrt.codegen.utils.CodeGenUtils;

/**
 * This manager provides routines to serialize and de-serialize data. Eventually
 * an extension-point will allow this implementation to be overridden.
 */
public final class SerializationManager {

	/**
	 * The common instance of this class.
	 */
	private static final SerializationManager INSTANCE = new SerializationManager();

	/**
	 * Constructor. Private to enforce the use of {@link #getInstance()}.
	 */
	private SerializationManager() {
	}

	public static SerializationManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Generates code for {@link UserCode} provided in an action, typically a transition's action.
	 * 
	 * <p>
	 * The generated code includes first a section consisting of macros, with a macro definition for
	 * each signal parameter, providing the necessary de-serialization from the message's payload.
	 * 
	 * <p>
	 * User code is added to the function between some tags, marked according to the value of
	 * the {@link CodeGenUtils.GENERATED_END} and {@link CodeGenUtils.GENERATED_START} constants.
	 * 
	 * @param func
	 *            - The {@link AbstractFunction} where the code will be added.
	 * @param msg
	 *            - The "msg" {@link Parameter} of the function.
	 * @param params
	 *            - The {@link ParameterSet} of signal parameters.
	 * @param userCode
	 *            - The actual {@link UserCode} to be inserted.
	 * @param userRegionIdentifier
	 *            - User region identifier
	 */
	public void generateUserCode(AbstractFunction func, Parameter msg, ParameterSet params, UserCode userCode, String userRegionIdentifier) {
		List<Macro> paramMacros = params.generateDefinitions(func, msg);

		if (userRegionIdentifier != null && !userRegionIdentifier.isEmpty()) {
			func.add(new Comment(false, userRegionIdentifier));
		}

		func.add(userCode);

		if (userRegionIdentifier != null && !userRegionIdentifier.isEmpty()) {
			func.add(new Comment(false, UserEditableRegion.userEditEnd()));
		}

		// Generate the #undefs in the reverse of the definition order.
		for (int i = paramMacros.size() - 1; i >= 0; --i) {
			func.add(new UndefDirective(paramMacros.get(i)));
		}
	}

	/**
	 * User code can be invoked by multiple triggers. Each of those triggers could have
	 * multiple parameters. This container class stores all of the parameter lists that
	 * are expected by the user code.
	 */
	public static class ParameterSet {

		/** The {@link CppCodePattern}. */
		private final CppCodePattern cpp;

		/** The list of signal {@link org.eclipse.papyrusrt.xtumlrt.common.Parameter}s. */
		private List<List<org.eclipse.papyrusrt.xtumlrt.common.Parameter>> triggerParams = new ArrayList<>();

		/**
		 * Constructor.
		 *
		 * @param cpp
		 *            - The {@link CppCodePattern}.
		 */
		public ParameterSet(CppCodePattern cpp) {
			this.cpp = cpp;
		}

		/**
		 * Adds a list of model-level signal parameters to the set.
		 * 
		 * @param params
		 *            - A {@link List} of {@link org.eclipse.papyrusrt.xtumlrt.common.Parameter}s.
		 */
		public void add(List<org.eclipse.papyrusrt.xtumlrt.common.Parameter> params) {
			// Copy the list to avoid errors when the model element is changing.
			triggerParams.add(new ArrayList<>(params));
		}

		/**
		 * The user-provided parameters appear as #define'ed macros in the generated function. This
		 * utility creates the appropriate definitions and returns the list of macros. This list can
		 * be used to generate the corresponding #undef's.
		 * 
		 * @param func
		 *            - The {@link AbstractFunction}.
		 * @param msg
		 *            - The function's {@link Parameter}.
		 * @return - The {@link List} or {@link Macro} definitions.
		 */
		public List<Macro> generateDefinitions(AbstractFunction func, Parameter msg) {
			List<Macro> macros = new ArrayList<>();

			// A conflict occurs when the same name is used with a different type or
			// index.
			Map<String, ParamGuard> existingGuards = new HashMap<>();

			List<org.eclipse.papyrusrt.xtumlrt.common.Type> rtdataTypes = new ArrayList<>(triggerParams.size());
			for (List<org.eclipse.papyrusrt.xtumlrt.common.Parameter> params : triggerParams) {
				if (!params.isEmpty()) {
					rtdataTypes.add(params.get(0).getType());
				}

				for (int index = 0; index < params.size(); ++index) {
					org.eclipse.papyrusrt.xtumlrt.common.Parameter param = params.get(index);
					String paramName = param.getName();

					// If there is already a macro for this parameter name, then make sure it is compatible
					// and move to the next parameter.
					ParamGuard paramGuard = new ParamGuard(param.getType(), index);
					ParamGuard existingGuard = existingGuards.get(paramName);
					if (existingGuard != null) {
						if (!existingGuard.equals(paramGuard)) {
							throw new RuntimeException("invalid attempt to generate function with conflicting trigging parameters");
						}
					}
					// Otherwise generate a macro to access this new combination and store a
					// guard to prevent a duplicate.
					else {
						existingGuards.put(paramName, paramGuard);
						macros.add(
								createDefinition(
										func,
										msg,
										param.getType() != null
												? TypesUtil.createCppType(cpp, param, param.getType())
												: PrimitiveType.VOID,
										paramName,
										index));
					}
				}
			}

			// This generates rtdata even when the generator knows there is not any incoming data.
			// This allows the developer to temporarily remove a transitions triggers, without
			// introducing a bunch of C++ compilation errors.
			// TODO The previous implementation did the same thing as here -- if there is one type
			// then it is used. If there is more than one type, the void* is used. However,
			// I think it is actually supposed to compute a compatible type if possible.
			macros.add(
					createDefinition(
							func,
							msg,
							rtdataTypes.size() == 1
									? TypesUtil.createCppType(cpp, null, rtdataTypes.get(0))
									: PrimitiveType.VOID,
							"rtdata",
							0));

			return macros;
		}

		/**
		 * Creates the definition of the macro that access and de-serialized a specific signal parameter
		 * from the function's "message" parameter.
		 * 
		 * @param func
		 *            - The {@link AbstractFunction}.
		 * @param msg
		 *            - The function's message {@link Parameter}.
		 * @param type
		 *            - The C++ {@link Type} of the signal's parameter.
		 * @param name
		 *            - The {@link String} name of the signal parameter.
		 * @param index
		 *            - The {@code int} index of the signal parameter in the message's payload.
		 * @return The {@link Macro} definition.
		 */
		private static Macro createDefinition(AbstractFunction func, Parameter msg, Type type, String name, int index) {
			boolean isVoid = type == PrimitiveType.VOID;
			if (!(type instanceof PrimitiveType)) {
				type = type.ptr().const_();
			} else {
				type = type.ptr();
			}

			Expression castExpr = new CastExpr(
					type,
					UMLRTRuntime.UMLRTMessage.getParam(
							new ElementAccess(msg),
							new IntegralLiteral(index++)));
			Macro macro = new Macro(
					name,
					isVoid || name.equals("rtdata") ? castExpr : new DereferenceExpr(castExpr));
			func.add(new DefineDirective(macro));
			return macro;
		}

		/**
		 * The generator cannot create code for two parameters with the same name if they
		 * have a different type or index. A {@link ParamGuard} checks whether a parameter
		 * has the same type and index as another.
		 */
		private static class ParamGuard {

			/** The model type ({@link org.eclipse.papyrusrt.xtumlrt.common.Type}) to check against. */
			public final org.eclipse.papyrusrt.xtumlrt.common.Type type;

			/** The index to check against. */
			public final int index;

			/**
			 * Constructor.
			 *
			 * @param type
			 *            - The model type ({@link org.eclipse.papyrusrt.xtumlrt.common.Type}) to check against.
			 * @param index
			 *            - The index to check against.
			 */
			ParamGuard(org.eclipse.papyrusrt.xtumlrt.common.Type type, int index) {
				this.type = type;
				this.index = index;
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof ParamGuard)) {
					return false;
				}

				ParamGuard other = (ParamGuard) obj;
				return type.equals(other.type)
						&& index == other.index;
			}
		}

	}

}
