/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.papyrus.propertytester;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrus.infra.emf.utils.EMFHelper;
import org.eclipse.papyrusrt.umlrt.core.utils.UMLRTProfileUtils;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * Property tester for UMLRT codegen plugin.
 * 
 * @author ysroh
 */
public class UMLRTPropertyTester extends PropertyTester {

	/** Stereotype property name. */
	private static final String STEREOTYPE_PROPERTY = "stereotype";

	/** eClass type property name. */
	private static final String ECLASS_PROPERTY = "eClass";

	/** isSourceEditable type property name. */
	private static final String IS_SOURCE_EDITABLE_PROPERTY = "isSourceEditable";

	/**
	 * Constructor.
	 *
	 */
	public UMLRTPropertyTester() {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		boolean result = false;
		Object element = receiver;
		if (receiver instanceof List && !((List) receiver).isEmpty()) {
			element = ((List) receiver).get(0);
		}

		EObject eobj = EMFHelper.getEObject(element);

		if (eobj != null) {
			if (eobj instanceof Element && STEREOTYPE_PROPERTY.equals(property)) {
				Stereotype st = ((Element) eobj).getAppliedStereotype((String) expectedValue);
				result = st != null;
			} else if (ECLASS_PROPERTY.equals(property)) {
				result = eobj.eClass().getName().equals(expectedValue);
			} else if (IS_SOURCE_EDITABLE_PROPERTY.equals(property)) {
				result = ((Boolean) expectedValue) == isSourceEditable(eobj);
			}
		}
		return result;
	}

	/**
	 * Queries if the object has editable user code.
	 * 
	 * @param eo
	 *            eobject to test
	 * @return true if source has user code.
	 */
	private boolean isSourceEditable(EObject eo) {
		boolean result = false;
		if (eo instanceof Element && UMLRTProfileUtils.isUMLRTProfileApplied((Element) eo)) {
			if (eo.eClass().equals(UMLPackage.Literals.CLASS) || eo.eClass().equals(UMLPackage.Literals.OPERATION)) {
				result = true;
			} else if (eo instanceof Transition) {
				result = true;
			} else if (eo instanceof State) {
				result = true;
			} else if (eo instanceof Trigger) {
				result = true;
			}
		}
		return result;
	}
}
