/*******************************************************************************
 * Copyright (c) 2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *   Contributors:
 *   Young-Soo Roh - Initial API and implementation
 *   
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.build;

import java.util.stream.Stream;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * UMLRT CPP Nature.
 * 
 */
public class UMLRTCCNature extends CProjectNature {

	/**
	 * Builder ID.
	 */
	public static final String BUILDER_NAME = "org.eclipse.papyrusrt.codegen.umlrtgensrcbuilder";

	/**
	 * Nature ID.
	 */
	public static final String UMLRT_CCNATURE_ID = "org.eclipse.papyrusrt.codegen.cpp.umlrtccnature";

	/**
	 * Constructor.
	 *
	 */
	public UMLRTCCNature() {
	}


	/**
	 * Add nature to project.
	 * 
	 * @param project
	 *            project
	 * @param monitor
	 *            monitor
	 * @throws CoreException
	 */
	public static void addUMLRTCCNature(IProject project, IProgressMonitor monitor) throws CoreException {
		addNature(project, UMLRT_CCNATURE_ID, monitor);
	}

	/**
	 * Remove nature.
	 * 
	 * @param project
	 *            project
	 * @param monitor
	 *            monitor
	 * @throws CoreException
	 */
	public static void removeUMLRTCCNature(IProject project, IProgressMonitor monitor) throws CoreException {
		removeNature(project, UMLRT_CCNATURE_ID, monitor);
	}



	/**
	 * @see org.eclipse.cdt.core.CProjectNature#configure()
	 *
	 * @throws CoreException
	 */
	@Override
	public void configure() throws CoreException {
		IProject project = getProject();
		if (project == null) {
			return;
		}

		IProjectDescription desc = project.getDescription();
		boolean found = Stream.of(desc.getBuildSpec()).anyMatch(c -> c.getBuilderName().equals(BUILDER_NAME));
		if (!found) {
			ICommand cmd = desc.newCommand();
			cmd.setBuilderName(BUILDER_NAME);

			// add source gen builder before any other builder
			ICommand[] newCommands = new ICommand[desc.getBuildSpec().length + 1];
			System.arraycopy(desc.getBuildSpec(), 0, newCommands, 1, desc.getBuildSpec().length);
			newCommands[0] = cmd;
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}
	}
}
