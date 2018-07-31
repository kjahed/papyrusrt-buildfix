/*******************************************************************************
 * Copyright (c) 2015-2017 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.instance.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrusrt.xtumlrt.aexpr.uml.XTUMLRTBoundsEvaluator;
import org.eclipse.papyrusrt.xtumlrt.common.Port;

/**
 * An implementation of {@link IPortInstance}.
 */
public class PortInstance implements IPortInstance {

	/** The {@link ICapsuleInstance} that contains this port instance. */
	private final ICapsuleInstance container;

	/** The {@link Port} model element that this instance represents. */
	private final Port type;

	/** A {@link List} of {@link IFarEnd}s for this port instance. */
	private final List<FarEnd> farEnds;

	/** The number of unnconnected far-ends. */
	private int unconnectedFarEnds;

	/** {@code true} iff this port instance is a relay port. */
	private boolean isRelay = false;

	/**
	 * Constructor.
	 *
	 * @param container
	 *            - A {@link ICapsuleInstance}. The {@link ICapsuleInstance} that contains this port instance.
	 * @param type
	 *            - A {@link Port} type. The {@link Port} model element that this instance represents.
	 */
	public PortInstance(ICapsuleInstance container, Port type) {
		this.container = container;
		this.type = type;
		this.unconnectedFarEnds = XTUMLRTBoundsEvaluator.getUpperBound(type);
		this.farEnds = new ArrayList<>(unconnectedFarEnds);
	}

	@Override
	public ICapsuleInstance getContainer() {
		return container;
	}

	@Override
	public Port getType() {
		return type;
	}

	@Override
	public String getName() {
		return type.getName();
	}

	@Override
	public Iterable<? extends IPortInstance.IFarEnd> getFarEnds() {
		return farEnds;
	}

	@Override
	public boolean isRelay() {
		return isRelay;
	}

	@Override
	public boolean isTopLevelPort() {
		return container != null && container.getCapsulePart() == null;
	}

	/**
	 * Mark this as a release port and disconnect.
	 * 
	 * @return The current far-end.
	 */
	public FarEnd convertToRelay() {
		// TODO This flag should not be required.
		isRelay = true;

		// If a far end has already been created, then remove and disconnect
		// it from this port. If there aren't any farEnds, then try to make a new
		// one. If that fails, then this cannot become a relay.

		if (farEnds.isEmpty()) {
			if (unconnectedFarEnds <= 0) {
				throw new RuntimeException("out of port instances, cannot create relay port for " + type.getName());
			}
			return createFarEnd();
		}

		FarEnd far = farEnds.remove(0);
		far.disconnectFrom(this);
		return far;
	}

	/** @return A new {@link FarEnd} (for this port instance) if it can be created and {@code null} otherwise. */
	public FarEnd createFarEnd() {
		if (unconnectedFarEnds <= 0) {
			return null;
		}

		--unconnectedFarEnds;
		return new FarEnd(farEnds.size());
	}

	@Override
	public String toString() {
		return container.toString() + '#' + type.getName();
	}

	/**
	 * Implements {@link IFarEnd}.
	 */
	public class FarEnd implements IPortInstance.IFarEnd {

		/** The index of this far end point. */
		private final int index;

		/**
		 * Constructor.
		 *
		 * @param index
		 *            - The index of this far end point.
		 */
		public FarEnd(int index) {
			this.index = index;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public ICapsuleInstance getContainer() {
			return container;
		}

		@Override
		public Port getType() {
			return type;
		}

		@Override
		public void connectWith(IFarEnd other) {
			farEnds.add((FarEnd) other);
		}

		@Override
		public IPortInstance getOwner() {
			return PortInstance.this;
		}

		/**
		 * @param port
		 *            - A {@link IPortInstance}.
		 * @return {@code true} iff the given port instance is the same as this far-end's owner.
		 */
		private boolean isOwnedBy(IPortInstance port) {
			return port == PortInstance.this;
		}

		/**
		 * Disconnect this far-end from the given {@link PortInstance}.
		 * 
		 * @param other
		 *            - A {@link PortInstance}.
		 * @return The {@link FarEnd} on the {@code other} port instance that we are disconneting from,
		 *         or {@code null} if none of this port instance's far-ends is connected to the {@code other} port instance.
		 */
		private FarEnd disconnectFrom(PortInstance other) {
			for (FarEnd far : farEnds) {
				if (far.isOwnedBy(other)) {
					farEnds.remove(far);
					return far;
				}
			}

			return null;
		}

		@Override
		public String toString() {
			return PortInstance.this.toString() + ".far[" + index + ']';
		}
	}

}
