/*******************************************************************************
 * Copyright (c) 2015-2017 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.instance.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.papyrusrt.codegen.cpp.ConnectorReporter;
import org.eclipse.papyrusrt.xtumlrt.aexpr.uml.XTUMLRTBoundsEvaluator;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.CapsuleKind;
import org.eclipse.papyrusrt.xtumlrt.common.CapsulePart;
import org.eclipse.papyrusrt.xtumlrt.common.Connector;
import org.eclipse.papyrusrt.xtumlrt.common.ConnectorEnd;
import org.eclipse.papyrusrt.xtumlrt.common.Port;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.Signal;
import org.eclipse.papyrusrt.xtumlrt.util.QualifiedNames;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTExtensions;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;

/**
 * An implementation of {@link ICapsuleInstance}, representing instances of {@link Capsule} model elements
 * at generation time.
 */
public class CapsuleInstance implements ICapsuleInstance {

	/** The {@link Capsule} type of the instance. */
	private final Capsule type;

	/** The {@link CapsulePart} of its {@link Capsule} that this instance occupies. */
	private final CapsulePart part;

	/** The capsule instance that contains this instance. */
	private final ICapsuleInstance container;

	/** The index that this capsule instance occupies in its replicated part. */
	private final Integer index;

	/** {@code true} iff this capsule instance is dynamic, i.e. if it is in an optional or plugin part. */
	private final boolean dynamic;

	/** A {@link Map} from {@link CapsulePart}s to the {@link List} of capsule instances contained by this instance. */
	private final Map<CapsulePart, List<CapsuleInstance>> contained = new TreeMap<>(new CapsulePartComparator());

	/** A {@link Map} from {@link Port}s in this instance's {@link Capsule} to {@link PortInstance}s. */
	private final Map<Port, PortInstance> ports = new LinkedHashMap<>();

	/**
	 * Create the top-level capsule instance.
	 * 
	 * @param top
	 *            - The {@link Capsule} type to use as top capsule.
	 */
	public CapsuleInstance(Capsule top) {
		this.type = top;
		this.part = null;
		this.container = null;
		this.index = null;
		this.dynamic = false;

		constructPortInstances();
		constructCapsuleInstances();
	}

	/**
	 * Create a normal capsule instance.
	 * 
	 * @param container
	 *            - The capsule instance that contains this instance.
	 * @param index
	 *            - The index that this instance occupies in its part.
	 * @param part
	 *            - The {@link CapsulePart} where this instance is instantiated.
	 * @param dynamic
	 *            - Whether this instance occupies an optional or plugin part.
	 */
	private CapsuleInstance(ICapsuleInstance container, Integer index, CapsulePart part, boolean dynamic) {
		this.type = part.getType();
		this.part = part;
		this.container = container;
		this.index = index;
		this.dynamic = dynamic;

		constructPortInstances();
		constructCapsuleInstances();
	}

	@Override
	public Capsule getType() {
		return type;
	}

	@Override
	public CapsulePart getCapsulePart() {
		return part;
	}

	@Override
	public ICapsuleInstance getContainer() {
		return container;
	}

	@Override
	public int getIndex() {
		return index == null ? 0 : index.intValue();
	}

	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public List<? extends ICapsuleInstance> getContained(CapsulePart part) {
		return contained.get(part);
	}

	@Override
	public List<IPortInstance> getPorts() {
		return new ArrayList<>(ports.values());
	}

	@Override
	public IPortInstance getPort(Port port) {
		return ports.get(port);
	}

	/**
	 * Creates {@link PortInstance}s for each {@link Port} element in this instance's {@link Capsule}.
	 */
	private void constructPortInstances() {
		for (Port port : XTUMLRTExtensions.getAllRTPorts(type)) {
			ports.put(port, new PortInstance(this, port));
		}
	}

	/**
	 * Creates {@link CapsuleInstance}s for each {@link CapsulePart} element in this instance's {@link Capsule}.
	 */
	private void constructCapsuleInstances() {
		for (CapsulePart part : XTUMLRTExtensions.getAllCapsuleParts(type)) {
			List<CapsuleInstance> instances = new ArrayList<>();

			int lower = XTUMLRTBoundsEvaluator.getLowerBound(part);
			int upper = XTUMLRTBoundsEvaluator.getUpperBound(part);
			final CapsuleKind kind = part.getKind();
			// Bug 515855: if this capsule instance is already dynamic (it is in an optional or plugin part)
			// then all its children must be dynamic as well.
			boolean dynamicSubcapsuleInstance = lower <= 0
					|| isDynamic()
					|| kind == CapsuleKind.OPTIONAL
					|| kind == CapsuleKind.PLUGIN;
			if (upper == 1) {
				instances.add(new CapsuleInstance(this, null, part, dynamicSubcapsuleInstance));
			} else {
				for (int i = 0; i < upper; ++i) {
					if (i == lower) {
						// EPP: This is strange: why mark only one capsule instance as dynamic? Andrew should explain.
						dynamicSubcapsuleInstance = true;
					}
					instances.add(new CapsuleInstance(this, i, part, dynamicSubcapsuleInstance));
				}
			}

			contained.put(part, instances);
		}
	}

	/**
	 * Create connections between the contained capsule instances according to the {@link Capsule}'s structure.
	 * 
	 * @param connReporter
	 *            - A {@link ConnectorReporter}.
	 * @param shallow
	 *            - {@code true} if it should create only the connections for this instance,
	 *            but not recursively create connections of the contained instances.
	 */
	public void connect(ConnectorReporter connReporter, boolean shallow) {
		// Connect all of the instances for this capsule's parts.
		for (Connector connector : XTUMLRTExtensions.getAllConnectors(type)) {
			connect(connReporter, connector);
		}

		// Now connect all of the contained capsules.
		if (!shallow) {
			for (List<CapsuleInstance> capsuleInstances : contained.values()) {
				for (CapsuleInstance capsuleInstance : capsuleInstances) {
					if (!capsuleInstance.isDynamic()) {
						capsuleInstance.connect(connReporter == null ? null : connReporter.createInner(capsuleInstance), false);
					}
				}
			}
		}
	}

	/**
	 * @param part
	 *            - A {@link CapsulePart} in this instance's {@link Capsule}.
	 * @return An {@link Iterable} to the capsule instances in the given part.
	 */
	private Iterable<CapsuleInstance> getInstancesFor(CapsulePart part) {
		Iterable<CapsuleInstance> instances = contained.get(part);
		return instances == null ? java.util.Collections.singletonList(this) : instances;
	}

	/**
	 * @param port0
	 *            - A {@link Port}.
	 * @param port1
	 *            - A {@link Port}.
	 * @return {@code true} iff the ports have the same conjugation.
	 */
	// Bug 242: Ports with the same conjugation are compatible if their protocols both have only
	// symmetric signals.
	private static boolean isSameConjugationCompatible(Port port0, Port port1) {
		Protocol t0 = port0.getType();
		if (t0 == null) {
			throw new RuntimeException("invalid attempt to generate ports " + QualifiedNames.fullName(port0) + " without Protocol");
		}

		Protocol t1 = port1.getType();
		if (t1 == null) {
			throw new RuntimeException("invalid attempt to generate ports " + QualifiedNames.fullName(port1) + " without Protocol");
		}

		Iterator<Signal> outSignals0 = XTUMLRTUtil.getOutSignals(t0).iterator();
		if (outSignals0.hasNext()) {
			return false;
		}

		Iterator<Signal> outSignals1 = XTUMLRTUtil.getOutSignals(t1).iterator();
		if (outSignals1.hasNext()) {
			return false;
		}

		return true;
	}

	/**
	 * Create the connections corresponding to the given {@code connector} in this capsule instance.
	 * 
	 * @param connReporter
	 *            - A {@link ConnectorReporter}.
	 * @param connector
	 *            - A {@link Connector} element in the model.
	 */
	private void connect(ConnectorReporter connReporter, Connector connector) {
		ConnectorEnd[] ends = connector.getEnds().toArray(new ConnectorEnd[2]);
		if (ends.length != 2) {
			return;
		}

		ConnectorBuilder cb = new ConnectorBuilder(new End(part, ends[0]), new End(part, ends[1]));

		// Decide whether each port instance is a relay of another port so that
		// it can be deleted.
		//
		// To determine this, one of the conditions are possible:
		// a) The protocol is not symmetric, both ports have the same
		// conjugation and exactly one of the ports is owned by the
		// capsule instance's part,
		// b) The protocol is not symmetric, the ports have opposite
		// conjugation and both ports are owned by the same part
		// c) The protocol is symmetric, both ports have the same conjugation,
		//
		// If one of these conditions applies, then the port(s) of the part
		// is(are) a relay for whichever port it is connected to. This means
		// this port instance is deleted and it's farEnd is used for the
		// connection.
		//
		// If none of these conditions apply, the ports remain and are connected.
		boolean isRelay0 = false;
		boolean isRelay1 = false;

		if (!dynamic && cb.primary.part == part && XTUMLRTUtil.isBorderPort(cb.primary.port)) {
			isRelay0 = ports.containsKey(cb.primary.port);
		}
		if (!dynamic && cb.secondary.part == part && XTUMLRTUtil.isBorderPort(cb.secondary.port)) {
			isRelay1 = ports.containsKey(cb.secondary.port);
		}

		int perPrimaryRole = cb.secondary.numPortInstances / cb.primary.numParts;
		Iterator<CapsuleInstance> secondaryCapsuleIterator = getInstancesFor(cb.secondary.part).iterator();
		CapsuleInstance secondaryCapsule = secondaryCapsuleIterator.hasNext() ? secondaryCapsuleIterator.next() : null;
		for (CapsuleInstance cap0 : getInstancesFor(cb.primary.part)) {
			for (int i = 0; secondaryCapsule != null && i < perPrimaryRole; ++i) {
				PortInstance.FarEnd farEnd0 = cap0.createFarEnd(cb.primary.port, isRelay0);
				PortInstance.FarEnd farEnd1 = secondaryCapsule.createFarEnd(cb.secondary.port, isRelay1);

				// If all far ends have been consumed, then advance to the next the secondary capsule instance.
				if (farEnd1 == null) {
					// Earlier checks confirm that there should be enough instances.
					if (!secondaryCapsuleIterator.hasNext()) {
						throw new RuntimeException("not enough secondary capsule instances to connect " + cb.primary.toString() + " and " + cb.secondary.toString() + " with " + connector.getName());
					}

					secondaryCapsule = secondaryCapsuleIterator.next();
					farEnd1 = secondaryCapsule.createFarEnd(cb.secondary.port, isRelay1);
				}

				farEnd0.connectWith(farEnd1);
				farEnd1.connectWith(farEnd0);

				if (connReporter != null) {
					connReporter.record(connector, farEnd0, farEnd1);
				}
			}
		}
	}

	/**
	 * Create a {@link PortInstance.FarEnd} for the given {@link Port} model element.
	 * 
	 * @param modelPort
	 *            - A {@link Port}.
	 * @param isRelay
	 *            - Whether the port is a relay port.
	 * @return A {@link PortInstance.FarEnd}.
	 */
	private PortInstance.FarEnd createFarEnd(Port modelPort, boolean isRelay) {
		PortInstance portInstance = ports.get(modelPort);
		if (portInstance == null) {
			return null;
		}

		PortInstance.FarEnd far = null;

		// If we need a relay port and the existing instance can be converted to a
		// relay then do so, but only if the port instance is not a top-level
		// port, since a top-level port cannot be a relay for a port outside
		// the capsule (since the capsule is being considered in isolation,
		// there is no "outside" to be connected to).
		// This last condition is important: if we convert to relay top-level
		// ports, then we end up with an incorrect model. For example, if we are
		// considering a capsule B in isolation and we are looking at a top-level
		// relay port B#q connected to some capsule parts B.c and B.d, e.g. with
		// connectors y : B#q <-> B.c#r and z : B#q <-> B.d#s, and if we convert
		// B#q to a relay (invoking convertToRelay) then the connection
		// B#q.far[0] <-> B.c#r.far[0] is removed and a connection
		// B.c#r.far[0] <-> B.d#s.far[0] is added, which is incorrect.
		// (see Bug 489055, models/tests/RelayPortFanOut)
		if (isRelay && !portInstance.isTopLevelPort()) {
			far = portInstance.convertToRelay();
		}

		// Otherwise either we don't want a relay, or the existing instance cannot be
		// converted. In both cases try to create a new end.
		return far == null ? portInstance.createFarEnd() : far;
	}

	@Override
	public String getQualifiedName(char sep) {
		String base = null;
		if (part != null) {
			base = part.getName();
		} else {
			// NOTE: The previous instance model did not have lower-case char, so the new one
			// cannot either. The problem is the strings in the allocations file.
			String name = type.getName();
			if (name == null || name.isEmpty()) {
				base = "Top";
			}
			// else if( Character.isLowerCase( name.charAt( 0 ) ) )
			base = name;
			// else
			// base = Character.toLowerCase( name.charAt( 0 ) )
			// + ( name.length() > 1 ? name.substring( 1 ) : "" );
		}

		if (container != null) {
			base = container.getQualifiedName(sep) + sep + base;
		}
		if (index == null) {
			return base;
		}

		switch (sep) {
		case '.':
			return base + '[' + index + ']';
		case '_':
		default:
			return base + sep + index;
		}
	}

	@Override
	public List<ICapsuleInstance> getContained() {
		List<ICapsuleInstance> list = new ArrayList<>();
		for (List<CapsuleInstance> capsules : contained.values()) {
			list.addAll(capsules);
		}
		return list;
	}

	@Override
	public String toString() {
		return getQualifiedName('.');
	}

	/**
	 * A {@link Comparator} of {@link CapsulePart}s.
	 * 
	 * This is used to store the contained parts sorted by name to ensure a consistent ordering
	 * between invocations.
	 * 
	 * @see Bug 475980
	 */
	private static class CapsulePartComparator implements Comparator<CapsulePart> {

		/** Constructor. */
		CapsulePartComparator() {
		}

		@Override
		public int compare(CapsulePart o1, CapsulePart o2) {
			// null sorts earlier
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			if (o2 == null) {
				return 1;
			}

			String n1 = o1.getName();
			String n2 = o2.getName();

			// null sorts earlier
			if (n1 == null) {
				return n2 == null ? 0 : -1;
			}
			if (n2 == null) {
				return 1;
			}

			return n1.compareTo(n2);
		}
	}

	/**
	 * Represents a {@link ConnectorEnd}.
	 */
	public static class End {

		/** The {@link CapsulePart} of the connected port-role. */
		public final CapsulePart part;

		/** The {@link Port} element for the connected port-role. */
		public final Port port;

		/** The number of parts. */
		public final int numParts;

		/** The number of port instances on this End. */
		public final int numPortInstances;

		/**
		 * Constructor.
		 *
		 * @param containingPart
		 *            - The {@link CapsulePart} of the connected port-role.
		 * @param connectorEnd
		 *            - The {@link Port} element for the connected port-role.
		 */
		public End(CapsulePart containingPart, ConnectorEnd connectorEnd) {
			CapsulePart p = connectorEnd.getPartWithPort();

			this.part = p == null ? containingPart : p;
			this.port = connectorEnd.getRole();
			this.numParts = part == null ? 1 : XTUMLRTBoundsEvaluator.getUpperBound(part);
			this.numPortInstances = numParts * XTUMLRTBoundsEvaluator.getUpperBound(port);
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(part == null ? "<no-part>" : part.getName());
			str.append('.');
			str.append(port.getName());
			str.append("{numPortInstances:");
			str.append(numPortInstances);
			str.append(", numParts:");
			str.append(numParts);
			str.append('}');
			return str.toString();
		}
	}

	/**
	 * A class to record the {@link End}s of a connector.
	 */
	private static class ConnectorBuilder {

		/** Primary {@link End} of the connector, i.e. the one that has the most port instances. */
		public final End primary;

		/** Secondary {@link End} of the connector. */
		public final End secondary;

		/**
		 * Constructor. Determines which end is "primary" based on the number of port instances
		 * for each end. The primary end is the one with the most port instances.
		 *
		 * @param end0
		 *            - A connector {@link End}.
		 * @param end1
		 *            - A connector {@link End}.
		 */
		ConnectorBuilder(End end0, End end1) {
			// The end with the most actual port instances is the primary side.
			if (end0.numPortInstances >= end1.numPortInstances) {
				this.primary = end0;
				this.secondary = end1;
			} else {
				this.primary = end1;
				this.secondary = end0;
			}
		}
	}

}
