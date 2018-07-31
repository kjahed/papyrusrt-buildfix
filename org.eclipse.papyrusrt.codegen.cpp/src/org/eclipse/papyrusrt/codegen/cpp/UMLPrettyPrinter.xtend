/*****************************************************************************
 * Copyright (c) 2017 Zeligsoft and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Ernesto Posse - Initial API and implementation
 *****************************************************************************/

package org.eclipse.papyrusrt.codegen.cpp

import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Pseudostate
import org.eclipse.uml2.uml.PseudostateKind
import org.eclipse.uml2.uml.State
import org.eclipse.uml2.uml.Transition
import org.eclipse.uml2.uml.Trigger
import static extension org.eclipse.papyrusrt.xtumlrt.util.NamesUtil.*
import static extension org.eclipse.papyrusrt.xtumlrt.util.ContainmentUtils.*
import static extension org.eclipse.papyrusrt.xtumlrt.external.predefined.UMLRTStateMachProfileUtil.*
import org.eclipse.uml2.uml.Region
import org.eclipse.uml2.uml.StateMachine

/**
 * @author epp
 */
class UMLPrettyPrinter {

	dispatch def String text(NamedElement element)
	'''«element.name»'''

	dispatch def String text(StateMachine stateMachine)
	'''
	state machine «stateMachine.effectiveQualifiedName»
	'''

	dispatch def String text(Region region) {
		(region.owner as NamedElement).text
	}

	dispatch def String text(State state)
	'''
	state «state.effectiveQualifiedName»
	'''

	dispatch def String text(Pseudostate pseudostate)
	'''
	«pseudostate.pseudoStateKindText» «pseudostate.effectiveQualifiedName»
	'''

	private def pseudoStateKindText(Pseudostate pseudostate) {
		switch pseudostate.kind {
			case PseudostateKind.CHOICE_LITERAL:			"choice point"
			case PseudostateKind.JUNCTION_LITERAL:		"junction point"
			case PseudostateKind.ENTRY_POINT_LITERAL:	"entry point"
			case PseudostateKind.EXIT_POINT_LITERAL:		"exit point"
			case PseudostateKind.INITIAL_LITERAL:		"initial point"
			case PseudostateKind.DEEP_HISTORY_LITERAL:	"deep history point"
			case PseudostateKind.TERMINATE_LITERAL:		"terminate point"
			default: "pseudostate"
		}
	}

	dispatch def String text(Transition transition)
	'''
	transition «transition.name» 
		with triggers «FOR t : getTriggers( transition ) BEFORE '[' SEPARATOR ';' AFTER ']'»«t.text»«ENDFOR»
		from «transition.source.text»
		to «transition.target.text»
	'''
	
	dispatch def String text(Trigger trigger)
	'''«trigger.event.text» on «FOR p : trigger.ports BEFORE '[' SEPARATOR ';' AFTER ']'»«p.name»«ENDFOR»'''

	dispatch def String text(CallEvent event)
	'''«event.operation.name»'''
	
	def String oneLineListText(Iterable<? extends NamedElement> list)
	'''«FOR element : list BEFORE '[' SEPARATOR ';' AFTER ']'»«element.text»«ENDFOR»'''

	def String multiLineListText(Iterable<? extends NamedElement> list)
	'''
	«FOR element : list BEFORE '[\n' AFTER ']'»
		«element.text»;
	«ENDFOR»
	'''

	def String groupText(Iterable<NamedElement> elements) {
		val context = lowestCommonAncestor(elements) as NamedElement
		
		'''
		context: «context.text»
		elements:
			«FOR element : elements»
				* «element.getEffectiveRelativeQualifiedName(context)»
			«ENDFOR»
		'''
	}

}