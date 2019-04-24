/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.papyrus;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.AbstractUMLRT2CppCodeGenerator;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator;
import org.eclipse.papyrusrt.codegen.cpp.build.ProjectGenerator;
import org.eclipse.papyrusrt.codegen.utils.ProjectUtils;

import com.google.inject.Inject;

/**
 * This class extends the {@link AbstractUMLRT2CppCodeGenerator} by providing methods that access the input model,
 * and output folder and generate the target project in the context of an Eclipse instance.
 * 
 * @author epp
 */
public final class PapyrusUMLRT2CppCodeGenerator extends AbstractUMLRT2CppCodeGenerator {

	/**
	 * Default Constructor. Private to avoid creating multiple instances.
	 */
	@Inject
	private PapyrusUMLRT2CppCodeGenerator() {
		super();
		setStandalone(false);
	}

	@Override
	public IStatus generate(List<EObject> elements, String top, boolean uml) {

		IStatus status = super.generate(elements, top, uml);

		Set<IProject> projects = new HashSet<>();
		for (EObject e : elements) {
			IProject p = getProject(e);
			if (p != null) {
				projects.add(p);
			}
		}

		// Refresh generated projects
		for (IProject p : projects) {
			try {
				p.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				// ignore
			}
		}

		return status;
	}

	@Override
	protected File getModelFolder(EObject eobj) {
		File modelFolder = null;
		if (eobj != null) {

			URI eobjUri = EcoreUtil.getURI(eobj);
			if (eobjUri != null) {

				IPath path = new org.eclipse.core.runtime.Path(eobjUri.toPlatformString(true));
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (file != null && file.exists()) {

					IContainer modelContainer = file.getParent();
					if (modelContainer != null && modelContainer.exists()) {
						modelFolder = modelContainer.getLocation().makeAbsolute().toFile();
					}
				}
			}
		}
		return modelFolder;
	}

	/**
	 * Obtains the folder where code for the model that owns the given object should be generated.
	 * 
	 * @param eobj
	 *            - An {@link EObject}, part of some UML-RT model.
	 * @return A {@link File} with the output folder.
	 */
	public File getOutputFolder(EObject eobj) {
		return getOutputFolder(eobj, null);
	}

	@Override
	protected File getOutputFolder(EObject eobj, XTUMLRT2CppCodeGenerator codeGen) {
		IProject project = getProject(eobj, codeGen);

		// Bug 109: Refresh the folder before checking if it exists.
		IFolder folder = project.getFolder("src");
		try {
			folder.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e) {
			CodeGenPlugin.error("could not refresh output folder", e);
		}

		if (!folder.exists()) {
			try {
				folder.create(true, true, null);
			} catch (CoreException e) {
				CodeGenPlugin.error("could not create output folder", e);
			}
		}

		return folder.getRawLocation().toFile();
	}

	/**
	 * Obtains the CDT project for the generated code for the model owning the given element.
	 * 
	 * If the project doesn't exit, create a new one.
	 * 
	 * @param eobj
	 *            - An {@link EObject}, part of some UML-RT model.
	 * @return An {@link IProject} with the CDT project.
	 */
	public IProject getProject(EObject eobj) {
		return getProject(eobj, null);
	}

	/**
	 * Obtains the CDT project for the generated code for the model owning the given element.
	 * 
	 * If the project doesn't exit, create a new one.
	 * 
	 * @param eobj
	 *            - An {@link EObject}, part of some UML-RT model.
	 * @param codeGen
	 *            - The {@link XTUMLRT2CppCodeGenerator}.
	 * @return An {@link IProject} with the CDT project.
	 */
	public IProject getProject(EObject eobj, XTUMLRT2CppCodeGenerator codeGen) {
		String projectName = ProjectUtils.getProjectName(eobj);
		projectName = projectName + "_CDTProject";

		IProject project = ProjectGenerator.getOrCreateCPPProject(projectName, null);

		return project;
	}
}
