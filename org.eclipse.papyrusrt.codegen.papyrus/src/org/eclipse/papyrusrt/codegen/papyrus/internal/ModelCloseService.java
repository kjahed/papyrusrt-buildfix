/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.papyrus.internal;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.papyrus.infra.core.resource.ModelSet;
import org.eclipse.papyrus.infra.core.services.IService;
import org.eclipse.papyrus.infra.core.services.ServiceException;
import org.eclipse.papyrus.infra.core.services.ServicesRegistry;
import org.eclipse.papyrus.infra.ui.editor.IMultiDiagramEditor;
import org.eclipse.papyrus.infra.ui.services.EditorLifecycleEventListener;
import org.eclipse.papyrus.infra.ui.services.EditorLifecycleManager;
import org.eclipse.papyrusrt.codegen.cpp.ChangeTracker;
import org.eclipse.papyrusrt.codegen.cpp.UMLChangeTracker;

/**
 * Service to handle the closing of a model to ensure that the change-tracker stops recording changes
 * and closes the resource.
 * 
 * @author epp
 */
public class ModelCloseService implements IService {

	/** Services registry. */
	private ServicesRegistry registry;

	/**
	 * Constructor.
	 */
	public ModelCloseService() {
	}

	@Override
	public void disposeService() throws ServiceException {
	}

	@Override
	public void init(ServicesRegistry registry) throws ServiceException {
		this.registry = registry;
	}

	@Override
	public void startService() throws ServiceException {
		// Get the ModelSet service from the ServicesRegistry
		ModelSet modelSet = registry.getService(ModelSet.class);
		TransactionalEditingDomain editingDomain = modelSet
				.getTransactionalEditingDomain();
		EditorLifecycleManager lifecycleManager = registry
				.getService(EditorLifecycleManager.class);
		lifecycleManager
				.addEditorLifecycleEventsListener(new EditorCycleListener(
						editingDomain));
	}

	/**
	 * An internal listener for life cycle events of Papyrus.
	 */
	protected static class EditorCycleListener implements
			EditorLifecycleEventListener {

		/** The Papyrus editing domain. */
		private TransactionalEditingDomain domain;

		/**
		 * Constructor.
		 *
		 * @param domain
		 *            - The Papyrus editing domain.
		 */
		EditorCycleListener(TransactionalEditingDomain domain) {
			this.domain = domain;
		}

		@Override
		public void postInit(IMultiDiagramEditor editor) {
		}

		@Override
		public void postDisplay(IMultiDiagramEditor editor) {
		}

		/**
		 * Executed before an editor will close => stop recording for this
		 * editing domain.
		 */
		@Override
		public void beforeClose(IMultiDiagramEditor editor) {
			for (Resource res : domain.getResourceSet().getResources()) {
				ChangeTracker activeInstance = UMLChangeTracker.getActiveInstance();
				if (activeInstance != null) {
					activeInstance.closeResource(res);
				}
			}
		}
	}
}
