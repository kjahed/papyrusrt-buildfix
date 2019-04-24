/*******************************************************************************
 * Copyright (c) 2014-2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Young-Soo Roh - Initial API and implementation
 *   
 *****************************************************************************/

package org.eclipse.papyrusrt.codegen.papyrus.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.papyrusrt.codegen.UMLRTCodeGenerator;
import org.eclipse.papyrusrt.codegen.UserEditableRegion;
import org.eclipse.papyrusrt.codegen.UserEditableRegion.Label;
import org.eclipse.papyrusrt.codegen.UserEditableRegion.TriggerDetail;
import org.eclipse.papyrusrt.codegen.config.CodeGenProvider;
import org.eclipse.papyrusrt.codegen.papyrus.Activator;
import org.eclipse.papyrusrt.codegen.papyrus.PapyrusUMLRT2CppCodeGenerator;
import org.eclipse.papyrusrt.codegen.papyrus.actionprovider.EditSourceActionProvider;
import org.eclipse.papyrusrt.codegen.papyrus.cdt.EditorUtil;
import org.eclipse.papyrusrt.codegen.papyrus.cdt.UMLEObjectLocator;
import org.eclipse.papyrusrt.umlrt.core.utils.StateUtils;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTState;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTTransition;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTTrigger;
import org.eclipse.papyrusrt.xtumlrt.util.QualifiedNames;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.uml2.uml.AnyReceiveEvent;
import org.eclipse.uml2.uml.CallEvent;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Event;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Pseudostate;
import org.eclipse.uml2.uml.PseudostateKind;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * Edit Source action.
 * 
 * @author ysroh
 *
 */
public class EditSourceAction extends Action {

	/** The code generator. */
	protected UMLRTCodeGenerator generator = CodeGenProvider.getDefault().get();

	/**
	 * 
	 */
	private String label;
	/**
	 * 
	 */
	private EObject context;

	/**
	 * 
	 * Constructor.
	 *
	 * @param context
	 *            context
	 * @param label
	 *            action label
	 */
	public EditSourceAction(EObject context, String label) {
		this.label = label;
		this.context = context;

		setEnabled(shouldEnable());
	}

	/**
	 * @see org.eclipse.jface.action.Action#getText()
	 *
	 * @return
	 */
	@Override
	public String getText() {
		return label;
	}

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 *
	 */
	@Override
	public void run() {

		IFile file = getFile((NamedElement) context);
		Label tag = getLabel(context);

		if (context.eClass().equals(UMLPackage.Literals.STATE)) {
			if (EditSourceActionProvider.ENTRY_CODE_MENU.equals(label)) {
				tag.setType(UMLPackage.Literals.STATE__ENTRY.getName());
			} else {
				tag.setType(UMLPackage.Literals.STATE__EXIT.getName());
			}
		} else if (context instanceof Transition) {
			if (EditSourceActionProvider.TRANSITION_GUARD_CODE_MENU.equals(label)) {
				tag.setType(UMLPackage.Literals.TRANSITION__GUARD.getName().toLowerCase());
			}
		} else if (context instanceof Trigger) {
			tag.setType(UMLPackage.Literals.TRANSITION__TRIGGER.getName().toLowerCase());
		}
		// Generate source code
		EObject root = EcoreUtil.getRootContainer(context);
		String top = UMLRTCppCodeGen.getTopCapsuleName((Element) root);
		IStatus result = generator.generate(Collections.singletonList(root), top, true);
		if (result.getSeverity() == MultiStatus.ERROR) {
			MessageBox messageBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
			messageBox.setText("Error");
			messageBox.setMessage("Code generation failed!");
			messageBox.open();
		} else {
			// Open CDT editor
			MultiStatus rc = new MultiStatus(Activator.PLUGIN_ID, 0, "Edit Source Action Status", null);
			EditorUtil.openEditor(file, tag, rc);
		}
	}

	/**
	 * Check if target element exist.
	 * 
	 * @return true if exist
	 */
	private boolean shouldEnable() {
		boolean result = true;

		if (context.eClass().equals(UMLPackage.Literals.STATE)) {
			final UMLRTState state = UMLRTState.getInstance((State) context);

			if (EditSourceActionProvider.ENTRY_CODE_MENU.equals(label) && state.getEntry() == null) {
				result = false;
			} else if (state.getExit() == null) {
				result = false;
			}
		} else if (context instanceof Transition) {
			final UMLRTTransition transition = UMLRTTransition.getInstance((Transition) context);
			if (EditSourceActionProvider.TRANSITION_GUARD_CODE_MENU.equals(label) && transition.getGuard() == null) {
				result = false;
			} else if (transition.getEffect() == null) {
				// create effect if not found.
				result = false;
			}
		} else if (context instanceof Trigger) {
			UMLRTTrigger trigger = UMLRTTrigger.getInstance((Trigger) context);
			if (trigger.getGuard() == null) {
				result = false;
			}
		}

		return result;
	}

	/**
	 * Compute label.
	 * 
	 * @param context
	 *            EObject
	 * @return label info
	 */
	public Label getLabel(EObject context) {
		Label label = new Label();
		label.setQualifiedName(getParentQualifiedName(context));
		if (context instanceof Transition) {
			// Transition action and guard
			label.setType(UMLPackage.Literals.TRANSITION.getName().toLowerCase());
			label.setDetails(getTransitionDetails((Transition) context));
		} else if (context.eClass().equals(UMLPackage.Literals.OPERATION)) {
			// Class operations
			label.setType(UMLPackage.Literals.OPERATION.getName().toLowerCase());
			label.setDetails(((Operation) context).getName());
		} else if (context.eClass().equals(UMLPackage.Literals.STATE)) {
			// entry, exit action
			label.setQualifiedName(label.getQualifiedName() + QualifiedNames.SEPARATOR
					+ ((State) context).getName());
		} else if (context instanceof Trigger) {
			Transition transition = (Transition) context.eContainer();
			label.setQualifiedName(getParentQualifiedName(transition));
			String details = getTransitionDetails(transition);
			details = details.concat(">>" + getTriggerDetail((Trigger) context).getTagString());
			label.setDetails(details);
		}
		label.setUri(context.eResource().getURI().toString());

		return label;
	}

	/**
	 * Compute transition details.
	 * 
	 * @param transition
	 *            Transition.
	 * @return transition detail string
	 */
	public String getTransitionDetails(Transition transition) {
		EObject sourceVertex = getImplicitVertex(transition.getSource());
		EObject targetVertex = getImplicitVertex(transition.getTarget());

		String sourceQname = UMLEObjectLocator.getSMQualifiedName(sourceVertex);
		String targetQname = UMLEObjectLocator.getSMQualifiedName(targetVertex);
		UserEditableRegion.TransitionDetails details = new UserEditableRegion.TransitionDetails(sourceQname, targetQname);
		for (Trigger t : transition.getTriggers()) {
			TriggerDetail detail = getTriggerDetail(t);
			if (detail != null) {
				details.addTriggerDetail(detail);
			}
		}
		return details.getTagString();
	}

	/**
	 * Get trigger details.
	 * 
	 * @param trigger
	 *            trigger
	 * @return detail tag
	 */
	public TriggerDetail getTriggerDetail(Trigger trigger) {
		TriggerDetail result = null;
		List<String> ports = new ArrayList<>();
		for (Port p : trigger.getPorts()) {
			ports.add(p.getName());
		}
		Event event = trigger.getEvent();
		if (event != null && (event instanceof CallEvent || event instanceof AnyReceiveEvent)) {
			String signalName = "*";
			if (event instanceof CallEvent) {
				Operation message = ((CallEvent) event).getOperation();
				signalName = message.getName();
			}

			if (signalName != null) {
				result = new TriggerDetail(signalName, ports);
			}
		}
		return result;
	}

	/**
	 * Get vertex where entry and exit point is ignored.
	 * 
	 * @param vertex
	 *            source vertex
	 * @return vertex
	 */
	private EObject getImplicitVertex(EObject vertex) {
		if (vertex instanceof Pseudostate) {
			PseudostateKind kind = ((Pseudostate) vertex).getKind();
			if (kind == PseudostateKind.ENTRY_POINT_LITERAL || kind == PseudostateKind.EXIT_POINT_LITERAL) {
				return vertex.eContainer();
			}
		}
		return vertex;
	}

	/**
	 * Compute qualified name for container.
	 * 
	 * @param object
	 *            object to compute
	 * @return qualified name
	 */
	public String getParentQualifiedName(EObject object) {
		List<String> names = new ArrayList<>();
		EObject container = object.eContainer();
		while (container != null) {
			if (container instanceof State && StateUtils.isRTState((State) container)) {
				names.add(0, ((State) container).getName());
			} else if (container instanceof Class && !(container instanceof StateMachine)) {
				names.add(0, ((Class) container).getName());
			} else if (container instanceof org.eclipse.uml2.uml.Package) {
				names.add(0, ((org.eclipse.uml2.uml.Package) container).getName());
			}

			container = container.eContainer();
		}
		return String.join(QualifiedNames.SEPARATOR, names);
	}

	/**
	 * Get generated source file for given element.
	 * 
	 * @param element
	 *            Element.
	 * @return generated source file
	 */
	public IFile getFile(NamedElement element) {
		if (generator instanceof PapyrusUMLRT2CppCodeGenerator) {
			IProject project = ((PapyrusUMLRT2CppCodeGenerator) generator).getProject(element);
			Class clazz = null;
			EObject container = element;
			while (container != null) {
				if (container instanceof Class && !(container instanceof StateMachine)) {
					clazz = (Class) container;
					break;
				}
				container = container.eContainer();
			}
			if (clazz != null) {
				IFolder srcFolder = project.getFolder("src");
				return srcFolder.getFile(clazz.getName() + ".cc");
			}
		}
		return null;
	}

}
