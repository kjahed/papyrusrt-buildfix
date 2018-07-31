/*******************************************************************************
 * Copyright (c) 2015-2017 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.instance.model;

import org.eclipse.papyrusrt.xtumlrt.common.Port;

/**
 * Represents an instance of a {@link Port} model element at generation time.
 */
public interface IPortInstance {

	/** @return The {@link ICapsuleInstance} that contains this port instance. */
	ICapsuleInstance getContainer();

	/** @return The {@link Port} model element that this instance represents. */
	Port getType();

	/** @return An {@link Iterable} of {@link IFarEnd}s for this port instance. */
	Iterable<? extends IPortInstance.IFarEnd> getFarEnds();

	/** @return The name of this port instance. */
	String getName();

	/** @return {@code true} iff this port instance is a relay port. */
	boolean isRelay();

	/** @return {@code true} iff this port is a top-level port. */
	boolean isTopLevelPort();

	/**
	 * Represents the "point of connection" where connectors end in this port instance,
	 * this is, it is the "far-end" of the port at the other end of some connector.
	 */
	interface IFarEnd {

		/** @return The index of this far end point. */
		int getIndex();

		/** @return The {@link ICapsuleInstance} that contains this port instance. */
		ICapsuleInstance getContainer();

		/** @return The {@link Port} model element that this instance represents. */
		Port getType();

		/** @return the {@link IPortInstance} that owns this far-end. */
		IPortInstance getOwner();

		/**
		 * Creates a connection with another far-end (the end of a connector).
		 * 
		 * @param other
		 *            - Another {@link IFarEnd}.
		 */
		void connectWith(IFarEnd other);
	}

}
