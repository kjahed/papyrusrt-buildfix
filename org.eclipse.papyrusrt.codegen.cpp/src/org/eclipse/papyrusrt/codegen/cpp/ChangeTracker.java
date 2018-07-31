/*******************************************************************************
* Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.papyrus.designer.languages.common.base.codesync.ChangeObject;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator.GeneratorKey;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator.Kind;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;

/**
 * A ChangeTracker records changes that have been made to a model, elements for which code has already been
 * generated, and changed elements. It prunes the collection of element generators so that only generators for
 * elements that have not been generated or that have changed will remain in the collection.
 * 
 * <p>
 * A change tracker should have a set of <em>changed</em> elements and a set of <em>already generated</em>
 * elements.
 * 
 * @author epp
 */
public interface ChangeTracker {

	/**
	 * Eliminates all generators for elements for which code has already been generated and which have
	 * not changed.
	 * 
	 * @param generators
	 *            - A {@link Map} from {@link GeneratorKey}s to {@link AbstractElementGenerator}s.
	 */
	void prune(Map<GeneratorKey, AbstractElementGenerator> generators);

	/**
	 * Consumes the recorded changed elements.
	 * 
	 * @param generators
	 *            - A {@link Map} from {@link GeneratorKey}s to {@link AbstractElementGenerator}s.
	 */
	void consumeChanges(Map<GeneratorKey, AbstractElementGenerator> generators);

	/**
	 * Record the changed elements from a list of {@link ChangeObject}s.
	 * 
	 * @param changeList
	 *            - A list of {@link ChangeObject}s.
	 */
	void addChanges(List<ChangeObject> changeList);

	/**
	 * Record an element as being already generated.
	 * 
	 * @param kind
	 *            - The {@link Kind} of element generator.
	 * @param object
	 *            - The {@link NamedElement} already generated.
	 */
	void addAlreadyGenerated(Kind kind, NamedElement object);

	/**
	 * @return The collection of all elements that have changed since the last generation.
	 */
	Collection<EObject> getAllChanged();

	/**
	 * Update the 'changed' and 'already generated' collections whenever a resource is closed. This removes
	 * all the elements from that resource from these collections.
	 * 
	 * @param resource
	 *            - A {@link Resource}.
	 */
	void closeResource(Resource resource);

	/**
	 * Resets the 'changed' and 'already generated' sets.
	 */
	void resetAll();

	/**
	 * Register the top capsule of the model.
	 * 
	 * @param topCapsule
	 *            - A {@link Capsule}.
	 */
	void setTop(EObject topCapsule);

}
