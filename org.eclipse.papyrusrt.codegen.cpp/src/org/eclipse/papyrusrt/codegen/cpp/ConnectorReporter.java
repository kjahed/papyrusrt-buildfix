/*******************************************************************************
 * Copyright (c) 2015-2017 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.papyrusrt.codegen.instance.model.ICapsuleInstance;
import org.eclipse.papyrusrt.codegen.instance.model.PortInstance;
import org.eclipse.papyrusrt.xtumlrt.common.Connector;

/**
 * This utility class is used to report on connections that are allocated while
 * generating the InstanceModel.
 */
public class ConnectorReporter {

	/** The {@link ICapsuleInstance} being examined. */
	private final ICapsuleInstance capsule;

	/** A map from model-level {@link Connector}s to the list of {@link Connection} instances that it represents. */
	private final Map<Connector, List<Connection>> connections = new LinkedHashMap<>();

	/** List of reporters for the sub-capsule instances. */
	private final List<ConnectorReporter> inners = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param capsule
	 *            - The {@link ICapsuleInstance} being examined.
	 */
	public ConnectorReporter(ICapsuleInstance capsule) {
		this.capsule = capsule;
	}

	/**
	 * Adds a new {@link Connection} between the given {@link PortInstance.FarEnd}s to the list
	 * of connections of the given {@link Connector}.
	 * 
	 * @param conn
	 *            - A {@link Connector}.
	 * @param far0
	 *            - First {@link PortInstance.FarEnd}.
	 * @param far1
	 *            - Second {@link PortInstance.FarEnd}.
	 */
	public void record(Connector conn, PortInstance.FarEnd far0, PortInstance.FarEnd far1) {
		List<Connection> conns = connections.get(conn);
		if (conns == null) {
			conns = new ArrayList<>();
			connections.put(conn, conns);
		}

		conns.add(new Connection(far0, far1));
	}

	/**
	 * Creates a {@link ConnectionReporter} for a given sub-{@link ICapsuleInstance} and
	 * adds it to the list of inner reporters.
	 * 
	 * @param sub
	 *            - A {@link ICapsuleInstance}.
	 * @return The new inner {@link ConnectionReporter} for the sub-capsule instance.
	 */
	public ConnectorReporter createInner(ICapsuleInstance sub) {
		ConnectorReporter inner = new ConnectorReporter(sub);
		inners.add(inner);
		return inner;
	}

	/**
	 * Logs all connections to the given {@link PrintStream}.
	 * 
	 * @param stm
	 *            - A {@link PrintStream}.
	 */
	public void log(PrintStream stm) {
		for (Map.Entry<Connector, List<Connection>> entry : connections.entrySet()) {
			stm.append(capsule.getQualifiedName('.'));
			stm.append('.');
			stm.append(entry.getKey().getName());
			stm.append('\n');
			for (Connection conn : entry.getValue()) {
				stm.append("    ");
				stm.append(conn.far0.toString());
				stm.append(" <-> ");
				stm.append(conn.far1.toString());
				stm.append('\n');
			}
		}

		for (ConnectorReporter inner : inners) {
			inner.log(stm);
		}
	}

	/**
	 * Logs all connections to a file called "<capsule-name>-connections.log" for the capsule of this
	 * reporter's capsule instance. This file is saved in the given output folder.
	 * 
	 * @param outFolder
	 *            - A {@link File} handler for the output folder.
	 */
	public void log(File outFolder) {
		PrintStream stm = null;
		try {
			File file = new File(outFolder, capsule.getType().getName() + "-connections.log");
			stm = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
			log(stm);
		} catch (IOException e) {
		} finally {
			if (stm != null) {
				stm.close();
			}
		}
	}

	/**
	 * A model-level {@link Connector} represents one or more {@link Connection}s depending on the
	 * replication of the ports linked by the {@code Connector}.
	 */
	private static class Connection {

		/** First {@link PortInstance.FarEnd} of the connection. */
		public final PortInstance.FarEnd far0;

		/** Second {@link PortInstance.FarEnd} of the connection. */
		public final PortInstance.FarEnd far1;

		/**
		 * Constructor.
		 *
		 * @param far0
		 *            - First {@link PortInstance.FarEnd} of the connection.
		 * @param far1
		 *            - Second {@link PortInstance.FarEnd} of the connection.
		 */
		Connection(PortInstance.FarEnd far0, PortInstance.FarEnd far1) {
			this.far0 = far0;
			this.far1 = far1;
		}
	}


}
