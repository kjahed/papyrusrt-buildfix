/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.papyrus.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.papyrus.infra.emf.utils.EMFHelper;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.UMLRTProfileUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;

/**
 * Handler for the "set default top capsule" command.
 * 
 * @author epp
 */
public class SetDefaultTopActionHandler extends AbstractHandler {

	/** Default top EAnnotation. */
	public static final String DEFAULT_TOP_ANNO = "UMLRT_Default_top";
	/** Default top key. */
	public static final String DEFAULT_TOP_KEY = "top_name";

	/**
	 * Constructor.
	 */
	public SetDefaultTopActionHandler() {
	}

	@Override
	protected void setBaseEnabled(boolean state) {
		super.setBaseEnabled(state);
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

					EObject eobjRoot = EcoreUtil.getRootContainer(eobj);
					final Element root = (Element) eobjRoot;

					TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(eobj);
					RecordingCommand command = new RecordingCommand(domain, "Set Default Top") {
						@Override
						protected void doExecute() {
							EAnnotation anno = root.getEAnnotation(DEFAULT_TOP_ANNO);
							if (anno == null) {
								anno = root.createEAnnotation(DEFAULT_TOP_ANNO);
							}
							anno.getDetails().put(DEFAULT_TOP_KEY, ((Class) eobj).getLabel());
						}
					};
					domain.getCommandStack().execute(command);
				}
			}
		}

		return result;
	}

}
