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
import org.eclipse.papyrusrt.xtumlrt.util.DetailedException
import org.eclipse.xtend.lib.annotations.Data
import static extension org.eclipse.papyrusrt.xtumlrt.util.NamesUtil.*

/**
 * @author epp
 */
@Data
class ValidationException extends DetailedException {

	static enum Kind { ERROR, WARNING }

	Kind kind
	EObject element
	String shortMsg
	String description

	override String toString() '''
		Validation «kind.kindStr»: «shortMsg.trim»
		  Element type: «getElementTypeStr» 
		  Element qualified name: «getElementQualifiedNameStr»
		  Description: «description»
	'''

	def kindStr(Kind kind) {
		switch kind {
			case Kind.ERROR:		"error"
			case Kind.WARNING:	"warning"
			default:				"info"
		}
	}

	def String getElementTypeStr() {
		if (element === null) {
			return "null element"
		}
		return element.eClass.name
	}

	def String getElementQualifiedNameStr() {
		if (element === null) {
			return "null element"
		}
		if (element instanceof org.eclipse.uml2.uml.NamedElement) {
			val umlQualifiedName = element.qualifiedName
			if (umlQualifiedName !== null && !umlQualifiedName.trim.empty) {
				return umlQualifiedName
			} else {
				return element.effectiveQualifiedName
			}
		}
		if (element instanceof Enum<?>) {
			return element.name
		}
		if (element instanceof EObject) {
			return element.effectiveQualifiedName
		}
		return "non-UML element: " + element.toString
	}

}
