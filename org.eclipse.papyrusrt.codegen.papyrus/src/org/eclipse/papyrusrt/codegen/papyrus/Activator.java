/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.papyrus;

import org.eclipse.papyrus.infra.core.log.LogHelper;
import org.eclipse.papyrusrt.codegen.UMLRTCodeGenerator;
import org.eclipse.papyrusrt.codegen.config.CodeGenProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = "org.eclipse.papyrusrt.codegen.papyrus";

	/** The shared instance. */
	private static Activator plugin;

	/** The shared instance of the code generator. */
	private static UMLRTCodeGenerator generator;

	/** Log helper. */
	private static LogHelper logHelper;

	/**
	 * Constructor.
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Register the login helper
		setLogHelper(new LogHelper(plugin));

		CodeGenProvider.getDefault().setModule(new PapyrusUMLRTCodeGenInjectionModule());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return The shared instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @return the logHelper
	 */
	public static LogHelper getLogHelper() {
		return logHelper;
	}

	/**
	 * @param logHelper
	 *            the logHelper to set
	 */
	public static void setLogHelper(LogHelper logHelper) {
		Activator.logHelper = logHelper;
	}

}
