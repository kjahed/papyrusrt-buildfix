/*******************************************************************************
 * Copyright (c) 2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.papyrus.designer.languages.common.base.codesync.ChangeObject;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator.GeneratorKey;
import org.eclipse.papyrusrt.codegen.cpp.XTUMLRT2CppCodeGenerator.Kind;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.RTSModelLibraryUtils;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.UMLRTProfileUtil;
import org.eclipse.papyrusrt.xtumlrt.trans.from.uml.UML2xtumlrtTranslator;
import org.eclipse.papyrusrt.xtumlrt.util.GeneralUtil;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.util.UMLSwitch;
import org.eclipse.uml2.uml.util.UMLUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * An implementation of {@link ChangeTracker} that records changes in an xtUML-RT model transformed
 * from a UML2 model (+ UML-RT Profile) created with Papyrus-RT, during a Papyrus-RT session, and
 * accepts {@link ChangeObject}s from the Papyrus-RT editor.
 * 
 * @author epp
 */
public class UMLChangeTracker implements ChangeTracker {

	/** The common instance of this class. */
	private static ChangeTracker ACTIVE_INSTANCE = null;

	/** The collection of elements for which code has already been generated, indexed by {@link Resource} and {@link Kind}. */
	private static Map<Resource, Multimap<Kind, EObject>> alreadyGenerated = new HashMap<>();

	/** The collection of elements that have changed since the last generation, indexed by {@link Resource} and {@link Kind}. */
	private static Map<Resource, Multimap<Kind, EObject>> changed = new HashMap<>();

	/** A record of the last time an element was modified. */
	private static Map<Resource, Map<String, Long>> controllerTimestamps = new HashMap<>();

	/** Extension used in generated C++ files. */
	private static final String CPP_EXTENSION = ".cc";

	/** The translator from UML to xtUML-RT. Used to obtain the original UML element that was transformed into an xtUML-RT element. */
	private UML2xtumlrtTranslator translator;

	/** The top capsule. */
	private EObject top;

	/** The {@link CppCodePattern}. */
	private CppCodePattern cpp;

	/**
	 * 
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}.
	 */
	public UMLChangeTracker(CppCodePattern cpp) {
		this.cpp = cpp;
		this.translator = cpp.getTranslator();
	}

	@Override
	public void setTop(EObject top) {
		this.top = top;
	}

	public static void setActiveInstance(ChangeTracker trackerInstance) {
		ACTIVE_INSTANCE = trackerInstance;
	}

	public static ChangeTracker getActiveInstance() {
		return ACTIVE_INSTANCE;
	}

	@Override
	public void prune(Map<GeneratorKey, AbstractElementGenerator> generators) {
		Iterator<Entry<GeneratorKey, AbstractElementGenerator>> iterator = generators
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<GeneratorKey, AbstractElementGenerator> next = iterator.next();
			GeneratorKey key = next.getKey();
			EObject umlElement = translator.getSource((org.eclipse.papyrusrt.xtumlrt.common.NamedElement) key.object);
			AbstractElementGenerator generator = next.getValue();
			String outputPath = generator.cpp.getOutputFolder().getAbsolutePath();
			boolean shouldRegenerate = false;
			// If missing output file then we should always regenerate.
			for (FileName f : generator.getGeneratedFilenames()) {
				File output = new File(outputPath + File.separator + f.getAbsolutePath() + CPP_EXTENSION);
				if (!output.exists()) {
					shouldRegenerate = true;
					break;
				}
				File header = new File(outputPath + File.separator + f.getIncludePath());
				if (!header.exists()) {
					shouldRegenerate = true;
					break;
				}
			}
			// Check to see if main source file is missing
			if (key.kind == Kind.Structural) {
				File output = new File(outputPath + File.separator + generator.cpp.getMainName() + CPP_EXTENSION);
				if (!output.exists()) {
					shouldRegenerate = true;
				}

				File f = cpp.getControllerAllocations(GeneralUtil.getName(top));
				if (f != null) {
					long lastModified = f.lastModified();
					Map<String, Long> map = controllerTimestamps.get(top.eResource());
					if (map == null) {
						map = new HashMap<>();
						controllerTimestamps.put(top.eResource(), map);
					} else {
						Long timeStamp = map.get(GeneralUtil.getName(top));
						if (timeStamp != null && timeStamp != lastModified) {
							shouldRegenerate = true;
						}
					}
					map.put(GeneralUtil.getName(top), lastModified);
				}
			}

			if (shouldRegenerate) {
				continue;
			}

			if (alreadyGeneratedContains(key.kind, umlElement)
					&& !changedContains(key.kind, umlElement)) {
				if (key.kind == Kind.Capsule) {
					boolean portWithChangedProtocol = false;
					for (org.eclipse.uml2.uml.Port umlPort : UMLRTProfileUtil.getAllRTPorts((Class) umlElement)) {
						org.eclipse.uml2.uml.Package protocol = UMLRTProfileUtil.getProtocol(umlPort);
						if (changedContains(Kind.Protocol, protocol)) {
							portWithChangedProtocol = true;
							break;
						}
					}

					if (!portWithChangedProtocol) {
						CodeGenPlugin.getLogger()
								.log(Level.INFO,
										"Pruning "
												+ ((NamedElement) umlElement)
														.getQualifiedName()
												+ " from generation.");
						iterator.remove();
					}
				} else {
					CodeGenPlugin.getLogger()
							.log(Level.INFO,
									"Pruning "
											+ ((org.eclipse.uml2.uml.NamedElement) umlElement)
													.getQualifiedName()
											+ " from generation.");
					iterator.remove();
				}
			}
		}
	}

	@Override
	public void consumeChanges(Map<GeneratorKey, AbstractElementGenerator> generators) {
		for (GeneratorKey gk : generators.keySet()) {
			EObject umlElement = translator.getSource((org.eclipse.papyrusrt.xtumlrt.common.NamedElement) gk.object);
			removeChanged(gk.kind, umlElement);
		}
	}

	/**
	 * @param kind
	 *            - A {@link Kind} of element generator.
	 * @param object
	 *            - A model element.
	 * @return {@code true} iff code for the element has already been generated.
	 */
	private boolean alreadyGeneratedContains(Kind kind, EObject object) {
		if (object != null) {
			Resource resource = object.eResource();
			if (resource != null) {
				Multimap<Kind, EObject> multimap = alreadyGenerated.get(resource);
				if (multimap != null) {
					return multimap.containsEntry(kind, object);
				}
			}
		}
		return false;
	}

	@Override
	public void addAlreadyGenerated(Kind kind, org.eclipse.papyrusrt.xtumlrt.common.NamedElement object) {
		Element source = translator.getSource(object);
		if (source != null) {
			Resource resource = source.eResource();
			if (resource != null) {
				Multimap<Kind, EObject> multimap = alreadyGenerated.get(resource);
				if (multimap == null) {
					multimap = HashMultimap.create();
					alreadyGenerated.put(resource, multimap);
				}
				multimap.put(kind, source);
			}
		}
	}

	/**
	 * @param kind
	 *            - A {@link Kind} of element generator.
	 * @param object
	 *            - A model element.
	 * @return {@code true} iff the element has changed since the last generation.
	 */
	private boolean changedContains(Kind kind, EObject object) {
		if (object != null) {
			Resource resource = object.eResource();
			if (resource != null) {
				Multimap<Kind, EObject> multimap = changed.get(resource);
				if (multimap != null) {
					return multimap.containsEntry(kind, object);
				}
			}
		}
		return false;
	}

	@Override
	public Collection<EObject> getAllChanged() {
		Collection<EObject> all = new ArrayList<>();
		if (changed != null) {
			Collection<Multimap<Kind, EObject>> allMultiMaps = changed.values();
			if (allMultiMaps != null) {
				for (Multimap<Kind, EObject> map : allMultiMaps) {
					all.addAll(map.values());
				}
			}
		}
		return all;
	}

	/**
	 * Removes the element from the collection of 'changed' elements.
	 * 
	 * @param kind
	 *            - A {@link Kind} of element generator.
	 * @param object
	 *            - A model element.
	 */
	private void removeChanged(Kind kind, EObject object) {
		if (object != null) {
			Resource resource = object.eResource();
			if (resource != null) {
				Multimap<Kind, EObject> multimap = changed.get(resource);
				if (multimap != null) {
					multimap.remove(kind, object);
				}
			}
		}
	}

	@Override
	public void addChanges(List<ChangeObject> changeList) {
		if (changeList != null) {

			ChangeCollector changeCollector = new ChangeCollector(changed);

			// In order to avoid going through switch logic for same element
			// again we check if the element is already taken care of using the
			// set.
			Set<EObject> alreadyAdded = new HashSet<>();
			for (ChangeObject change : changeList) {
				EObject target = change.eObject;

				// Bug 473056: The target can be null depending on the
				// nature of the change. In this case there is nothing to do so
				// the element should be silently ignored.
				if (target != null && !alreadyAdded.contains(target)) {
					alreadyAdded.add(target);
					changeCollector.doSwitch(target);
				}
			}
		}
	}

	@Override
	public void closeResource(Resource resource) {
		if (changed.remove(resource) != null) {
			CodeGenPlugin.getLogger().log(Level.INFO,
					"Cleaning up changed map for resource: "
							+ resource.getURI()
									.toString());
		}

		if (alreadyGenerated.remove(resource) != null) {
			CodeGenPlugin.getLogger().log(Level.INFO,
					"Cleaning up already generated map for resource: "
							+ resource.getURI()
									.toString());
		}
	}

	@Override
	public void resetAll() {
		UMLChangeTracker.alreadyGenerated = new HashMap<>();
		UMLChangeTracker.changed = new HashMap<>();
	}

	/**
	 * 
	 */
	private class ChangeCollector extends UMLSwitch<Boolean> {

		/** A local reference to the collection of changed elements. */
		final Map<Resource, Multimap<Kind, EObject>> changed;

		/**
		 * Constructor.
		 *
		 * @param changed
		 *            - The collection of changed elements.
		 */
		public ChangeCollector(Map<Resource, Multimap<Kind, EObject>> changed) {
			this.changed = changed;
		}

		// Unhandled kinds of elements are ignored.
		@Override
		public Boolean defaultCase(EObject object) {
			EObject container = object.eContainer();
			if (container != null) {
				doSwitch(container);
			}

			return Boolean.TRUE;
		}

		@Override
		public Boolean casePackage(Package object) {
			// If this is a <<ProtocolContainer>> then create a Protocol
			// generator for it and
			// examine all capsules, select the ones that have a port that uses
			// this protocol.
			if (UMLRTProfileUtil.isProtocolContainer(object)) {
				org.eclipse.papyrusrt.xtumlrt.common.CommonElement xtumlrtElement = translator.getGenerated(object);
				if (xtumlrtElement instanceof NamedElement && !RTSModelLibraryUtils.isSystemElement(xtumlrtElement)) {
					createChange(Kind.Protocol, object);
					for (Element element : ((Package) EcoreUtil.getRootContainer(object)).getOwnedElements()) {
						if (UMLRTProfileUtil.isCapsule(element)) {
							for (org.eclipse.uml2.uml.Port umlPort : UMLRTProfileUtil.getAllRTPorts((org.eclipse.uml2.uml.Class) element)) {
								if (UMLRTProfileUtil.getProtocol(umlPort) == object) {
									doSwitch(element);
								}
							}
						}
					}
				}
			}

			return Boolean.TRUE;
		}

		@Override
		public Boolean caseDataType(DataType object) {
			createChange(Kind.BasicClass, object);
			return Boolean.TRUE;
		}

		@Override
		public Boolean caseBehavior(org.eclipse.uml2.uml.Behavior object) {
			// We need to have a separate clause for state machines as in UML
			// Behavior is a sub-type of Class so we don't want to confuse
			// this case with the Class case, otherwise the caseClass method
			// will create a BasicClass generator even if the behavior
			// belongs to a capsule
			EObject container = object.eContainer();
			if (container instanceof org.eclipse.uml2.uml.Class) {
				// Bug 468195
				// If the changes are made to statemachine only then we do not
				// need to create structural generator to regenerate Controller.
				// So create changes for its class and behaviour
				createClassChange((org.eclipse.uml2.uml.Class) container);
			}

			// bug#477748: if changes are made to a opaque behaviour
			// then we still need to determine if we need to generate
			// so continue investigate
			return defaultCase(object);
		}

		@Override
		public Boolean caseClass(org.eclipse.uml2.uml.Class object) {
			createClassChange(object);

			// Regenerate Controllers.cc/hh if the changes are made to Capsule
			if (UMLRTProfileUtil.isCapsule(object)) {
				createChange(Kind.Structural, top);
			}

			return Boolean.TRUE;
		}

		/**
		 * Creates changes for a (UML) {@link Class} element, whether a basic class or capsule,
		 * and changes for its {@link StateMachine} and its super-class(es).
		 * 
		 * @param object
		 *            - The {@link Class} element.
		 */
		private void createClassChange(Class object) {

			if (!UMLRTProfileUtil.isCapsule(object)) {
				createChange(Kind.BasicClass, object);
			} else {
				// If the Class has been stereotyped with "Capsule" then the
				// structural as well as state machine elements must be generated.
				createChange(Kind.Capsule, object);

				Behavior behaviour = object.getClassifierBehavior();
				if (behaviour == null) {
					behaviour = object.getOwnedBehavior(null);
				}

				if (behaviour instanceof StateMachine) {
					createChange(Kind.StateMachine, behaviour);
				} else {
					createChange(Kind.EmptyStateMachine, object);
				}
			}
			// bug#476606 : Regenerate subclasses
			for (Setting setting : UMLUtil.getInverseReferences(object)) {
				EObject eo = setting.getEObject();
				if (eo instanceof Generalization) {
					Classifier specific = ((Generalization) eo).getSpecific();
					if (specific != object && specific instanceof Class) {
						createClassChange((Class) specific);
					}
				}
			}
		}

		/**
		 * Adds an element to the collection of changed elements.
		 * 
		 * @param kind
		 *            - The {@link Kind} of element generator.
		 * @param object
		 *            - The element.
		 */
		private void createChange(Kind kind, EObject object) {
			if (object != null) {
				Resource resource = object.eResource();
				if (resource != null) {
					Multimap<Kind, EObject> multimap = changed.get(resource);
					if (multimap == null) {
						multimap = HashMultimap.create();
						changed.put(resource, multimap);
					}
					multimap.put(kind, object);
				}
			}
		}
	}

}
