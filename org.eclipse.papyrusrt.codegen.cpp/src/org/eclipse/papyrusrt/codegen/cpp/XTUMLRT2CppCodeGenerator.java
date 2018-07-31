/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.internal.CapsuleGenerator;
import org.eclipse.papyrusrt.codegen.cpp.internal.EclipseGeneratorManager;
import org.eclipse.papyrusrt.codegen.cpp.internal.GeneratorDescriptor;
import org.eclipse.papyrusrt.codegen.cpp.internal.ProtocolGenerator;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppGenerationProperties;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.xtumlrt.common.Artifact;
import org.eclipse.papyrusrt.xtumlrt.common.BaseContainer;
import org.eclipse.papyrusrt.xtumlrt.common.Behaviour;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.Entity;
import org.eclipse.papyrusrt.xtumlrt.common.Enumeration;
import org.eclipse.papyrusrt.xtumlrt.common.Model;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.common.Package;
import org.eclipse.papyrusrt.xtumlrt.common.Parameter;
import org.eclipse.papyrusrt.xtumlrt.common.Port;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.Signal;
import org.eclipse.papyrusrt.xtumlrt.common.StructuredType;
import org.eclipse.papyrusrt.xtumlrt.common.TypeDefinition;
import org.eclipse.papyrusrt.xtumlrt.common.util.CommonSwitch;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.RTSModelLibraryUtils;
import org.eclipse.papyrusrt.xtumlrt.statemach.StateMachine;
import org.eclipse.papyrusrt.xtumlrt.statemach.StatemachFactory;
import org.eclipse.papyrusrt.xtumlrt.umlrt.RTModel;
import org.eclipse.papyrusrt.xtumlrt.util.GeneralUtil;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTExtensions;

/**
 * The "core" of the code generator which creates and executes element-specific generators for each model element
 * that needs code to be generated.
 * 
 * <p>
 * It collects elements to be generated, including dependent elements, creates element-kind specific generators
 * for them (instances of subclasses of {@link AbstractElementGenerator}), then prunes the collection of generators by
 * removing those for elements that should not be generated (like library elements, elements explicitly marked as to
 * be ignored, and elements that have already been generated but have not changed, if incremental generation is performed)
 * and invokes {@link AbstractElementGenerator#generate} for each such generator.
 * 
 * <p>
 * This class is instantiated and invoked by an {@link AbstractUMLRT2CppCodeGenerator}.
 */
public class XTUMLRT2CppCodeGenerator {

	/** The {@link CppCodePattern}. */
	private final CppCodePattern cpp;

	/** The top capsule. */
	private EObject top;

	/** The {@link ChangeTracker} used to support incremental generation. */
	private ChangeTracker changeTracker;

	/**
	 * This enum defines the types of generators supported. Each generator corresponds to a
	 * particular kind of model element.
	 * 
	 * <p>
	 * The kinds supported are:
	 * <ul>
	 * <li>{@code BasicClass}("ClassGenerator")
	 * <li>{@code Capsule}("CapsuleGenerator")
	 * <li>{@code Protocol}("ProtocolGenerator")
	 * <li>{@code Behaviour}("BehaviourGenerator")
	 * <li>{@code StateMachine}("StateMachineGenerator")
	 * <li>{@code EmptyStateMachine}("EmptyStateMachineGenerator")
	 * <li>{@code Enum}("EnumGenerator")
	 * <li>{@code Artifact}("ArtifactGenerator")
	 * <li>{@code Structural}("StructuralGenerator")
	 * </ul>
	 * 
	 * <p>
	 * This is used by the 'generator' extension point.
	 * 
	 * @see {@link EclipseGeneratorManager}, {@link GeneratorDescriptor}
	 */
	public enum Kind {
		BasicClass("ClassGenerator"), Capsule("CapsuleGenerator"), Protocol("ProtocolGenerator"), Behaviour("BehaviourGenerator"), StateMachine("StateMachineGenerator"), EmptyStateMachine("EmptyStateMachineGenerator"), Enum("EnumGenerator"), Artifact(
				"ArtifactGenerator"), Structural("StructuralGenerator");

		/** The kind's id string. */
		public final String id;

		/**
		 * Constructor.
		 * 
		 * @param id
		 *            - The kind's id string.
		 */
		Kind(String id) {
			this.id = id;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param cpp
	 *            - The {@link CppCodePattern}
	 * @param changeTracker
	 *            - The {@link ChangeTracker}
	 */
	public XTUMLRT2CppCodeGenerator(CppCodePattern cpp, ChangeTracker changeTracker) {
		this.cpp = cpp;
		this.changeTracker = changeTracker;
	}

	public ChangeTracker getChangeTracker() {
		return this.changeTracker;
	}

	/**
	 * Tell the generator to use the given capsule as the top capsule.
	 * 
	 * @param topCapsule
	 *            - An {@link EObject}. It should be a {@link Capsule}.
	 */
	public void setTop(EObject topCapsule) {
		this.top = topCapsule;
		this.changeTracker.setTop(topCapsule);
	}

	/**
	 * Execute the generation.
	 * 
	 * @param inputElements
	 *            - The list of {@link EObject}s to translate.
	 * @return An {@link IStatus} with the result.
	 */
	public IStatus generate(List<EObject> inputElements) {
		long start = System.currentTimeMillis();
		MultiStatus status = new MultiStatus(CodeGenPlugin.ID, IStatus.OK, "UML-RT Code Generation", null);

		Map<GeneratorKey, AbstractElementGenerator> generators = new LinkedHashMap<>();
		Collector collector = new Collector(generators);
		for (EObject target : inputElements) {
			if (!toBeGenerated(target)) {
				continue;
			}
			if (!collector.doSwitch(target)) {
				status.add(CodeGenPlugin.error("Error while examining model changes"));
			}
		}
		status.add(CodeGenPlugin.info("Examined model changes " + (System.currentTimeMillis() - start) + "ms"));

		Set<FileName> srcFileNames = new LinkedHashSet<>();
		for (AbstractElementGenerator gen : generators.values()) {
			srcFileNames.addAll(gen.getGeneratedFilenames());
		}
		cpp.setFilenames(srcFileNames);

		start = System.currentTimeMillis();
		changeTracker.prune(generators);
		status.add(CodeGenPlugin.info("Prune unchanged elements " + (System.currentTimeMillis() - start) + "ms"));

		for (GeneratorKey key : generators.keySet()) {
			AbstractElementGenerator generator = generators.get(key);
			try {
				start = System.currentTimeMillis();
				if (generator.generate()) {
					status.add(CodeGenPlugin
							.info(generator.getLabel() + ' ' + (System.currentTimeMillis() - start) + "ms"));
					changeTracker.addAlreadyGenerated(key.kind, (NamedElement) key.object);
				} else {
					status.add(CodeGenPlugin.error("Error while generating " + generator.getLabel()));
				}
			} catch (Exception e) {
				status.add(CodeGenPlugin.error(e));
			}
		}

		start = System.currentTimeMillis();
		changeTracker.consumeChanges(generators);
		status.add(CodeGenPlugin
				.info("Consume changes to elements " + (System.currentTimeMillis() - start) + "ms"));

		return status;
	}

	/**
	 * Determine whether the given element should be generated or ignored.
	 * 
	 * @param target
	 *            - An {@link EObject}. Should be a {@link Element}.
	 * @return {@code true} if the {@code target} element should be generated. {@code false} if
	 *         it is not a {@link NamedElement}, or an element from the RTS library or it has the
	 *         {@code GenerationProperties} stereotype (or a subclass) from the {@code RTCppProperties} profile
	 *         and the stereotype has the {@code generate} attribute set to {@code false}.
	 */
	private boolean toBeGenerated(EObject target) {
		if (!(target instanceof NamedElement)) {
			return false;
		}
		NamedElement element = (NamedElement) target;
		if (RTSModelLibraryUtils.isSystemElement(element)) {
			return false;
		}
		Boolean generateBool = RTCppGenerationProperties.getGenerationPropGenerate(element);
		if (generateBool == null) {
			return true;
		}
		return generateBool.booleanValue();
	}

	/**
	 * Keys for a map storing element generators.
	 * 
	 * <p>
	 * It is essentially a triple <em>(kind, element, context)</em> where
	 * <ul>
	 * <li>kind is the {@link Kind} of generator,
	 * <li>element is the {@link Element} to be generated, and
	 * <li>context is an {@link Element} used as context
	 * </ul>
	 * 
	 * @author epp
	 */
	protected static class GeneratorKey {

		/** The {@link Kind} of generator. */
		public final Kind kind;

		/** The {@link Element} to be generated. */
		public final EObject object;

		/** An {@link Element} used as context. */
		public final EObject context;

		/**
		 * Constructor.
		 *
		 * @param kind
		 *            - The {@link Kind} of generator.
		 * @param object
		 *            - The {@link Element} to be generated.
		 * @param context
		 *            - An {@link Element} used as context. Possibly null.
		 */
		public GeneratorKey(Kind kind, EObject object, EObject context) {
			this.kind = kind;
			this.object = object;
			this.context = context;
		}

		@Override
		public int hashCode() {
			return kind.hashCode() ^ object.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof GeneratorKey)) {
				return false;
			}

			GeneratorKey other = (GeneratorKey) obj;
			if (context == null) {
				return kind == other.kind && object.equals(other.object) && other.context == null;
			}
			return kind == other.kind && object.equals(other.object) && context.equals(other.context);
		}
	}

	/**
	 * This class is a switch that traverses the model creating and collecting generators (instances of
	 * {@link AbstractElementGenerator}) according to element dependencies. For example, if code for a
	 * {@link Capsule} is to be generated, then the collector creates a {@link CapsuleGenerator} and
	 * a {@link ProtocolGenerator} for each of the {@link Protocol}s in the capsule's {@link Port}s.
	 * 
	 * @author epp
	 */
	private class Collector extends CommonSwitch<Boolean> {

		/** The {@link GeneratorManager}. */
		private final GeneratorManager genManager = GeneratorManager.getInstance();

		/** The map from (kind, element, context) triples to element generators. */
		public final Map<GeneratorKey, AbstractElementGenerator> generators;

		/**
		 * Constructor.
		 *
		 * @param generators
		 *            - The map from (kind, element, context) triples to element generators.
		 */
		public Collector(Map<GeneratorKey, AbstractElementGenerator> generators) {
			this.generators = generators;
		}

		/**
		 * Creates an element generator (an instance of a {@link AbstractElementGenerator} of the
		 * given {@code kind} for the given {@code element} in the given {@code context}.
		 * 
		 * @param kind
		 *            - The {@link Kind} of generator.
		 * @param object
		 *            - The {@link NamedElement} to generate code for.
		 * @param context
		 *            - The {@link NamedElement} to use as the context.
		 * @param <T>
		 *            - The type of {@link NamedElement} to generate.
		 * @return - {@code true} if a generator was created and inserted and {@code false}
		 *         otherwise.
		 */
		private <T extends NamedElement> boolean createGenerator(Kind kind, T object, T context) {
			GeneratorKey key = new GeneratorKey(kind, object, context);
			if (generators.get(key) != null) {
				return false;
			}

			AbstractElementGenerator generator = genManager.getGenerator(kind, cpp, object, context);
			if (generator == null) {
				return false;
			}

			generators.put(key, generator);
			return true;
		}

		/**
		 * Assign an empty state machine to the given {@link Entity}.
		 * 
		 * @param entity
		 *            - An {@link Entity}.
		 */
		private void assignEmptyStateMachine(Entity entity) {
			if (entity.getBehaviour() == null) {
				StateMachine emptyStateMachine = StatemachFactory.eINSTANCE.createStateMachine();
				entity.setBehaviour(emptyStateMachine);
			}
		}

		/**
		 * Check if the given behaviour is an empty state machine.
		 * 
		 * @param behaviour
		 *            - A {@link Behaviour}.
		 * @return {@code true} if {@code behaviour} is {@code null} or a {@link StateMachine} with a
		 *         {@code null} top composite state.
		 */
		private boolean isEmptyStateMachine(Behaviour behaviour) {
			return behaviour == null
					|| (behaviour instanceof StateMachine
							&& ((StateMachine) behaviour).getTop() == null);
		}

		// Unhandled kinds of elements are ignored.
		@Override
		public Boolean defaultCase(EObject object) {
			return true;
		}

		@Override
		public Boolean caseModel(Model model) {
			if (!toBeGenerated(model)) {
				return true;
			}
			for (Entity element : model.getEntities()) {
				doSwitch(element);
			}
			for (Protocol element : model.getProtocols()) {
				doSwitch(element);
			}
			for (TypeDefinition element : model.getTypeDefinitions()) {
				doSwitch(element);
			}
			for (Package element : model.getPackages()) {
				doSwitch(element);
			}
			if (model instanceof RTModel) {
				for (Artifact element : ((RTModel) model).getContainedArtifacts()) {
					doSwitch(element);
				}
			}

			return true;
		}

		@Override
		public Boolean casePackage(Package packge) {
			if (!toBeGenerated(packge)) {
				return true;
			}
			for (Entity element : packge.getEntities()) {
				doSwitch(element);
			}
			for (Protocol element : packge.getProtocols()) {
				doSwitch(element);
			}
			for (TypeDefinition element : packge.getTypeDefinitions()) {
				doSwitch(element);
			}
			for (Package element : packge.getPackages()) {
				doSwitch(element);
			}

			return true;
		}

		@Override
		public Boolean caseProtocol(Protocol protocol) {
			if (!toBeGenerated(protocol)) {
				return true;
			}
			// If this is a Protocol then create a Protocol generator for it and
			// examine all capsules, select the ones that have a port that uses
			// this protocol.
			// The context for the new generator is null because we generate the
			// protocol in its own class, not within another class.
			if (!RTSModelLibraryUtils.isSystemElement(protocol) && createGenerator(Kind.Protocol, protocol, null)) {
				// Look at the data type for all signals and generate the data
				// class (if any)
				// for each.
				for (Signal signal : XTUMLRTUtil.getSignals(protocol)) {
					for (Parameter p : signal.getParameters()) {
						if (p.getType() != null) {
							doSwitch(p.getType());
						}
					}
				}

				BaseContainer root = XTUMLRTUtil.getRoot(protocol);
				if (root == null) {
					return true;
				}
				for (Capsule capsule : XTUMLRTExtensions.getAllCapsules(root)) {
					for (Port port : XTUMLRTExtensions.getAllRTPorts(capsule)) {
						if (port.getType() == protocol) {
							doSwitch(capsule);
						}
					}
				}
			}
			return true;
		}

		@Override
		public Boolean caseTypeDefinition(TypeDefinition typedef) {
			if (!toBeGenerated(typedef)) {
				return true;
			}
			return doSwitch(typedef.getType());
		}

		@Override
		public Boolean caseBehaviour(Behaviour behaviour) {
			Kind kind = behaviour instanceof StateMachine ? Kind.StateMachine : Kind.Behaviour;
			EObject eObject = behaviour.eContainer();
			if (eObject instanceof Entity) {
				Entity entity = (Entity) eObject;
				createGenerator(kind, behaviour, entity);
			}
			return true;
		}

		@Override
		public Boolean caseStructuredType(StructuredType struct) {
			if (!toBeGenerated(struct)) {
				return true;
			}
			// The context for the new generator is null because we generate the
			// struct in its own class, not within another class.
			createGenerator(Kind.BasicClass, struct, null);
			return true;
		}

		@Override
		public Boolean caseCapsule(Capsule capsule) {
			if (!toBeGenerated(capsule)) {
				return true;
			}
			// The context for the new generator is null because we generate the
			// capsule in its own class, not within another class.
			createGenerator(Kind.Capsule, capsule, null);

			Behaviour behaviour = XTUMLRTExtensions.getActualBehaviour(capsule);
			if (behaviour != null && !isEmptyStateMachine(behaviour) && toBeGenerated(behaviour)) {
				doSwitch(behaviour);
			} else {
				assignEmptyStateMachine(capsule);
				createGenerator(Kind.EmptyStateMachine, capsule.getBehaviour(), capsule);
			}

			// TODO Until there is a way to allocate capsules to threads, we
			// treat the
			// CapsuleType called "Top" specially.
			// The context for the new generator is null because we don't
			// generate the Controllers.cc/.hh nested within another class.
			if (GeneralUtil.getName(top).equals(capsule.getName())) {
				createGenerator(Kind.Structural, capsule, null);
			}

			return true;
		}

		@Override
		public Boolean caseEntity(Entity object) {
			if (!toBeGenerated(object)) {
				return true;
			}

			// The user provided classes are not nested within a class so there
			// is no
			// reason to provide context.
			createGenerator(Kind.BasicClass, object, null);
			return true;
		}

		@Override
		public Boolean caseEnumeration(Enumeration object) {
			if (!toBeGenerated(object)) {
				return true;
			}

			// The user provided enumerations are not nested within a class so
			// there is no
			// reason to provide context.
			createGenerator(Kind.Enum, object, null);
			return true;
		}

		@Override
		public Boolean caseArtifact(Artifact object) {
			if (!toBeGenerated(object)) {
				return true;
			}
			createGenerator(Kind.Artifact, object, null);
			return true;
		}

	};
}
