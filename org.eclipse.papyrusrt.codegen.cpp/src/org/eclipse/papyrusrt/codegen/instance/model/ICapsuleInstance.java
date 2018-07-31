/*******************************************************************************
 * Copyright (c) 2015-2017 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.instance.model;

import java.util.List;

import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.CapsulePart;
import org.eclipse.papyrusrt.xtumlrt.common.Port;

/**
 * Represents an instance of a {@link Capsule} model element at generation time.
 */
public interface ICapsuleInstance {

	/** @return The {@link Capsule} type of the instance. */
	Capsule getType();

	/** @return The {@link CapsulePart} of its {@link Capsule} that this instance occupies. */
	CapsulePart getCapsulePart();

	/** @return The capsule instance that contains this instance. */
	ICapsuleInstance getContainer();

	/** @return The index that this capsule instance occupies in its replicated part. */
	int getIndex();

	/**
	 * @param sep
	 *            - A {@code char} to use as separator.
	 * @return The qualified name of this instance.
	 */
	String getQualifiedName(char sep);

	/** @return The list of {@link IPortInstance}s belonging to this capsule instance. */
	List<IPortInstance> getPorts();

	/**
	 * @param port
	 *            - A {@link Port} in this instance's {@link Capsule}.
	 * @return The {@link IPortInstance} corresponding to the given {@link Port} model element.
	 */
	IPortInstance getPort(Port port);

	/** @return {@code true} iff this capsule instance is dynamic, i.e. if it is in an optional or plugin part. */
	boolean isDynamic();

	/** @return The {@link List} of capsule instances contained in this instance. */
	List<ICapsuleInstance> getContained();

	/**
	 * @param part
	 *            - A {@link CapsulePart} in this instance's {@link Capsule}.
	 * @return The {@link List} of capsule instances contained in the {@code part}.
	 */
	List<? extends ICapsuleInstance> getContained(CapsulePart part);
}

