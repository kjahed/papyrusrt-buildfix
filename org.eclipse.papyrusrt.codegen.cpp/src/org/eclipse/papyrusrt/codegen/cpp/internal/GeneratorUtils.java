/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.internal;

import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ExpressionBlob;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.IntegralLiteral;
import org.eclipse.papyrusrt.xtumlrt.aexpr.uml.XTUMLRTBoundsEvaluator;
import org.eclipse.papyrusrt.xtumlrt.common.MultiplicityElement;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;

/**
 * General utility methods.
 */
public final class GeneratorUtils {

	/**
	 * Default Constructor. It is private since this is a utility class.
	 */
	private GeneratorUtils() {
	}

	/**
	 * Obtain an expression representing the replication of an element.
	 * 
	 * <p>
	 * The replication is computed from the model element's lower and upper bounds.
	 * If the model element has an upper bound with an integer value, an {@link IntegralLiteral}
	 * with that value is returned. If the lower bound has an integer value larger than the
	 * upper bound, or the upper bound cannot be parsed, the lower bound is returned. If neither
	 * the lower nor upper bound can be parsed, return an {@link ExpressionBlob} with the expression
	 * occuring in the upper bound.
	 * 
	 * @param element
	 *            - A {@link MultiplicityElement}.
	 * @param <T>
	 *            - The type of the element.
	 * @return An {@link Expression} representing the bound, i.e. replication of the given element.
	 */
	public static <T extends MultiplicityElement & NamedElement> Expression generateBoundExpression(T element) {
		return new ExpressionBlob(XTUMLRTBoundsEvaluator.getBoundString(element));
	}

	/**
	 * @param s
	 *            - A {@link String}.
	 * @return An {@link IntegralLiteral} expression if {@code s} can be parsed as an integer,
	 *         or an {@link ExpressionBlob} otherwise.
	 */
	public static Expression generateIntegralOrBlob(String s) {
		Expression result = null;
		try {
			int i = Integer.parseInt(s);
			result = new IntegralLiteral(i);
		} catch (NumberFormatException e) {
			result = new ExpressionBlob(s);
		}
		return result;
	}

}
