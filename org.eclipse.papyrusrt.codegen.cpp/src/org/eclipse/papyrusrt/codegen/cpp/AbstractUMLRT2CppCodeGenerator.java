/*******************************************************************************
 * Copyright (c) 2014-2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.papyrus.designer.languages.common.base.codesync.ChangeObject;
import org.eclipse.papyrus.designer.languages.common.base.codesync.ManageChangeEvents;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.UMLRTCodeGenerator;
import org.eclipse.papyrusrt.codegen.config.CodeGenProvider;
import org.eclipse.papyrusrt.codegen.cpp.profile.facade.RTCppPropertiesProfileMetadata;
import org.eclipse.papyrusrt.codegen.cpp.validation.PostUML2xtumlrtValidator;
import org.eclipse.papyrusrt.codegen.cpp.validation.PreUML2xtumlrtValidator;
import org.eclipse.papyrusrt.codegen.utils.CodeGenUtils;
import org.eclipse.papyrusrt.xtumlrt.aexpr.uml.XTUMLRTBoundsEvaluator;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.external.ExternalPackageManager;
import org.eclipse.papyrusrt.xtumlrt.external.ExternalPackageMetadata;
import org.eclipse.papyrusrt.xtumlrt.trans.from.uml.UML2xtumlrtModelTranslator;
import org.eclipse.papyrusrt.xtumlrt.trans.from.uml.UML2xtumlrtTranslator;
import org.eclipse.papyrusrt.xtumlrt.trans.preproc.ModelPreprocessor;
import org.eclipse.papyrusrt.xtumlrt.trans.to.dot.ui.handlers.TranslateToDotHandler;
import org.eclipse.uml2.uml.Element;

import com.google.inject.Injector;

/**
 * Abstract base generator. This is the generation "director". It executes the overall workflow of generation.
 * 
 * @see {@link #generate}
 */
public abstract class AbstractUMLRT2CppCodeGenerator implements UMLRTCodeGenerator {

	/** Flag for debugging. Must be false in production. */
	public static final boolean DEBUG = false;

	/** The collection of 'known' packages that must be loaded and registered. */
	private static final ExternalPackageMetadata[] REQUIRED_PACKAGES = {
			RTCppPropertiesProfileMetadata.INSTANCE,
			AnsiCLibraryMetadata.INSTANCE,
	};

	/** Default status for the outcome of generator tasks. */
	private static final IStatus OK_STATUS = new Status(IStatus.OK, CodeGenPlugin.ID, "ok");

	/** Reference to the preprocessor. */
	private static final ModelPreprocessor MODEL_PREPROCESSOR = new ModelPreprocessor();

	/** The language used in state machine actions. */
	private static final String ACTION_LANGUAGE = "C++";

	/** The {@link ExternalPackageManager}. */
	protected ExternalPackageManager externalPackageManager = ExternalPackageManager.getInstance();

	/** The {@link ResourceSet} containing the model being generated as well as external packages. */
	private ResourceSet resourceSet;

	/** Whether we are running in a stand-alone Java application or within Eclipse. */
	private boolean standalone = true;

	/** Flag set to {@code false} for incremental generation, and {@code true} for full regeneration. */
	private boolean regenerate = false;

	/** Whether the external package manager should be reset on each generation. */
	private boolean forceExternalPackageReset = true;

	/** Whether the source model is a UML model ({@code true}) or an XTUMLRT model ({@code false}). */
	private boolean sourceIsUML = true;

	/** The translator from UML to XTUMLRT. */
	private UML2xtumlrtTranslator translator;

	/** The translator from XTUMLRT to Cpp (the C++ language subset meta-model). */
	private XTUMLRT2CppCodeGenerator codegen;

	/** The Cpp code pattern factory. */
	private CppCodePattern cpp;

	/** The change tracker for incremental generation. */
	private ChangeTracker changeTracker;

	/** A {@link Map} from {@link File}s to {@link List}s of {@link EObject}s, associating each input model folder to the list of elements in that model. */
	private Map<File, List<EObject>> targets;

	/** A {@link Map} from {@link File}s to {@link File}s, associating each input model folder to the corresponding output folder. */
	private Map<File, File> outputFolders;

	/**
	 * Constructor.
	 */
	protected AbstractUMLRT2CppCodeGenerator() {
		reset();
	}

	/**
	 * Reset the state of the generator to a clean slate.
	 */
	private void reset() {
		Injector injector = CodeGenProvider.getDefault().getInjector();
		translator = new UML2xtumlrtModelTranslator();
		cpp = injector.getInstance(CppCodePattern.class);
		cpp.setTranslator(translator);

		changeTracker = sourceIsUML ? new UMLChangeTracker(cpp) : new XTUMLRTChangeTracker(cpp);
		codegen = new XTUMLRT2CppCodeGenerator(cpp, changeTracker);
		targets = new HashMap<>();
		outputFolders = new HashMap<>();
		XTUMLRTBoundsEvaluator.setTranslator(translator);
	}

	/**
	 * @return {@code true} iff we are running in a stand-alone Java application or within Eclipse.
	 */
	public boolean isStandalone() {
		return standalone;
	}

	/**
	 * @param standalone
	 *            - Whether we are running in a stand-alone Java application or within Eclipse.
	 */
	@Override
	public void setStandalone(boolean standalone) {
		this.standalone = standalone;
	}

	@Override
	public void setRegenerate(boolean flag) {
		regenerate = flag;
	}

	@Override
	public boolean getRegenerate() {
		return regenerate;
	}

	/**
	 * Initialize management of external packages. It performs several tasks:
	 * 
	 * <ol>
	 * <li>Sets the {@link ResourceSet} of the {@link #externalPackageManager};
	 * <li>Possibly resets to its initial state;
	 * <li>Tells the {@link #externalPackageManager} whether it is running in stand-alone mode;
	 * <li>Adds all the built-in packages as required.
	 * <li>Invokes {@link ExternalPackageManager#setup()} which loads and registers the packages;
	 * <li>Performs custom setups for each package as required.
	 * </ol>
	 * 
	 * @param resourceSet
	 *            - The {@link ResourceSet}.
	 * @return An {@link IStatus}
	 */
	public IStatus setupExternalPackageManagement(ResourceSet resourceSet) {
		IStatus success = OK_STATUS;
		externalPackageManager = ExternalPackageManager.getInstance();
		if (forceExternalPackageReset) {
			externalPackageManager.reset();
		}
		externalPackageManager.setResourceSet(resourceSet);
		externalPackageManager.setStandalone(standalone);
		for (ExternalPackageMetadata metadata : REQUIRED_PACKAGES) {
			externalPackageManager.addRequiredPackage(metadata);
		}
		success = externalPackageManager.setup();
		return success;
	}

	/**
	 * Executes the generation workflow.
	 * 
	 * <ol>
	 * <li>Creates the necessary components:
	 * <ul>
	 * <li>{@link XTUMLRT2CppCodeGenerator}: the "core" that executes element-specific generators for each element to be generated.
	 * <li>{@link CppCodePattern}: factory class for common C++ model elements which caches elements generated and invokes the model-to-text transformation.
	 * <li>{@link UML2xtumlrtModelTranslator}: the translator from UML to the xtUML-RT intermediate representation.
	 * <li>{@link ChangeTracker}: recorder of elements which have changed since the last generation.
	 * </ul>
	 * <li>Collects the model changes.
	 * <li>Collects the model element's input and output folders and {@link ResourceSet}.
	 * <li>Loads the RTS model library.
	 * <li>Translates the model elements from UML to xtUML-RT.
	 * <li>Executes the core generator on the xtUML-RT elements.
	 * <li>Invokes the model-to-text transformation that generates a CDT project and source files.
	 * </ol>
	 * 
	 * @param elements
	 *            - The list of input model elements to be generated
	 * @param top
	 *            - The name of the top capsule
	 * @param uml
	 *            - Whether the input model elements are UML2 elements (true) or xtUMLrt elements (false)
	 * @return An {@link IStatus} with the result of generation.
	 */
	@Override
	public synchronized IStatus generate(List<EObject> elements, String top, boolean uml) {
		IStatus success = OK_STATUS;
		sourceIsUML = uml;
		reset();
		success = preGenerationTasks(elements, top);
		if (isOk(success)) {
			MultiStatus accumulatedStatus = new MultiStatus(CodeGenPlugin.ID, IStatus.INFO, "UML-RT Code Generator Invoked", null);

			long start = System.currentTimeMillis();
			if (targets.isEmpty()) {
				accumulatedStatus.add(CodeGenPlugin.error("Selection must contain at least one model element"));
			} else {
				try {
					for (Map.Entry<File, List<EObject>> entry : targets.entrySet()) {
						File modelFolder = entry.getKey();
						List<EObject> elementsToGenerate = entry.getValue();
						doGenerate(elementsToGenerate, modelFolder, accumulatedStatus);
					}
				} catch (Throwable t) {
					CodeGenPlugin.error("Error during code generation.", t);
					accumulatedStatus.add(CodeGenPlugin.error(t));
				}
			}

			String message = "Generation " + (accumulatedStatus.getSeverity() <= IStatus.INFO ? "complete" : "error")
					+ ", elapsed time " + (System.currentTimeMillis() - start) + " ms";

			MultiStatus result = new MultiStatus(CodeGenPlugin.ID, IStatus.INFO, message, null);
			result.addAll(accumulatedStatus);
			success = result;
		}
		return success;
	}

	/**
	 * Perform pre-generation tasks.
	 * 
	 * @param elements
	 *            - The list of {@link EObject} elements for which code will be generated.
	 * @param top
	 *            - A {@link String}; the name of the top capsule.
	 * @return A {@link IStatus}.
	 */
	protected IStatus preGenerationTasks(List<EObject> elements, String top) {
		IStatus success = OK_STATUS;
		success = setupResourceSet(elements);
		success = findTopCapsule(elements, top);
		if (isOk(success)) {
			collectFolders(elements);
			collectAllChangedElements(elements);
		}
		return success;
	}

	/**
	 * Setup the {@link #resourceSet} to work with.
	 * 
	 * <p>
	 * The resource set is chosen to be the resource set of the first {@link EObject} provided.
	 * 
	 * <p>
	 * If the elements belong to different resource sets, a warning is issued, as it may result in generation errors.
	 * 
	 * <p>
	 * If the chosen resource set is different than the previously chosen resource set, we initialize the external packages.
	 * 
	 * @see #setupExternalPackageManagement()
	 * 
	 * @param elements
	 *            - The list of {@link EObject}s from which to generate.
	 * @return An {@link IStatus}.
	 */
	private IStatus setupResourceSet(List<EObject> elements) {
		IStatus success = OK_STATUS;
		ResourceSet elementsResourceSet = null;
		boolean first = true;
		for (EObject eobj : elements) {
			if (first || elementsResourceSet == null) {
				first = false;
				elementsResourceSet = eobj.eResource().getResourceSet();
			} else if (elementsResourceSet != eobj.eResource().getResourceSet()) {
				CodeGenPlugin.warning("Target elements belong to different resource sets. This may result in errors during generation.");
			}
		}
		if (elementsResourceSet != resourceSet || forceExternalPackageReset) {
			resourceSet = elementsResourceSet;
			success = setupExternalPackageManagement(resourceSet);
		}
		if (resourceSet == null) {
			success = new Status(IStatus.ERROR, CodeGenPlugin.ID, "Unable to inizialize the resource set.");
		}
		return success;
	}

	/**
	 * @param runningStatus
	 *            - A {@link IStatus}.
	 * @return {@code true} iff the {@code runningStatus} is {@code null} or is OK.
	 */
	private boolean isOk(IStatus runningStatus) {
		return runningStatus == null || runningStatus.isOK();
	}

	/**
	 * @param elements
	 *            - A list of {@link EObject}s.
	 * @param top
	 *            - A {@link String}. The name of the top capsule to look for.
	 * @return A {@link IStatus}.
	 */
	private IStatus findTopCapsule(List<EObject> elements, String top) {
		IStatus success = OK_STATUS;
		EObject topCapsule = CodeGenUtils.findCapsule(elements.get(0), top);
		if (topCapsule != null) {
			codegen.setTop(topCapsule);
			cpp.setTop(topCapsule);
		} else {
			String message = "Top capsule not found";
			CodeGenPlugin.error(message);
			success = new Status(IStatus.ERROR, CodeGenPlugin.ID, message);
		}
		return success;
	}

	/**
	 * Record the folder(s) for input elements and the corresponding output folder(s).
	 * 
	 * @param elements
	 *            - A {@link List} of {@link EObject} elements.
	 */
	private void collectFolders(List<EObject> elements) {
		for (EObject eobj : elements) {
			File modelFolder = getModelFolder(eobj);
			List<EObject> list = targets.get(modelFolder);
			if (list == null) {
				list = new ArrayList<>();
				targets.put(modelFolder, list);
			}
			list.add(eobj);
			File outputFolder = outputFolders.get(modelFolder);
			if (outputFolder == null) {
				outputFolder = getOutputFolder(eobj, codegen);
				outputFolders.put(modelFolder, outputFolder);
			}
		}
	}

	/**
	 * Collects the elements that changed since the last generation,
	 * as well as those elements that depend on the changed elements.
	 * 
	 * @param elements
	 *            - A {@link List} of {@link EObject}s.
	 */
	private void collectAllChangedElements(List<EObject> elements) {
		final List<ChangeObject> changes = new ArrayList<>();
		if (regenerate) {
			changeTracker.resetAll();
		}
		for (EObject eobj : elements) {
			getChanges(changes, eobj);
		}
		changeTracker.addChanges(changes);
	}

	/**
	 * Record the changes in a list to be tracked by the {@link ChangeTracker}.
	 * 
	 * @param changes
	 *            - The list where to accumulate {@link ChangeObject}s.
	 * @param object
	 *            - An {@link EObject} used to get the editing domain from which the changes are received.
	 */
	private void getChanges(final List<ChangeObject> changes, EObject object) {
		if (CodeGenPlugin.isStandalone() || CodeGenPlugin.isTextual()) {
			// do nothing for stand alone generation
			return;
		}
		TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(object);

		if (domain != null) {

			EList<ChangeObject> changeList = ManageChangeEvents.getChangeList(domain);
			ManageChangeEvents.stopRecording(domain);
			ManageChangeEvents.initChangeList(domain, true);
			if (changeList != null) {
				changes.addAll(changeList);
			}
		}
	}

	/**
	 * @param inputElements
	 *            - The {@link List} of {@link EObject} elements to generate.
	 * @param modelFolder
	 *            - The {@link File} handle to the input model folder.
	 * @param accumulatedStatus
	 *            - The {@link MultiStatus} to accumulate the outcomes.
	 */
	protected void doGenerate(List<EObject> inputElements, File modelFolder, MultiStatus accumulatedStatus) {
		List<EObject> elements = inputElements;
		File outputFolder = outputFolders.get(modelFolder);
		cpp.setOutputFolder(outputFolder);
		cpp.setModelFolder(modelFolder);

		NamedElement xtumlrtModelElement = null;
		// 1. Translate elements from UML to XTUMLRT if necessary
		if (sourceIsUML) {
			try {
				// pre validation
				PreUML2xtumlrtValidator preValidator = new PreUML2xtumlrtValidator();
				MultiStatus preResult = preValidator.validate(elements);
				handleValidationResult(preResult);

				elements = translateFromUMLtoXTUMLRT(elements, modelFolder, accumulatedStatus);
				xtumlrtModelElement = (NamedElement) translator.getGenerated((Element) inputElements.get(0));

				if (DEBUG) {
					org.eclipse.uml2.uml.Model umlModel = (org.eclipse.uml2.uml.Model) EcoreUtil.getRootContainer(inputElements.get(0));
					IFile newFile = TranslateToDotHandler.getNewFile(umlModel);
					final IStatus status = TranslateToDotHandler.generate(xtumlrtModelElement, newFile);
				}

				// post validation
				PostUML2xtumlrtValidator postValidator = new PostUML2xtumlrtValidator(translator);
				MultiStatus postResult = postValidator.validate(xtumlrtModelElement);
				handleValidationResult(postResult);

			} catch (Throwable t) {
				accumulatedStatus.add(CodeGenPlugin.error("Error during translation from UML to XTUMLRT", t));
				t.printStackTrace(System.err);
				return;
			}
		} else {
			xtumlrtModelElement = (NamedElement) inputElements.get(0);
		}

		if (xtumlrtModelElement != null) {
			MODEL_PREPROCESSOR.preprocess(xtumlrtModelElement);
		} else {
			accumulatedStatus.add(CodeGenPlugin.error("Translation to xtumlrt yielded a null model."));
		}

		// 2. Translate elements from XTUMLRT to Cpp (the C++ subset meta-model)
		try {
			accumulatedStatus.addAll(codegen.generate(elements));
		} catch (Throwable t) {
			accumulatedStatus.add(CodeGenPlugin.error("Error during generation from XTUMLRT to Cpp", t));
			t.printStackTrace(System.err);
			return;
		}
		// 3. Translate from Cpp model elements to C++ source files (creates a CDT project).
		long writeStart = System.currentTimeMillis();
		if (cpp.write()) {
			accumulatedStatus.add(CodeGenPlugin.info("Updated generated files "
					+ (System.currentTimeMillis() - writeStart) + "ms"));
		} else {
			accumulatedStatus.add(CodeGenPlugin.error("Failed to write generated model to disk"));
		}
	}

	private void handleValidationResult(MultiStatus result) throws Throwable {
		for (IStatus status : result.getChildren()) {
			if (status.getSeverity() == IStatus.ERROR) {
				throw status.getException();
			} else if (status.getSeverity() == IStatus.WARNING) {
				CodeGenPlugin.warning(status.getMessage());
			} else {
				CodeGenPlugin.info(status.getMessage());
			}
		}
	}

	/**
	 * Perform the translation from UML to XTUMLRT.
	 * 
	 * @param elements
	 *            - A {@link List} of {@link EObject} elements; either UML elements or XTUMLRT elements.
	 * @param modelFolder
	 *            - The {@link File} handle for the input model folder.
	 * @param status
	 *            - The {@link MultiStatus} to accumulate results.
	 * @return The {@link List} of {@link EObject} XTUMLRT elements
	 */
	private List<EObject> translateFromUMLtoXTUMLRT(List<EObject> elements, File modelFolder, MultiStatus status) {
		Path path = modelFolder.toPath();
		List<EObject> translated = null;
		translator.setActionLanguage(ACTION_LANGUAGE);
		translator.setChangeSet(codegen.getChangeTracker().getAllChanged());

		status.addAll(translator.generate(elements, path));
		translated = translator.getAllGenerated();

		if (translated == null || translated.isEmpty()) {
			status.add(new Status(IStatus.WARNING, CodeGenPlugin.ID, "Unable to translate elements from UML to XTUMLRT"));
		}
		return translated;
	}

	/**
	 * Subclass must provide the input model folder.
	 * 
	 * @param context
	 *            - An {@link EObject}.
	 * @return The {@link File} handle to the folder that contains the model to which the context object belongs.
	 */
	protected abstract File getModelFolder(EObject context);

	/**
	 * Subclass must provide the output folder.
	 * 
	 * @param context
	 *            - An {@link EObject}.
	 * @param codeGen
	 *            - The {@link XTUMLRT2CppCodeGenerator}.
	 * @return The {@link File} handle to the folder that will contain the generated code.
	 */
	protected abstract File getOutputFolder(EObject context, XTUMLRT2CppCodeGenerator codeGen);

	/**
	 * Used when the code generation project does not exist, but
	 * the change tracker has entries for the context. The change tracker
	 * needs to be reset for this {@link EObject} context, otherwise,
	 * code generation will regenerate instead of generating new.
	 * 
	 * @param context
	 *            - An {@link EObject}.
	 * @param codeGen
	 *            - The {@link XTUMLRT2CppCodeGenerator}.
	 */
	protected void resetResource(EObject context, XTUMLRT2CppCodeGenerator codeGen) {
		if (codeGen != null) {
			Resource resource = context.eResource();
			codeGen.getChangeTracker().closeResource(resource);
		}
	}

}

