/*****************************************************************************
 * Copyright (c) 2017 Zeligsoft (2009) Ltd and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Ernesto Posse - Initial API and implementation
 *****************************************************************************/

package org.eclipse.papyrusrt.codegen.cpp.validation

import org.eclipse.emf.ecore.EObject
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus
import org.eclipse.papyrusrt.codegen.CodeGenPlugin
import org.eclipse.papyrusrt.codegen.cpp.validation.ValidationException
import org.eclipse.core.runtime.MultiStatus

/**
 * @author epp
 */
class StatusFactory {
	
	static def Status createErrorStatus(EObject element, String msg) {
		createErrorStatus(element, msg, "No detailed description available", false)
	}
	
	static def Status createErrorStatus(EObject element, String msg, String description) {
		createErrorStatus(element, msg, description, false)
	}
	
	static def Status createErrorStatus(EObject element, String msg, String description, boolean throwExn) {
		createStatus(ValidationException.Kind.ERROR, element, msg, description, throwExn)
	}
	
	static def void addErrorStatus(EObject element, String msg, MultiStatus aggregateStatus) {
		addStatus(ValidationException.Kind.ERROR, element, msg, "No detailed description available", aggregateStatus)
	}

	static def void addErrorStatus(EObject element, String msg, String description, MultiStatus aggregateStatus) {
		addStatus(ValidationException.Kind.ERROR, element, msg, description, aggregateStatus)
	}

	static def Status createWarningStatus(EObject element, String msg) {
		createWarningStatus(element, msg, "No detailed description available", false)
	}
	
	static def Status createWarningStatus(EObject element, String msg, String description) {
		createWarningStatus(element, msg, description, false)
	}
	
	static def Status createWarningStatus(EObject element, String msg, String description, boolean throwExn) {
		createStatus(ValidationException.Kind.WARNING, element, msg, description, throwExn)
	}

	static def void addWarningStatus(EObject element, String msg, MultiStatus aggregateStatus) {
		addStatus(ValidationException.Kind.WARNING, element, msg, "No detailed description available", aggregateStatus)
	}

	static def void addWarningStatus(EObject element, String msg, String description, MultiStatus aggregateStatus) {
		addStatus(ValidationException.Kind.WARNING, element, msg, description, aggregateStatus)
	}

	static def createStatus(ValidationException.Kind kind, EObject element, String msg, String description, boolean throwExn) {
		val exception = new ValidationException(kind, element, msg, description)
		val status = new Status(kind.IStatusOfKind, CodeGenPlugin.ID, exception.toString, exception)
		if (throwExn) {
			throw exception
		}
		status
	}

	static def addStatus(ValidationException.Kind kind, EObject element, String msg, String description, MultiStatus aggregateStatus) {
		val status = createStatus(kind, element, msg, description, false)
		aggregateStatus.add(status)
	}

	static def getIStatusOfKind(ValidationException.Kind kind) {
		switch kind {
			case ERROR:		IStatus.ERROR
			case WARNING:	IStatus.WARNING
			default:			IStatus.INFO
		}
	}

}