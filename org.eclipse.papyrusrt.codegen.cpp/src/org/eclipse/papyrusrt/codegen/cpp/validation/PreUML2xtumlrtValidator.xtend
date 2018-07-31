/*******************************************************************************
 * Copyright (c) 2017 Zeligsoft (2009) Limited  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.papyrusrt.codegen.cpp.validation

import com.google.common.base.Strings
import java.util.List
import java.util.Optional
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.MultiStatus
import org.eclipse.emf.ecore.EObject
import org.eclipse.papyrusrt.codegen.CodeGenPlugin
import org.eclipse.papyrusrt.codegen.cpp.CppCodeGenPlugin
import org.eclipse.papyrusrt.codegen.cpp.UMLPrettyPrinter
import org.eclipse.papyrusrt.umlrt.uml.UMLRTGuard
import org.eclipse.papyrusrt.xtumlrt.trans.TransformValidator
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.LiteralUnlimitedNatural
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Pseudostate
import org.eclipse.uml2.uml.PseudostateKind
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.State
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Transition
import org.eclipse.uml2.uml.Trigger
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.ValueSpecification
import static extension org.eclipse.papyrusrt.codegen.cpp.validation.StatusFactory.*
import static extension org.eclipse.papyrusrt.xtumlrt.external.predefined.UMLRTStateMachProfileUtil.*
import static extension org.eclipse.papyrusrt.xtumlrt.external.predefined.UMLRTProfileUtil.*
import static extension org.eclipse.papyrusrt.xtumlrt.util.ContainmentUtils.*
import org.eclipse.papyrusrt.umlrt.uml.internal.impl.PortRTImpl

/**
 * Pre UML2xtumlrt validation
 * @author ysroh
 */
class PreUML2xtumlrtValidator implements TransformValidator<List<EObject>> {

	extension UMLPrettyPrinter prettyPrinter = new UMLPrettyPrinter
	
	static val UNLIMITED_NATURAL = LiteralUnlimitedNatural.UNLIMITED

	override MultiStatus validate(List<EObject> context) {
		val status = new MultiStatus(CodeGenPlugin.ID, IStatus.INFO, "UML-RT Code Generator - pre-generation validation", null)
		for (e : context) {
			e.eAllContents.forEach[validateElement(status); validateDuplicateElement(status)]
		}
		status
	}

	protected dispatch def void validateElement(EObject e, MultiStatus result) {
	}

	protected dispatch def void validateElement(Constraint element, MultiStatus result) {
		val guard = UMLRTGuard.getInstance(element)
		if (guard !== null && !guard.bodies.containsKey(CppCodeGenPlugin.LANGUAGE)) {
			addErrorStatus(element, "Guard must have a C++ body specification", "No C++ body was found.", result)
		} 
	}

	protected dispatch def void validateElement(MultiplicityElement element, MultiStatus result) {
		val lower = element.lowerValue
		if (lower !== null) {
			lower.validateElement(result)
		}
		val upper = element.upperValue
		if (upper !== null) {
			upper.validateElement(result)
		}
	}

	/**
	 * Validates properties that are not ports, i.e. parts or attributes.
	 * 
	 * <p>Parts must satisfy the following:
	 * 
	 * <ol>
	 *   <li> A part must have the {@link CapsulePart} stereotype
	 *   <li> A part must have a type
	 *   <li> The type of a part must be a {@link Capsule}
	 *   <li> A part must have a replication set.
	 *   <li> A part's replication must not be 0..*
	 * </ol>
	 * 
	 * <p>Note: if the property doesn't have the {@link CapsulePart} stereotype we do not issue a warning or error
	 * because it might be an attribute. 
	 * 
	 * @param element - A {@link Property}.
	 * @param result - A {@link MultiStatus}.
	 */
	protected dispatch def void validateElement(Property property, MultiStatus result) {
		if (property.isCapsulePart) {
			// The part must have a type and the type must be a capsule
			if (property.type === null) {
				addErrorStatus(property, "The part's type is unset.", "All parts must have their type set to be a Capsule.", result)
			} else {
				if (!property.type.isCapsule) {
					addErrorStatus(property, "The part's type is not a Capsule.", "All parts must have their type set to be a Capsule.", result)
				}
			}
			// Replication must be set and it must not be 0..*
			if (!isReplicationSet(property)) {
				addWarningStatus(property, "The part has no replication set. Assuming 1.", 
					"The replication of a part is its multiplicity. The multiplicity's lower and upper values are derived from the replication. "
					+ "If no replication is given the default (1) is assumed. A part must not have replication set to '0..*'.",
					result)
			} else if (property.replication.get().isUnlimited) {
				addErrorStatus(property, "The part has replication set to 0..*. This is not allowed.",
					"The part's replication must be set to a positive integer or an arithmetic expression " +
					"with constants and variables defined in the namespace.",
					result)
			}
		}
	}

	/**
	 * Validates a port.
	 * 
	 * <p>Ports must satisfy the following:
	 * 
	 * <ol>
	 *   <li> A port must have the {@link RTPort} stereotype
	 *   <li> A port must have a type
	 *   <li> The type of a port must be a {@link Protocol}
	 *   <li> A port must have a replication set.
	 *   <li> A port's replication must not be 0..*
	 * </ol>
	 * 
	 * @param element - A {@link Port}.
	 * @param result - A {@link MultiStatus}.
	 */
	protected dispatch def void validateElement(Port port, MultiStatus result) {
		if (!port.isRTPort) {
			addWarningStatus(port, "This port doesn't have the RTPort stereotype.", result)
		}
		// The port must have a type and the type must be a protocol
		if (port.type === null) {
			addErrorStatus(port, "The port's type is unset.", "All ports must have their type set to be a Protocol.", result)
		} else {
			if (!port.type.isProtocol) {
				addErrorStatus(port, "The port's type is not a protocol.", "All ports must have their type set to be a Protocol.", result)
			}
		}
		// Replication must be set and it must not be 0..*
		if (!isReplicationSet(port)) {
			addWarningStatus(port, "The port has no replication set. Assuming 1.", 
				"The replication of a port is its multiplicity. The multiplicity's lower and upper values are derived from the replication. "
				+ "If no replication is given the default (1) is assumed. A port must not have replication set to '0..*'.",
				result)
		} else if (port.replication.get().isUnlimited) {
			addErrorStatus(port, "The port has replication set to 0..*. This is not allowed.",
				"The port's replication must be set to a positive integer or an arithmetic expression " +
				"with constants and variables defined in the namespace.",
				result)
		}
	}

	private def isReplicationSet(MultiplicityElement element) {
		val lowerValueIsSet = element.eIsSet(UMLPackage.Literals.MULTIPLICITY_ELEMENT__LOWER_VALUE) || element.lowerValue !== null
		val upperValueIsSet = element.eIsSet(UMLPackage.Literals.MULTIPLICITY_ELEMENT__UPPER_VALUE) || element.upperValue !== null
		return (lowerValueIsSet || upperValueIsSet)
	}

	private def getReplication(MultiplicityElement element) {
		var lowerValue = element.lowerValue
		var upperValue = element.upperValue
		val lowerValueIsSet = element.eIsSet(UMLPackage.Literals.MULTIPLICITY_ELEMENT__LOWER_VALUE) || lowerValue !== null
		val upperValueIsSet = element.eIsSet(UMLPackage.Literals.MULTIPLICITY_ELEMENT__UPPER_VALUE) || upperValue !== null
		if (upperValueIsSet) {
			Optional.of(upperValue)
		} else if (lowerValueIsSet) {
			Optional.of(lowerValue)
		} else {
			Optional.empty
		}
	}

	private dispatch def boolean isUnlimited(ValueSpecification spec) {
		false
	}
	
	private dispatch def boolean isUnlimited(LiteralUnlimitedNatural spec) {
		spec.value == UNLIMITED_NATURAL
	}

	/**
	 * Validates a state.
	 * 
	 * <p>Checks whether there are conflicting outgoing transitions from the state. 
	 *  
	 * @param state - A {@link State}.
	 * @param result - A {@link MultiStatus}.
	 */
	protected dispatch def void validateElement(State state, MultiStatus result) {
		val containingStates = 
			state.getAllOwningElementsUptoType(StateMachine)
				.filter[it instanceof State]
				.map[it as State]
		val allOutgoingTransitionsHierarchy = containingStates.map[allOutgoingTransitions].flatten
		for (transition : state.allOutgoingTransitions) {
			val otherEquivalentTransitions = allOutgoingTransitionsHierarchy.filter[conflict(it, transition)]
			if (!otherEquivalentTransitions.empty) {
				val conflictingTransitions = otherEquivalentTransitions.multiLineListText
				addWarningStatus(state, "State has conflicting transitions.",
					"The state has at least two conflicting (ambiguous) outgoing transitions with the same trigger and guard. \n"
					+ "Transition " + transition.text + "conflicts with the following:\n"
					+ conflictingTransitions + "\n"
					+ "The transition with the deepest source will be selected and the others ignored.\n"
					+ "If there is more than one such transitions any one of them will be selected and others will be ignored.\n"
					+ "Note that the transitions may have a different source, namely a composite state that contains this state.\n",
					result)
				return
			}
		}
	}
	
	/**
	 * @param transition1 - A {@link Transition}.
	 * @param transition2 - A {@link Transition}.
	 * @return {@code true} iff the two transition conflict, i.e. if they are not the same but 
	 * they have a common trigger (cf. {@link #commonTrigger}) and the same guard (cf. {@link #sameGuard}).
	 * Note that this method already assumes that the source of one of the transitions is the same as or is
	 * contained in the source of the other transition.
	 */
	private def conflict(Transition transition1, Transition transition2) {
		transition1 !== transition2
		&& commonTrigger(transition1, transition2) 
		&& sameGuard(transition1, transition2)
	}
	
	/**
	 * @param transition1 - A {@link Transition}.
	 * @param transition2 - A {@link Transition}.
	 * @return {@code true} iff the two transition have equal guards or at least guards with equal specification.
	 */
	private def sameGuard(Transition transition1, Transition transition2) {
		transition1.guard == transition2.guard
		|| transition1.guard?.specification == transition2.guard?.specification
	}
	
	/**
	 * @param transition1 - A {@link Transition}.
	 * @param transition2 - A {@link Transition}.
	 * @return {@code true} iff the two transition have at least one equivalent trigger (cf. {@link #equivalentTrigger})
	 */
	private def commonTrigger(Transition transition1, Transition transition2)
	{
		transition1.triggers.exists[t1 | transition2.triggers.exists[t2 | equivalentTrigger(t1, t2)]]
	}
	
	/**
	 * @param trigger1 - A {@link Trigger}.
	 * @param trigger2 - A {@link Trigger}.
	 * @return {@code true} iff the two triggers are eequivalent, i.e., iff they are equal or 
	 * the have the same event and at least one port in common.
	 */
	private def equivalentTrigger(Trigger trigger1, Trigger trigger2) {
		trigger1 == trigger2
		|| trigger1.event == trigger2.event
			&& trigger1.ports.exists[trigger2.ports.contains(it)]
	}

	/**
	 * Validates a transition.
	 * 
	 * <p>A transition must satisfy the following:
	 * 
	 * <ol>
	 *   <li>If the transition is the first segment in a transition chain, it should have a trigger (but it may be added by a subclass)
	 * </ol>
	 *  
	 * @param transition - A {@link Transition}.
	 * @param result - A {@link MultiStatus}.
	 */
	protected dispatch def void validateElement(Transition transition, MultiStatus result) {
		if (transition.isFirstSegment) {
			if (transition.triggers.empty) {
				addWarningStatus(transition,
					"Transition has no triggers",
					"A transition which is the first segment in a transition chain, i.e. a transition which " +
					"leaves a state, should have at least one trigger. Triggers may be added in state machines " + 
					"of capsules which are subclasses of this capsule.",
					result)
			}
		}
	}
	
	/**
	 * Determines if the given transition is the first segment in a transition chain, 
	 * this is, if the source of the transition is a state either directly or as an exit point.
	 * 
	 * @param t - A {@link Transition}
	 * @return {@code true} iff t's source is either a {@link State} or an exit point with no incoming transitions.
	 */
	private def isFirstSegment(Transition t) {
		val source = t.source
		source instanceof State
		|| source instanceof Pseudostate 
			&& (source as Pseudostate).kind == PseudostateKind.EXIT_POINT_LITERAL
			&& source.incomings.empty
	}

	/**
	 * Validates a trigger.
	 * 
	 * <p>A trigger must satisfy the following:
	 * 
	 * <ol>
	 *   <li>A trigger should have the {@link RTTrigger} stereotype
	 *   <li>A trigger should have an event
	 *   <li>A trigger should have a port
	 * </ol>
	 *  
	 * @param trigger - A {@link Trigger}.
	 * @param result - A {@link MultiStatus}.
	 */
	protected dispatch def void validateElement(Trigger trigger, MultiStatus result) {
		if (!trigger.isRTTrigger) {
			addWarningStatus(trigger,
				"Trigger doesn't have the \"RTTrigger\" stereotype applied.",
				"Triggers without the \"RTTrigger\" stereotype might lead to incorrectly generated code.",
				result)
		}
		if (trigger.event === null) {
			addWarningStatus(trigger,
				"Trigger has no event",
				"A trigger should have an event associated. Without an event, code generation might fail or it might produce incorrect code.",
				result)
		}
		if (trigger.ports.empty) {
			addWarningStatus(trigger,
				"Trigger has no ports",
				"A trigger should have at least one port associated. Without a port, code generation might fail or it might produce incorrect code.",
				result)
		}
	}
	
	/**
	 * Validates a pseudostate.
	 * 
	 * <p>Pseudostates must satisfy the following:
	 * 
	 * <ol>
	 *   <li>A choice point should have at least one outgoing transition.
	 *   <li>For a choice point with only one outgoing transition, the transition should not have a guard.
	 *   <li>A choice point should not have multiple unguarded outgoing transitions. 
	 *   <li>A junction point should have one outgoing transition.
	 *   <li>A junction point must not have more than one outgoing transition.
	 * </ol>
	 * 
	 * @param pseudostate - A {@link Pseudostate}
	 * @param result - A {@link MultiStatus}
	 */
	protected dispatch def void validateElement(Pseudostate pseudostate, MultiStatus result) {
		if (pseudostate.isChoicePoint) {
			val outgoingTransitions = pseudostate.outgoings
			if (outgoingTransitions.empty) {
				addWarningStatus(pseudostate,
					"Choice point has no outgoing transitions.",
					"A choice point should have outgoing transitions. Outgoing transitions may be specified in " +
					"state machines of capsules which are subclasses of this capsule.",
					result)
			} else if (outgoingTransitions.size == 1) {
				if (outgoingTransitions.get(0).guard !== null) {
					addWarningStatus(pseudostate,
						"Choice point has exactly one guarded outgoing transition.",
						"If a choice point has only one outgoing transition, it should be guarded. ",
						result)
				}
			} else { // pseudostate.outgoings.size > 1
				val unguardedTransitions = outgoingTransitions.filter[guard === null]
				if (unguardedTransitions.size > 1) {
					addWarningStatus(pseudostate,
						"Choice point has multiple unguarded transitions.",
						"A choice point should have one unguarded transition at most.",
						result)
				}
			}
		} else if (pseudostate.isJunctionPoint) {
			if (pseudostate.outgoings.empty) {
				addWarningStatus(pseudostate,
					"Junction point has no outgoing transitions.",
					"A junction point should have outgoing transitions. Outgoing transitions may be specified in " +
					"state machines of capsules which are subclasses of this capsule.",
					result)
			} else if (pseudostate.outgoings.size > 1) {
				addErrorStatus(pseudostate,
					"Junction point has more than one outgoing transition.",
					"A junction point can have one and only one outgoing transition.",
					result)
			}
		}
	}
	
	
	/**
	 * Check for duplicated named elements.
	 */
	protected def dispatch void validateDuplicateElement(Object element, MultiStatus result) {
	}

	protected def dispatch void validateDuplicateElement(NamedElement element, MultiStatus result) {
		if (element.namespace !== null && !Strings.isNullOrEmpty(element.name)) {
			val unique = element.namespace.ownedMembers.filter[e | e !== element && e.eClass === element.eClass && e.name == element.name].empty
			if (!unique) {
				addWarningStatus(element, "More than one element with the same name exist in the same namespace.", result)
			}
		}
	}


}
