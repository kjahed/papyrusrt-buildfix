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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.papyrus.infra.emf.utils.EMFHelper;
import org.eclipse.papyrusrt.codegen.UMLRTCodeGenerator;
import org.eclipse.papyrusrt.codegen.config.CodeGenProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.uml2.uml.Element;

/**
 * Handler for the code generation command.
 * 
 * @author epp
 */
public class UMLRTCppCodeGen extends AbstractHandler {

	/** The code generator. */
	protected UMLRTCodeGenerator generator = CodeGenProvider.getDefault().get();

	/**
	 * Constructor.
	 *
	 */
	public UMLRTCppCodeGen() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object result = null;
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {

			List<EObject> targets = new ArrayList<>();

			IStructuredSelection selection = (IStructuredSelection) sel;
			if (!selection.isEmpty()) {
				EObject eobj = EMFHelper.getEObject(selection.getFirstElement());
				if (eobj != null && eobj instanceof Element) {

					EObject eobjRoot = EcoreUtil.getRootContainer(eobj);
					Element element = (Element) eobjRoot;

					String topCapsuleName = getTopCapsuleName(element);
					targets.add(eobjRoot);

					final IStatus status = generator.generate(targets, topCapsuleName, true);
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							ErrorDialog.openError(Display.getCurrent().getActiveShell(), "UML-RT Code Generator", null, status);
						}
					});

				}
			}
		}

		return result;
	}

	/**
	 * Obtains the name of the Top capsule.
	 * 
	 * @param root
	 *            - The model's root {@link Element}.
	 * @return The name of the Top capsule.
	 */
	public static String getTopCapsuleName(Element root) {
		String retVal = null;

		EAnnotation anno = root.getEAnnotation(SetDefaultTopActionHandler.DEFAULT_TOP_ANNO);
		if (anno != null) {
			retVal = anno.getDetails().get(SetDefaultTopActionHandler.DEFAULT_TOP_KEY);
		}

		return retVal != null ? retVal : "Top";
	}
}
