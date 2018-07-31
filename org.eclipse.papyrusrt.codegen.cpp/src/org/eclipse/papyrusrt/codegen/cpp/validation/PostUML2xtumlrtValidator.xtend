/*******************************************************************************
 * Copyright (c) 2017 Zeligsoft (2009) Limited  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.papyrusrt.codegen.cpp.validation

import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.MultiStatus
import org.eclipse.emf.ecore.EObject
import org.eclipse.papyrusrt.codegen.CodeGenPlugin
import org.eclipse.papyrusrt.xtumlrt.common.Attribute
import org.eclipse.papyrusrt.xtumlrt.common.Capsule
import org.eclipse.papyrusrt.xtumlrt.common.CapsulePart
import org.eclipse.papyrusrt.xtumlrt.common.CommonElement
import org.eclipse.papyrusrt.xtumlrt.common.Connector
import org.eclipse.papyrusrt.xtumlrt.common.Entity
import org.eclipse.papyrusrt.xtumlrt.common.Port
import org.eclipse.papyrusrt.xtumlrt.common.Protocol
import org.eclipse.papyrusrt.xtumlrt.common.RedefinableElement
import org.eclipse.papyrusrt.xtumlrt.common.Signal
import org.eclipse.papyrusrt.xtumlrt.statemach.CompositeState
import org.eclipse.papyrusrt.xtumlrt.statemach.EntryPoint
import org.eclipse.papyrusrt.xtumlrt.statemach.ExitPoint
import org.eclipse.papyrusrt.xtumlrt.statemach.JunctionPoint
import org.eclipse.papyrusrt.xtumlrt.statemach.State
import org.eclipse.papyrusrt.xtumlrt.statemach.StateMachine
import org.eclipse.papyrusrt.xtumlrt.statemach.Transition
import org.eclipse.papyrusrt.xtumlrt.trans.TransformValidator
import org.eclipse.papyrusrt.xtumlrt.trans.from.uml.UML2xtumlrtTranslator
import static extension org.eclipse.papyrusrt.codegen.cpp.validation.StatusFactory.*
import static extension org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil.*
import static extension org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTExtensions.*

/**
 * Post UML2xtumlrt SM validation
 * @author ysroh
 */
class PostUML2xtumlrtValidator implements TransformValidator<EObject> {

	private UML2xtumlrtTranslator translator

	new(UML2xtumlrtTranslator translator) {
		this.translator = translator
	}

	override MultiStatus validate(EObject element) {
		val status = new MultiStatus(CodeGenPlugin.ID, IStatus.INFO, "UML-RT Code Generator Invoked", null)
		element.eAllContents.forEach[validateGeneratedElement(status)]
		status
	}

	protected def void validateGeneratedElement(EObject o, MultiStatus result) {
		if (o instanceof CommonElement) {
			val source = translator.getSource(o)
			o.validateElement(source, result)
		}
	}

	protected dispatch def void validateElement(EObject o, EObject source, MultiStatus result) {
	}

	/**
	 * Validate if the transition belongs to correct state
	 * let n1 be the source node of t and n2 its target node.
	 * 1) sibling transition: the owner of n1 is S, the owner of n2 is also S and the owner of t must be S as well.
	 * 2) entering transition: the owner of n2 is S; then the owner of t must be S as well (and n1 is S or one of its entry points)
	 * 3) exiting transition: the owner of n1 is S; then the owner of t must be S as well (and n2 is S or one of its exit points)
	 * 4) through transition: then n1 = n2 is a composite state S and the owner of t is S.
	 */
	protected dispatch def void validateElement(Transition t, EObject source, MultiStatus result) {
		if (source === null) {
			// This transition might be generated by flattening so ignore it
			return
		}
		val transitionOwner = t.eContainer

		var EObject sourceNode = t.sourceVertex
		if (sourceNode instanceof EntryPoint || sourceNode instanceof ExitPoint) {
			sourceNode = sourceNode.eContainer
		}
		var EObject targetNode = t.targetVertex
		if (targetNode instanceof EntryPoint || targetNode instanceof ExitPoint) {
			targetNode = sourceNode.eContainer
		}

		// check to see if this is a "through" transition
		if (sourceNode instanceof CompositeState && sourceNode === targetNode) {
			// check "through" transition
			if (t.sourceVertex instanceof EntryPoint || t.targetVertex instanceof ExitPoint) {
				if (targetNode !== transitionOwner) {
					addErrorStatus(source, 
						"The transition's owner is not the composite state that owns its source and target.", 
						"A \"through\" transition must be owned by its source and target state (which are the same).", 
						result)
				}
			} else {
				// We cannot differentiate a "through" transition from a "loop" transition
				// so just check loosely
				if (targetNode !== transitionOwner && targetNode.eContainer !== transitionOwner) {
					addErrorStatus(source, 
						"The transition does not belong to the correct state.",
						"The transition's owner must be either its target state or it's target's state owner.",  
						result)
				}
			}

		} // check to see if this is an entering transition
		else if (t.sourceVertex instanceof EntryPoint || sourceNode === targetNode.eContainer) {
			if (sourceNode !== transitionOwner) {
				addErrorStatus(source, 
					"The transition is not owned by it's source state.",
					"An \"entering\" transition must be owned by its source state, which itself must own the transition's target.", 
					result)
			}
		} // check to see if this is an exiting transition
		else if (t.targetVertex instanceof ExitPoint || targetNode === sourceNode.eContainer) {
			if (targetNode !== transitionOwner) {
				addErrorStatus(source, 
					"The transition is not owned by it's target state.",
					"An \"exiting\" transition must be owned by its target state, which itself must own the transition's source.", 
					result)
			}
		} // check to see if this is a "sibling" transition 
		else if (sourceNode.eContainer === targetNode.eContainer) {
			if (targetNode.eContainer !== transitionOwner) {
				addErrorStatus(source, 
					"The transition's owner is not the composite state that owns its source and target.", 
					"A \"sibling\" transition must be owned by the same composite state that owns its source and target states.", 
					result)
			}
		}
	}

	protected dispatch def void validateElement(Port port, EObject source, MultiStatus result) {
		val capsule = port.owner as Capsule
		val capsuleParent = capsule.redefines
		if (capsuleParent instanceof Capsule) {
			val allParentPorts = capsuleParent.getAllRTPorts
			if (allParentPorts.exists[ name == port.name && port.redefines !== it ]) {
				addErrorStatus(source, 
					"Port has the same name as a port in the parent capsule but it does not redefine it.",
					"A named element that redefines another named element must have the same name.",
					result)
			}
		}
	}

	protected dispatch def void validateElement(CapsulePart part, EObject source, MultiStatus result) {
		val capsule = part.owner as Capsule
		val capsuleParent = capsule.redefines
		if (capsuleParent instanceof Capsule) {
			val allParentParts = capsuleParent.getAllCapsuleParts
			if (allParentParts.exists[name == part.name && part.redefines !== it]) {
				addErrorStatus(source,
					"Port has the same name as a port in the parent capsule but it does not redefine it.",
					"A named element that redefines another named element must have the same name.",
					result)
			}
		}
	}

	protected dispatch def void validateElement(Connector conn, EObject source, MultiStatus result) {
		val capsule = conn.owner as Capsule
		val capsuleParent = capsule.redefines
		if (capsuleParent instanceof Capsule) {
			val allParentParts = capsuleParent.getAllConnectors
			if (allParentParts.exists[name == conn.name && conn.redefines !== it]) {
				addErrorStatus(source,
					"Connector has the same name as a connector in the parent capsule but it does not redefine it", 
					"A named element that redefines another named element must have the same name.",
					result)
			}
		}
	}

	protected dispatch def void validateElement(Attribute attr, EObject source, MultiStatus result) {
		val capsule = attr.owner as Entity
		val capsuleParent = capsule.redefines
		if (capsuleParent instanceof Entity) {
			val allParentAttrs = capsuleParent.allAttributes
			if (allParentAttrs.exists[name == attr.name && attr.redefines !== it]) {
				addErrorStatus(source, 
					"Attribute has the same name as a attribute in the parent capsule but it does not redefine it",
					"A named element that redefines another named element must have the same name.",
					result)
			}
		}
	}

	protected dispatch def void validateElement(Signal signal, EObject source, MultiStatus result) {
		val protocol = signal.owner as Protocol
		val protocolParent = protocol.redefines
		if (protocolParent instanceof Protocol) {
			val allParentSignals = protocolParent.allSignals
			if (allParentSignals.exists[name == signal.name && signal.redefines !== it]) {
				addErrorStatus(source, 
					"Protocol message has the same name as a protocol message in the parent protocol but it does not redefine it",
					"A named element that redefines another named element must have the same name.",
					result)
			}
		}
	}

	protected dispatch def void validateElement(State state, EObject source, MultiStatus result) {
		val owner = state.owner as RedefinableElement
		if (owner instanceof StateMachine) {
			// don't need to validate the top composite state.
			return
		}
		val redefine = owner.redefines
		if (redefine instanceof CompositeState) {
			val allParentStates = redefine.substates
			if (allParentStates.exists[name == state.name && state.redefines !== it]) {
				addErrorStatus(source,
					"State has the same name as a state in the parent state but it does not redefine it",
					"A named element that redefines another named element must have the same name.",
					result)
			}
		}
	}

}