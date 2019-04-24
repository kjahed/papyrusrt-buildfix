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

package org.eclipse.papyrusrt.codegen.papyrus.actionprovider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.papyrus.views.modelexplorer.actionprovider.AbstractCommonActionProvider;
import org.eclipse.papyrusrt.codegen.papyrus.handlers.EditSourceAction;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * @author ysroh
 *
 */
public class EditSourceActionProvider extends AbstractCommonActionProvider {


	/**
	 * 
	 */
	public static final String EDIT_CODE_MENU = "Edit Code";
	/**
	 * 
	 */
	public static final String ENTRY_CODE_MENU = "Entry Code";
	/**
	 * 
	 */
	public static final String EXIT_CODE_MENU = "Exit Code";
	/**
	 * 
	 */
	public static final String TRANSITION_EFFECT_CODE_MENU = "Transition Effect Code";
	/**
	 * 
	 */
	public static final String TRANSITION_GUARD_CODE_MENU = "Transition Guard Code";
	/**
	 * 
	 */
	public static final String TRIGGER_GUARD_CODE_MENU = "Edit Trigger Guard Code";


	/**
	 * Constructor.
	 *
	 */
	public EditSourceActionProvider() {
	}

	/**
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 *
	 * @param menu
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		ISelection selection = getContext().getSelection();
		EObject selectedEObject = null;
		if (selection instanceof IStructuredSelection
				&& ((IStructuredSelection) selection).size() > 0) {
			Object selectedElement = resolveSemanticObject(((IStructuredSelection) selection)
					.getFirstElement());
			if (selectedElement != null && selectedElement instanceof EObject) {
				selectedEObject = (EObject) selectedElement;
			}
		}

		if (selectedEObject == null) {
			return;
		}
		List<EditSourceAction> actions = createActions(selectedEObject);

		if (actions.size() == 1) {
			if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) != null) {
				menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, actions.get(0));
			} else {
				menu.add(actions.get(0));
			}
		} else if (actions.size() >= 2) {
			IMenuManager editSourceMenu;
			if (menu.find(IWorkbenchActionConstants.MB_ADDITIONS) == null) {
				editSourceMenu = menu;
			} else {
				editSourceMenu = new MenuManager(EDIT_CODE_MENU);
				menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, editSourceMenu);
			}

			for (int i = 0; i < actions.size(); ++i) {
				editSourceMenu.add(actions.get(i));
			}
		}
	}

	/**
	 * Create actions.
	 * 
	 * @param context
	 *            selected element
	 * @return actions
	 */
	private List<EditSourceAction> createActions(EObject context) {
		List<EditSourceAction> actions = new ArrayList<>();

		if (context.eClass() == UMLPackage.Literals.CLASS) {
			EditSourceAction action = new EditSourceAction(context, EDIT_CODE_MENU);
			actions.add(action);
		} else if (context.eClass() == UMLPackage.Literals.OPERATION) {
			EditSourceAction action = new EditSourceAction(context, EDIT_CODE_MENU);
			actions.add(action);
		} else if (context instanceof State) {
			EditSourceAction entryAction = new EditSourceAction(context, ENTRY_CODE_MENU);
			actions.add(entryAction);
			EditSourceAction exitAction = new EditSourceAction(context, EXIT_CODE_MENU);
			actions.add(exitAction);
		} else if (context instanceof Transition) {
			EditSourceAction transitionAction = new EditSourceAction(context, TRANSITION_EFFECT_CODE_MENU);
			actions.add(transitionAction);
			EditSourceAction guardAction = new EditSourceAction(context, TRANSITION_GUARD_CODE_MENU);
			actions.add(guardAction);
		} else if (context instanceof Trigger) {
			EditSourceAction guardAction = new EditSourceAction(context, TRIGGER_GUARD_CODE_MENU);
			actions.add(guardAction);
		}

		return actions;
	}

}
