/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.papyrus.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.papyrus.infra.emf.utils.EMFHelper;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.UMLRTProfileUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.uml2.uml.Class;

/**
 * Handler for the "Generate As Top" command.
 * 
 * @author epp
 */
public class GenerateAsTopActionHandler extends UMLRTCppCodeGen {

	/**
	 * Constructor.
	 */
	public GenerateAsTopActionHandler() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Object result = null;

		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {

			IStructuredSelection selection = (IStructuredSelection) sel;
			Object obj = selection.getFirstElement();
			final EObject eobj = EMFHelper.getEObject(obj);
			if (eobj == null) {
				throw new IllegalArgumentException(obj.getClass().getCanonicalName() + " is not an EObject");
			}

			if (eobj instanceof Class) {
				if (UMLRTProfileUtil.isCapsule((Class) eobj)) {

					List<EObject> targets = new ArrayList<>();

					EObject eobjRoot = EcoreUtil.getRootContainer(eobj);
					targets.add(eobjRoot);

					final IStatus status = generator.generate(targets, ((Class) eobj).getLabel(), true);
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							ErrorDialog.openError(Display.getCurrent().getActiveShell(), "UML-RT Code Generator", null,
									status);
						}
					});
				}
			}
		}

		return result;
	}

}
