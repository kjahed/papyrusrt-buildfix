/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppGenerationProperties;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTRuntime;
import org.eclipse.papyrusrt.codegen.lang.cpp.CppWriter;
import org.eclipse.papyrusrt.codegen.lang.cpp.Element;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.HeaderFile;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Constructor;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppArtifact;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppClass;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppEnum;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppNamespace;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Destructor;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.ElementList;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Enumerator;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.LinkageSpec;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.name.FileName;
import org.eclipse.papyrusrt.xtumlrt.common.Artifact;
import org.eclipse.papyrusrt.xtumlrt.common.Capsule;
import org.eclipse.papyrusrt.xtumlrt.common.CapsulePart;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.common.Port;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.RedefinableElement;
import org.eclipse.papyrusrt.xtumlrt.common.Signal;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.RTSModelLibraryUtils;
import org.eclipse.papyrusrt.xtumlrt.trans.from.uml.UML2xtumlrtTranslator;
import org.eclipse.papyrusrt.xtumlrt.util.GeneralUtil;
import org.eclipse.papyrusrt.xtumlrt.util.QualifiedNames;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;

import com.google.inject.Inject;

/**
 * The Code Pattern implements the top-level of the documented code pattern. This is
 * used for sharing language model elements between different parts of the generator's
 * implementation.
 * 
 * <p>
 * This class provides factory methods for different kinds of C++ elements to be generated
 * from model elements. These factory methods cache their result so that multiple invocations
 * for the same source model element return the same generated C++ element. As such, this class
 * can be seen as a repository of the generated C++ elements.
 * 
 * <p>
 * Note that the factory methods provided here return specific patterns intended for use with the
 * UML-RT runtime. For more general purpose generation, the abstract syntax of the C++ subset defined
 * in the {@link org.eclipse.papyrusrt.codegen.lang.cpp} package could be used directly.
 */
public class CppCodePattern {

	/** The translator from UML to xtUMLrt. */
	private UML2xtumlrtTranslator translator;

	/** The output folder. */
	private File outputFolder;

	/** The folder containing the model. */
	private File modelFolder;

	/** The top capsule. */
	private EObject top;

	/** The list of source files generated. */
	private List<FileName> sourceFiles = new ArrayList<>();

	/** Map from top-level model elements (classes, capsules, protocols, etc.) to the corresponding C++ {@link ElementList}. */
	private final Map<Key, ElementList> elementLists = new HashMap<>();

	/** Map from model artifact elements to C++ artifacts. */
	private final Map<Key, CppArtifact> artifacts = new HashMap<>();

	/** Map from model class/capsule/protocol elements to C++ class elements. */
	private final Map<Key, CppClass> cppClasses = new HashMap<>();

	/** Map from model protocol elements to C++ namespace elements. */
	private final Map<Key, CppNamespace> cppNamespaces = new HashMap<>();

	/** Map from model elements to C++ variable elements. */
	private final Map<Key, Variable> variables = new HashMap<>();

	/** Map from model signal, port, part and enum elements to C++ enums. */
	private final Map<Key, CppEnum> cppEnums = new HashMap<>();

	/** Map from model elements to C++ enumerators. */
	private final Map<Key, Enumerator> enumerators = new HashMap<>();

	/** Map from model class/capsule elements to C++ constructors. */
	private final Map<Key, Constructor> constructors = new HashMap<>();

	/** Map from model class/capsule elements to C++ copy constructors. */
	private final Map<Key, Constructor> copyConstructors = new HashMap<>();

	/** Map from model class/capsule elements to C++ destructors. */
	private final Map<Key, Destructor> destructors = new HashMap<>();

	/** List of all generated C++ {@link ElementList}s. */
	private final List<ElementList> outputs = new ArrayList<>();

	/**
	 * Describes the meaning of each type of "file" that can be produced by the generator.
	 */
	public static enum Output {
		CapsuleClass, ProtocolClass, BasicClass, UserEnum, ProtocolBaseRole, ProtocolConjugateRole, SignalId, PortId, BorderPortId, InternalPortId, PartId, Deployment, UMLRTCapsuleClass, UMLRTTypeDescriptor, Artifact;
	}

	/**
	 * Constructor.
	 */
	@Inject
	public CppCodePattern() {
	}

	public UML2xtumlrtTranslator getTranslator() {
		return translator;
	}

	public File getOutputFolder() {
		return this.outputFolder;
	}

	public void setOutputFolder(File outputFolder) {
		this.outputFolder = outputFolder;
	}

	public void setModelFolder(File modelFolder) {
		this.modelFolder = modelFolder;
	}

	public void setTop(EObject topCapsule) {
		this.top = topCapsule;
	}

	public void setTranslator(UML2xtumlrtTranslator translator) {
		this.translator = translator;
	}

	/**
	 * Add a collection of file names to the list of source files.
	 * 
	 * @param filenames
	 *            - A collection of {@link FileName}s.
	 */
	public void setFilenames(Collection<FileName> filenames) {
		sourceFiles.addAll(filenames);
	}

	/**
	 * Mark the given element list as writable. Files that are not marked as writable
	 * can be referenced but output should not be produced.
	 * 
	 * @param elements
	 *            - An {@link ElementList}.
	 * @return {@code true} iff the element list is in the {@link #elementLists} list of
	 *         {@link ElementLists} generated and it is added to the list of outputs.
	 */
	public boolean markWritable(ElementList elements) {
		if (!elementLists.containsValue(elements)) {
			return false;
		}
		outputs.add(elements);
		return true;
	}

	/**
	 * Creates a {@link File} handle for the "<capsule>.controllers" file that specifies the allocation
	 * of capsule parts to controllers.
	 * 
	 * @param topCapsule
	 *            - The name of the top capsule.
	 * @return The {@link File} handle.
	 */
	public File getControllerAllocations(String topCapsule) {
		File allocationsFile = new File(modelFolder, topCapsule + ".controllers");
		return allocationsFile.exists() ? allocationsFile : null;
	}

	/**
	 * Obtain a C++ {@link ElementList} (i.e. a C++ compilation unit, a source/header pair) for a given
	 * model {@link NamedElement} and the kind of output.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The {@link NamedElement}.
	 * @return The {@link ElementList}.
	 */
	public ElementList getElementList(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		ElementList elementList = null;
		switch (output) {
		case Deployment:
			elementList = elementLists.get(k);
			if (elementList == null) {
				elementList = new ElementList(new FileName(element.getName() + "Controllers"));
				elementLists.put(k, elementList);
			}
			return elementList;
		case Artifact:
			elementList = elementLists.get(k);
			if (elementList == null) {
				elementList = new ElementList(new FileName(element.getName()));
				elementLists.put(k, elementList);
			}
			return elementList;
		default:
			return getElementList(k, element);
		}
	}

	/**
	 * Obtain a C++ "artifact", a simple header/implementation file pair, for a given model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link CppArtifact}.
	 */
	public CppArtifact getWritableCppArtifact(Output output, NamedElement element) {
		CppArtifact artifact = getArtifact(output, element);
		HeaderFile header = artifact.getDefinedIn();
		if (header instanceof ElementList) {
			markWritable((ElementList) header);
		}
		return artifact;
	}

	/**
	 * Obtain a C++ class for a given model element and mark it as writable.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link CppClass}.
	 */
	public CppClass getWritableCppClass(Output output, NamedElement element) {
		CppClass cls = getCppClass(output, element);
		HeaderFile header = cls.getDefinedIn();
		if (header instanceof ElementList) {
			markWritable((ElementList) header);
		}
		return cls;
	}

	/**
	 * Obtain a C++ enum for a given model element and mark it as writable.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link CppEnum}.
	 */
	public CppEnum getWritableCppEnum(Output output, NamedElement element) {
		CppEnum enm = getCppEnum(output, element);
		HeaderFile header = enm.getDefinedIn();
		if (header instanceof ElementList) {
			markWritable((ElementList) header);
		}
		return enm;
	}

	/**
	 * Obtain a C++ namespace for a given model element and mark it as writable.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link CppNamespace}.
	 */
	public CppNamespace getWritableCppNamespace(Output output, NamedElement element) {
		CppNamespace namespace = getCppNamespace(output, element);
		HeaderFile header = namespace.getDefinedIn();
		if (header instanceof ElementList) {
			markWritable((ElementList) header);
		}
		return namespace;
	}

	/**
	 * Obtain a C++ class for a given model element depending on the kind of model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link CppClass}.
	 */
	public CppClass getCppClass(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		CppClass cls = cppClasses.get(k);
		if (cls == null) {
			switch (output) {
			case CapsuleClass:
			case BasicClass:
				cls = new CppClass(getName(output, element));
				ElementList elements = getElementList(k, element);
				elements.addElement(cls);
				applyRTCppGenerationProperties(elements, element);
				break;
			case ProtocolBaseRole:
			case ProtocolConjugateRole:
				cls = new CppClass(getName(output, element));
				getCppNamespace(Output.ProtocolClass, element).addMember(cls);
				break;
			default:
				throw new RuntimeException("code pattern does not contain a CppClass for " + output.toString());
			}

			cppClasses.put(k, cls);
		}

		return cls;
	}

	/**
	 * Obtain a C++ namespace for a given model element depending on the kind of model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link CppNamespace}.
	 */
	public CppNamespace getCppNamespace(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		CppNamespace namespace = cppNamespaces.get(k);
		if (namespace == null) {
			switch (output) {
			case ProtocolClass:
				namespace = new CppNamespace(getName(output, element));
				ElementList elements = getElementList(k, element);
				elements.addElement(namespace);
				applyRTCppGenerationProperties(elements, element);
				break;
			default:
				throw new RuntimeException("code pattern does not contain a CppNamespace for " + output.toString());
			}

			cppNamespaces.put(k, namespace);
		}

		return namespace;
	}

	/**
	 * Obtain a C++ constructor for a given model element depending on the kind of model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link Constructor}.
	 */
	public Constructor getConstructor(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		Constructor ctor = constructors.get(k);
		if (ctor == null) {
			switch (output) {
			case BasicClass:
			case CapsuleClass:
			case ProtocolBaseRole:
			case ProtocolConjugateRole:
				ctor = new Constructor();
				getCppClass(output, element).addMember(CppClass.Visibility.PUBLIC, ctor);
				break;
			default:
				throw new RuntimeException("code pattern does not contain a Constructor for " + output.toString());
			}

			constructors.put(k, ctor);
		}

		return ctor;
	}

	/**
	 * Obtain a C++ copy constructor for a given model element depending on the kind of model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link Constructor}.
	 */
	public Constructor getCopyConstructor(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		Constructor ctor = copyConstructors.get(k);
		if (ctor == null) {
			switch (output) {
			case BasicClass:
			case CapsuleClass:
			case ProtocolBaseRole:
			case ProtocolConjugateRole:
				ctor = new Constructor();
				getCppClass(output, element).addMember(CppClass.Visibility.PUBLIC, ctor);
				break;
			default:
				throw new RuntimeException("code pattern does not contain a copy Constructor for " + output.toString());
			}
			copyConstructors.put(k, ctor);
		}
		return ctor;
	}

	/**
	 * Obtain a C++ destructor for a given model element depending on the kind of model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link Destructor}.
	 */
	public Destructor getDestructor(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		Destructor dtor = destructors.get(k);
		if (dtor == null) {
			switch (output) {
			case BasicClass:
			case CapsuleClass:
				dtor = new Destructor();
				getCppClass(output, element).addMember(CppClass.Visibility.PUBLIC, dtor);
				break;
			default:
				throw new RuntimeException("code pattern does not contain a Destructor for " + output.toString());
			}

			destructors.put(k, dtor);
		}

		return dtor;
	}

	/**
	 * Obtain a C++ variable for a given element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The {@link Variable}.
	 */
	public Variable getVariable(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		Variable var = variables.get(k);
		if (var == null) {
			switch (output) {
			case UMLRTCapsuleClass:
				var = new Variable(LinkageSpec.EXTERN, UMLRTRuntime.UMLRTCapsuleClass.getType().const_(), getName(output, element));
				break;
			case UMLRTTypeDescriptor:
				var = new Variable(LinkageSpec.EXTERN, UMLRTRuntime.UMLRTObject.getType().const_(), getName(output, element));
				break;
			default:
				throw new RuntimeException("code pattern does not contain a Variable for " + output.toString());
			}

			variables.put(k, var);
		}

		return var;
	}

	/**
	 * Create a C++ enum for a given model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The {@link NamedElement}.
	 * @return The {@link CppEnum}.
	 */
	public CppEnum getCppEnum(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		CppEnum enm = cppEnums.get(k);
		if (enm == null) {
			switch (output) {
			case UserEnum:
				enm = new CppEnum(element.getName());
				break;
			default:
				throw new RuntimeException("code pattern does not contain a CppEnum for " + output.toString());
			}

			ElementList elements = getElementList(k, element);
			elements.addElement(enm);
			enm.setDefinedIn(elements);

			cppEnums.put(k, enm);
		}

		return enm;
	}

	/**
	 * Create a C++ artifact (.cc/.hh pari) for a given model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The {@link NamedElement}.
	 * @return The {@link CppArtifact}.
	 */
	public CppArtifact getArtifact(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		CppArtifact artifact = artifacts.get(k);
		if (artifact == null) {
			switch (output) {
			case Artifact:
				if (!(element instanceof Artifact)) {
					throw new RuntimeException("code pattern requires Artifact for " + output.toString() + " but got " + element.getClass().getCanonicalName());
				}
				artifact = new CppArtifact(getName(output, element));
				ElementList elements = getElementList(k, element);
				elements.addElement(artifact);
				break;
			default:
				throw new RuntimeException("code pattern does not contain an Artifact for " + output.toString());
			}
		}
		return artifact;
	}

	/**
	 * @param protocol
	 *            - A model {@link Protocol}.
	 * @return A {@link Map}<{@link String},{@link NamedElement}> from signal names to {@link Signal}s.
	 *         The map is sorted (i.e. it has a predictable iteration order) so that the signals of each protocol
	 *         are after the signals of its parent(s).
	 */
	private static Map<String, NamedElement> getSortedSignals(Protocol protocol) {
		// If the base element has 2 features, A and C, and the derived has feature B, then
		// the IDs must be sorted A, C, B. The parents are first and then the children,
		// each group is sorted by name. In cases where the derived element redefines
		// feature A, the IDs must continue to be sorted A, C, B.
		//
		// This is needed because the IDs are built into the code. Derived elements must
		// use the same number as the parent.
		//
		// The implementation finds the root Element, then starts a LinkedHashMap that is
		// indexed by the feature element's name. LinkedHashMaps will replace existing
		// values without changing the order.

		RedefinableElement parent = protocol.getRedefines();
		Map<String, NamedElement> elements = parent instanceof Protocol
				? getSortedSignals((Protocol) parent)
				: new LinkedHashMap<>();
		for (NamedElement element : RTSModelLibraryUtils.getUserSignals(protocol)) {
			elements.put(element.getName(), element);
		}

		return elements;
	}

	/**
	 * @param capsule
	 *            - A {@link Capsule}.
	 * @return A {@link Map}<{@link String},{@link NamedElement}> from port names to {@link Port}s.
	 *         The map is sorted (i.e. it has a predictable iteration order) so that the ports of each capsule
	 *         are after the ports of its parent(s).
	 */
	private static Map<String, NamedElement> getSortedPorts(Capsule capsule) {
		// See implementation comment in #getSortedSignals.

		RedefinableElement parent = capsule.getRedefines();
		Map<String, NamedElement> elements = parent instanceof Capsule
				? getSortedPorts((Capsule) parent)
				: new LinkedHashMap<>();
		for (Port element : XTUMLRTUtil.getRTPorts(capsule)) {
			elements.put(element.getName(), element);
		}

		return elements;
	}

	/**
	 * @param capsule
	 *            - A {@link Capsule}.
	 * @return A {@link Map}<{@link String},{@link NamedElement}> from part names to {@link CapsulePart}s.
	 *         The map is sorted (i.e. it has a predictable iteration order) so that the parts of each capsule
	 *         are after the parts of its parent(s).
	 */
	private static Map<String, NamedElement> getSortedParts(Capsule capsule) {
		// See implementation comment in #getSortedSignals.

		RedefinableElement parent = capsule.getRedefines();
		Map<String, NamedElement> elements = parent instanceof Capsule
				? getSortedParts((Capsule) parent)
				: new LinkedHashMap<>();
		for (CapsulePart element : XTUMLRTUtil.getCapsuleParts(capsule)) {
			elements.put(element.getName(), element);
		}

		return elements;
	}

	/**
	 * Obtain a C++ enum to define the IDs of signals, ports or parts of a given protocol or capsule element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The {@link NamedElement}.
	 * @return The {@link CppEnum}.
	 */
	public CppEnum getIdEnum(Output output, NamedElement element) {
		Key k = new Key(output, element, null);
		CppEnum enm = cppEnums.get(k);
		if (enm == null) {
			Map<String, NamedElement> enumeratorElements = null;

			Output clsKind = null;
			String enumName = null;
			Expression firstLiteral = null;
			switch (output) {
			case SignalId:
				CppNamespace namespace = getCppNamespace(Output.ProtocolClass, element);
				enm = new CppEnum(namespace, "SignalId", UMLRTRuntime.UMLRTSignal.FIRST_PROTOCOL_SIGNAL_ID());
				namespace.addMember(enm);

				enumeratorElements = getSortedSignals((Protocol) element);
				break;
			case PortId:
				clsKind = Output.CapsuleClass;
				enumName = "PortId";
				enumeratorElements = getSortedPorts((Capsule) element);
				break;
			case PartId:
				clsKind = Output.CapsuleClass;
				enumName = "PartId";
				enumeratorElements = getSortedParts((Capsule) element);
				break;
			case BorderPortId:
				clsKind = Output.CapsuleClass;
				enumName = "BorderPortId";
				break;
			case InternalPortId:
				clsKind = Output.CapsuleClass;
				enumName = "InternalPortId";
				break;
			default:
				throw new RuntimeException("code pattern does not contain a CppEnum for " + output.toString());
			}

			if (enm == null) {
				CppClass cls = getCppClass(clsKind, element);
				enm = new CppEnum(cls, enumName, firstLiteral);
				cls.addMember(CppClass.Visibility.PUBLIC, enm);
			}

			cppEnums.put(k, enm);

			if (enumeratorElements != null) {
				for (NamedElement enumeratorElement : enumeratorElements.values()) {
					getEnumerator(output, enumeratorElement, element);
				}
			}
		}

		return enm;
	}

	/**
	 * Obtain a C++ enumerator for a specific signal, port or part element
	 * inside a context (protocol or capsule).
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The {@link NamedElement}: a {@link Signal}, {@link Port} or {@link CapsulePart}
	 * @param context
	 *            - The context {@link NamedElement}: either a {@link Protocol} for signal, or a {@link Capsule} for ports and parts
	 * @return The C++ {@link Enumerator}.
	 */
	public Enumerator getEnumerator(Output output, NamedElement element, NamedElement context) {
		Key k = new Key(output, element, context);
		Enumerator enumerator = enumerators.get(k);
		if (enumerator == null) {
			switch (output) {
			case SignalId: {
				if (!(element instanceof Signal)) {
					throw new RuntimeException("code pattern requires Signal for " + output.toString() + " but got " + element.getClass().getCanonicalName());
				}

				if (!(context instanceof NamedElement)) {
					throw new RuntimeException("code pattern requires NamedElement as owner for " + output.toString() + " but got " + context.getClass().getCanonicalName());
				}

				break;
			}
			case PortId:
			case PartId:
			case BorderPortId:
			case InternalPortId: {
				if (!(context instanceof NamedElement)) {
					throw new RuntimeException("code pattern requires NamedElement as owner for " + output.toString() + " but got " + context.getClass().getCanonicalName());
				}

				break;
			}
			default:
				throw new RuntimeException("code pattern does not contain an Enumerator for " + output.toString());
			}

			// Accessing the IdEnum could trigger addition of all enumerators. Check for a value
			// after accessing the enum. Create a new instance only when it does not already
			// exist.
			CppEnum enm = getIdEnum(output, context);
			enumerator = enumerators.get(k);
			if (enumerator == null) {
				enumerator = enm.add(getName(output, element));
				enumerators.put(k, enumerator);
			}
		}

		return enumerator;
	}

	/**
	 * Obtain a {@link MemberAccess} expression to an {@link Enumerator} for a signal, port or part.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The {@link NamedElement}: a {@link Signal}, {@link Port} or {@link CapsulePart}
	 * @param context
	 *            - The context {@link NamedElement}: either a {@link Protocol} for signal, or a {@link Capsule} for ports and parts
	 * @return The C++ {@link MemberAccess}.
	 */
	public Expression getEnumeratorAccess(Output output, NamedElement element, NamedElement context) {
		org.eclipse.papyrusrt.codegen.lang.cpp.element.NamedElement cppElement = null;
		switch (output) {
		case SignalId: {
			if (!(element instanceof Signal)) {
				throw new RuntimeException("code pattern requires Signal for " + output.toString() + " but got " + element.getClass().getCanonicalName());
			}

			Signal signal = (Signal) element;
			if (context == null) {
				NamedElement owner = XTUMLRTUtil.getOwner(signal);
				if (owner == null || !(owner instanceof Protocol)) {
					throw new RuntimeException("code pattern: the owner of signal " + signal.getName() + " is not a protocol");
				}
				Protocol protocol = (Protocol) owner;
				if (RTSModelLibraryUtils.isSystemElement(protocol)) {
					return UMLRTRuntime.getSystemProtocolSignalAccess(signal);
				}
				context = protocol;
			}

			cppElement = getCppNamespace(Output.ProtocolClass, context);
			break;
		}
		case PartId: {
			if (!(element instanceof CapsulePart)) {
				throw new RuntimeException("code pattern requires Property for " + output.toString() + " but got " + element.getClass().getCanonicalName());
			}

			if (context == null) {
				CapsulePart part = (CapsulePart) element;
				NamedElement capsule = XTUMLRTUtil.getOwner(part);
				context = capsule;
			}

			cppElement = getCppClass(Output.CapsuleClass, context);
			break;
		}
		case PortId:
		case BorderPortId:
		case InternalPortId: {
			if (!(element instanceof Port)) {
				throw new RuntimeException("code pattern requires Port for " + output.toString() + " but got " + element.getClass().getCanonicalName());
			}

			if (context == null) {
				Port port = (Port) element;
				NamedElement capsule = XTUMLRTUtil.getOwner(port);
				context = capsule;
			}

			cppElement = getCppClass(Output.CapsuleClass, context);
			break;
		}
		default:
			throw new RuntimeException("no enumerator access expression defined for " + output);
		}

		Enumerator enumerator = getEnumerator(output, element, context);
		return new MemberAccess(cppElement, enumerator);
	}

	/**
	 * This is a dispatch method that delegates to the {@code get}<em>XXX</em> methods to obtain
	 * the C++ element that corresponds to the given model element.
	 * 
	 * @param element
	 *            - A model {@link NamedElement}.
	 * @return The corresponding C++ {@link Element}.
	 */
	public Element getCppElement(NamedElement element) {
		if (RTSModelLibraryUtils.isSystemElement(element)) {
			return UMLRTRuntime.getSystemElement(element);
		}
		if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Capsule) {
			return getCppClass(Output.CapsuleClass, element);
		} else if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.StructuredType) {
			return getCppClass(Output.BasicClass, element);
		} else if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Protocol) {
			return getCppNamespace(Output.ProtocolClass, element);
		} else if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Enumeration) {
			return getCppEnum(Output.UserEnum, element);
		} else if (element instanceof org.eclipse.papyrusrt.xtumlrt.common.Artifact) {
			return getArtifact(Output.Artifact, element);
		}
		return null;
	}

	/**
	 * Constructs the name given to the C++ element to generate for a given model element.
	 * 
	 * @param output
	 *            - The {@link Output} kind.
	 * @param element
	 *            - The model {@link NamedElement}.
	 * @return The name as a {@link String}.
	 */
	private String getName(Output output, NamedElement element) {
		switch (output) {
		case CapsuleClass:
			return "Capsule_" + element.getName();
		case ProtocolBaseRole:
			return "Base";
		case ProtocolConjugateRole:
			return "Conj";
		case SignalId:
			return "signal_" + element.getName();
		case PortId:
			return "port_" + element.getName();
		case BorderPortId:
			return "borderport_" + element.getName();
		case InternalPortId:
			return "internalport_" + element.getName();
		case PartId:
			return "part_" + element.getName();
		case UMLRTTypeDescriptor:
			return "UMLRTType_" + element.getName();
		default:
			return element.getName();
		}
	}

	/**
	 * Obtain a C++ {@link ElementList} (i.e. a C++ compilation unit, a source/header pair) for a given
	 * model {@link NamedElement} and the {@link Key} used to store the elements in the cache(s).
	 * 
	 * @param k
	 *            - The {@link Key} kind.
	 * @param element
	 *            - The {@link NamedElement}.
	 * @return The {@link ElementList}.
	 */
	private ElementList getElementList(Key k, NamedElement element) {
		ElementList elementList = elementLists.get(k);
		if (elementList == null) {
			switch (k.output) {
			case UMLRTCapsuleClass:
				return getElementList(Output.CapsuleClass, element);
			case Artifact:
				if (!(element instanceof Artifact)) {
					throw new RuntimeException("code pattern for Artifact requires " + k.output.toString());
				}
				Artifact aft = (Artifact) element;
				String fileName = aft.getFileName();
				if (fileName.isEmpty()) {
					if (aft.getName().isEmpty()) {
						throw new RuntimeException("artifact has no name: " + QualifiedNames.getQualifiedName(aft, true));
					}
					fileName = aft.getName();
				}
				elementList = new ElementList(new FileName(fileName));
				break;
			default:
				elementList = new ElementList(new FileName(element.getName()));
				break;
			}
			elementLists.put(k, elementList);
		}

		return elementList;
	}

	// TODO Accessors for common parts of the top-level of the code pattern. E.g., the
	// SignalId is needed by several generators; there should be an accessor here.

	/**
	 * Write the C++ model elements into C++ source files. This is essentially a model-to-text
	 * transformation where the model is the intermediate C++ abstract syntax defined in
	 * {@link org.eclipse.papyrusrt.codegen.lang.cpp}.
	 * 
	 * @return {@code true} if successful.
	 */
	public boolean write() {
		String baseFolder = outputFolder.getAbsolutePath();

		boolean ret = true;
		for (ElementList output : outputs) {
			CppWriter out = CppWriter.create(baseFolder, output);
			try {
				if (!output.write(out)) {
					ret = false;
					System.err.println("Failure while writing '" + output.getName().getAbsolutePath() + "' to disk");
				}
			} finally {
				out.close();
			}
		}

		// Add main source file name
		AbstractCppMakefileGenerator gen = new CppMakefileGenerator();
		String makefile = gen.formatFilename(GeneralUtil.getName(top));

		// Generate makefile for top capsule
		gen.generate(Paths.get(baseFolder, makefile).toString(), sourceFiles, getMainName());

		// Generate default makefile
		new CppDefaultMakefileGenerator().generate(Paths.get(baseFolder, "Makefile").toString(), makefile);

		// Also add CMake file
		gen = new CppCMakeListsGenerator();
		makefile = gen.formatFilename(GeneralUtil.getName(top));

		// Generate CMake for top capsule
		gen.generate(Paths.get(baseFolder, makefile).toString(), sourceFiles, getMainName());


		return ret;
	}

	public String getMainName() {
		return GeneralUtil.getName(top) + "Main";
	}

	/**
	 * Applied generation properties specified with stereotypes from the RTCppProperties profile.
	 * 
	 * @param elements
	 *            - An C++ {@link ElementList}.
	 * @param element
	 *            - A model {@link NameElement}.
	 */
	private void applyRTCppGenerationProperties(ElementList elements, NamedElement element) {
		Boolean generateHeaderBool = RTCppGenerationProperties.getFileGenerationPropGenerateHeader(element);
		Boolean generateImplemBool = RTCppGenerationProperties.getFileGenerationPropGenerateImplementation(element);
		String headPreface = RTCppGenerationProperties.getCppFileHeaderPreface(element);
		String headEnding = RTCppGenerationProperties.getCppFileHeaderEnding(element);
		String implPreface = RTCppGenerationProperties.getCppFileImplementationPreface(element);
		String implEnding = RTCppGenerationProperties.getCppFileImplementationEnding(element);
		boolean generateHeader = generateHeaderBool == null || generateHeaderBool.booleanValue();
		boolean generateImplem = generateImplemBool == null || generateImplemBool.booleanValue();
		elements.setWriteDecl(generateHeader);
		elements.setWriteDefn(generateImplem);
		elements.addDeclPrefaceText(headPreface);
		elements.addDeclEndingText(headEnding);
		elements.addDefnPrefaceText(implPreface);
		elements.addDefnEndingText(implEnding);
	}

	/**
	 * Instances of this class are used as keys to the maps that store the generated C++ elements.
	 */
	private static class Key {

		/** The {@link Output} kind. */
		public final Output output;

		/** The model {@link NamedElement}. */
		public final NamedElement element;

		/** The context model {@link NamedElement}. */
		public final NamedElement context;

		/**
		 * Constructor.
		 *
		 * @param output
		 *            - The {@link Output} kind.
		 * @param element
		 *            - The model {@link NamedElement}.
		 * @param context
		 *            - The context model {@link NamedElement}.
		 */
		Key(Output output, NamedElement element, NamedElement context) {
			this.output = output;
			this.element = element;
			this.context = context;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Key)) {
				return false;
			}
			Key other = (Key) obj;
			if (context == null) {
				return output == other.output
						&& element.equals(other.element)
						&& other.context == null;
			}
			return output == other.output
					&& element.equals(other.element)
					&& context.equals(other.context);
		}

		@Override
		public int hashCode() {
			return output.ordinal() ^ element.hashCode();
		}
	}

}
