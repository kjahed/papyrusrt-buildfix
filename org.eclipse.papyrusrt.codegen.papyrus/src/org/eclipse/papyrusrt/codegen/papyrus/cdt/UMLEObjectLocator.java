/*******************************************************************************
 * Copyright (c) 2014-2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.papyrusrt.codegen.papyrus.cdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.papyrus.editor.PapyrusMultiDiagramEditor;
import org.eclipse.papyrus.infra.core.resource.ModelMultiException;
import org.eclipse.papyrus.infra.core.resource.ModelSet;
import org.eclipse.papyrus.infra.core.resource.NotFoundException;
import org.eclipse.papyrus.infra.core.services.ExtensionServicesRegistry;
import org.eclipse.papyrus.infra.core.services.ServiceException;
import org.eclipse.papyrus.infra.core.services.ServicesRegistry;
import org.eclipse.papyrus.uml.tools.model.UmlModel;
import org.eclipse.papyrusrt.codegen.UserEditableRegion;
import org.eclipse.papyrusrt.codegen.UserEditableRegion.CommitResult;
import org.eclipse.papyrusrt.codegen.UserEditableRegion.Label;
import org.eclipse.papyrusrt.codegen.UserEditableRegion.TransitionDetails;
import org.eclipse.papyrusrt.codegen.UserEditableRegion.TriggerDetail;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTCapsule;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTOpaqueBehavior;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTState;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTStateMachine;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTTransition;
import org.eclipse.papyrusrt.umlrt.uml.UMLRTTrigger;
import org.eclipse.papyrusrt.umlrt.uml.internal.facade.UMLRTUMLRTPackage;
import org.eclipse.papyrusrt.xtumlrt.util.NamesUtil;
import org.eclipse.papyrusrt.xtumlrt.util.QualifiedNames;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.uml2.common.util.UML2Util;
import org.eclipse.uml2.uml.AnyReceiveEvent;
import org.eclipse.uml2.uml.CallEvent;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Event;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.Vertex;

/**
 * 
 * @author ysroh
 *
 */
public class UMLEObjectLocator implements org.eclipse.papyrusrt.codegen.IEObjectLocator {

	/**
	 * Language.
	 */
	public static final String LANGUAGE = "C++";

	/** Resource set. */
	private ModelSet rset = null;

	/** Available model set. */
	private Map<String, ModelSet> modelSetMap = new HashMap<>();

	/**
	 * Constructor.
	 *
	 */
	public UMLEObjectLocator() {
	}

	/**
	 * Retrieve root element of given resource uri.
	 * 
	 * @param uri
	 *            resource uri
	 * @return root package
	 */
	private Package getRoot(String uri) {

		URI resourceUri = URI.createURI(uri);
		if (modelSetMap.containsKey(uri)) {
			rset = modelSetMap.get(uri);
		} else if (rset == null) {
			try {
				ServicesRegistry registry = new ExtensionServicesRegistry(org.eclipse.papyrus.infra.core.Activator.PLUGIN_ID);
				registry.startServicesByClassKeys(ModelSet.class);
				rset = registry.getService(ModelSet.class);
				try {
					registry.startRegistry();
				} catch (ServiceException se) {
				}
				rset.loadModels(resourceUri);
			} catch (ServiceException | ModelMultiException e) {
			}
		}

		UmlModel model = (UmlModel) rset.getModel(UmlModel.MODEL_ID);
		Package root = null;
		try {
			root = (Package) model.lookupRoot();
		} catch (NotFoundException e) {
		}
		return root;
	}

	@Override
	public EObject getEObject(UserEditableRegion.Label label) {

		List<String> qualifiedNames = new ArrayList<>();
		qualifiedNames.addAll(Arrays.asList(label.getQualifiedName().split(QualifiedNames.SEPARATOR)));

		Package nextPackage = getRoot(label.getUri());
		if (nextPackage == null) {
			// resource not found
			return null;
		}
		Class capsuleOrClass = null;
		// remove model name
		qualifiedNames.remove(0);
		// find a capsule or class
		while (!qualifiedNames.isEmpty()) {
			String qname = qualifiedNames.remove(0);
			Package pkg = nextPackage.getNestedPackage(qname);
			if (pkg == null) {
				capsuleOrClass = (Class) nextPackage.getPackagedElement(qname, false, UMLPackage.Literals.CLASS, false);
				break;
			}
			nextPackage = pkg;
		}

		EObject result = null;

		if (capsuleOrClass != null) {
			if (label.getType().equals(UMLPackage.Literals.OPERATION.getName().toLowerCase())) {
				// Operation
				final Class clazz = capsuleOrClass;
				Operation op = capsuleOrClass.getOwnedOperation(label.getDetails(), null, null);
				if (op != null) {
					TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(capsuleOrClass);
					if (op.getMethods().isEmpty()) {
						Command cmd = new RecordingCommand(domain) {

							@Override
							protected void doExecute() {
								OpaqueBehavior method = (OpaqueBehavior) clazz.createOwnedBehavior(null, UMLPackage.Literals.OPAQUE_BEHAVIOR);
								op.getMethods().add(method);
								method.getLanguages().add(LANGUAGE);
								method.getBodies().add("");
							}
						};
						domain.getCommandStack().execute(cmd);
					}

					OpaqueBehavior method = (OpaqueBehavior) op.getMethods().get(0);

					if (!method.getLanguages().contains(LANGUAGE)) {
						Command cmd = new RecordingCommand(domain) {

							@Override
							protected void doExecute() {
								method.getLanguages().add(LANGUAGE);
								method.getBodies().add("");
							}
						};
						domain.getCommandStack().execute(cmd);
					}

					result = method;
				}
			} else {
				// This is element belong to a state machine.
				EObject smElement = getSMElement(capsuleOrClass, qualifiedNames);
				result = getUserCodeElement(smElement, label.getType(), label.getDetails());

			}
		}

		return result;
	}

	/**
	 * Queries the element that holds the user code.
	 * 
	 * @param container
	 *            cotainer
	 * @param type
	 *            element type
	 * @param details
	 *            element details
	 * @return element containing user code
	 */
	private EObject getUserCodeElement(EObject container, String type, String details) {
		EObject result = null;
		if (type.equals(UMLPackage.Literals.TRANSITION.getName().toLowerCase())) {
			// transition action
			Transition t = getTransition(container, details);
			if (t != null) {
				result = t.getEffect();
			}
		} else if (type.equals(UMLPackage.Literals.STATE__ENTRY.getName())) {
			// entry action
			if (container instanceof State) {
				UMLRTState state = UMLRTState.getInstance((State) container);
				if (state != null) {
					result = state.getEntry().toUML();
				} else {
					// could be class state machine
					State s = (State) container;
					result = s.getEntry();
				}
			}
		} else if (type.equals(UMLPackage.Literals.STATE__EXIT.getName())) {
			// exit action
			if (container instanceof State) {
				UMLRTState state = UMLRTState.getInstance((State) container);
				if (state != null) {
					result = state.getExit().toUML();
				} else {
					// could be class state machine
					State s = (State) container;
					result = s.getExit();
				}
			}
		} else if (type.equals(UMLPackage.Literals.TRANSITION__GUARD.getName())) {
			// transition guard
			Transition t = getTransition(container, details);
			if (t != null) {
				UMLRTTransition rtTransition = UMLRTTransition.getInstance(t);
				if (rtTransition != null && rtTransition.getGuard() != null) {
					result = rtTransition.getGuard().toUML();
				} else {
					// could be class state machine
					result = t.getGuard();
				}
			}
		} else if (type.equals(UMLPackage.Literals.TRANSITION__TRIGGER.getName())) {
			// trigger guard
			String[] tokens = details.split(TransitionDetails.EXTRA_DETAIL_SEPARATOR);
			Transition transition = getTransition(container, tokens[0]);
			if (transition != null) {
				TriggerDetail tdetail = new TriggerDetail(tokens[1]);
				Trigger trigger = findTrigger(transition, tdetail);
				if (trigger != null) {
					UMLRTTrigger rtTrigger = UMLRTTrigger.getInstance(trigger);
					if (rtTrigger.getGuard() != null) {
						result = rtTrigger.getGuard().toUML();
					}
				}
			}
		}
		return result;
	}

	/**
	 * Find element under State machine.
	 * 
	 * @param capsule
	 *            Capsule containing state machine
	 * @param qnameRelativeToSM
	 *            qualified name relative to state machine
	 * @return EObject
	 */
	private EObject getSMElement(Class capsule, List<String> qnameRelativeToSM) {
		UMLRTCapsule rtCapsule = UMLRTCapsule.getInstance(capsule);
		EObject result = null;

		if (rtCapsule != null) {

			UMLRTStateMachine sm = rtCapsule.getStateMachine();
			UMLRTState state = null;
			if (!qnameRelativeToSM.isEmpty()) {
				state = (UMLRTState) sm.getVertex(qnameRelativeToSM.remove(0), false, UMLRTUMLRTPackage.Literals.STATE);
				while (state != null && !qnameRelativeToSM.isEmpty()) {
					state = (UMLRTState) state.getSubvertex(qnameRelativeToSM.remove(0), false, UMLRTUMLRTPackage.Literals.STATE);
				}
				result = !qnameRelativeToSM.isEmpty() ? null : (state == null ? null : state.toUML());
			} else {
				result = sm.toUML();
			}
		}

		return result;
	}

	/**
	 * Retrieve Transition from Capsule or Class.
	 * 
	 * @param container
	 *            State machine or State that contains this transition
	 * @param transitionDetails
	 *            Transition details
	 * @return Transition
	 */
	private Transition getTransition(EObject container, String transitionDetails) {
		Transition result = null;
		List<UMLRTTransition> transitions = new ArrayList<>();
		if (container instanceof StateMachine) {
			UMLRTStateMachine sm = UMLRTStateMachine.getInstance((StateMachine) container);
			transitions = sm.getTransitions();
		} else {
			UMLRTState state = UMLRTState.getInstance((State) container);
			transitions = state.getSubtransitions();
		}

		UserEditableRegion.TransitionDetails details = new UserEditableRegion.TransitionDetails(transitionDetails);

		for (UMLRTTransition t : transitions) {
			if (getSMQualifiedName(t.toUML().getSource()).equals(details.getSourceQname()) && getSMQualifiedName(t.toUML().getTarget()).equals(details.getTargetQname())) {
				if (details.getTriggerDetails().isEmpty()) {
					result = t.toUML();
					break;
				}

				// check if the number of triggers are same. Go to next transition if different.
				if (details.getTriggerDetails().size() != t.getTriggers().size()) {
					continue;
				}

				Trigger trigger = null;
				for (TriggerDetail detail : details.getTriggerDetails()) {
					trigger = findTrigger(t.toUML(), detail);
					if (trigger == null) {
						// if trigger details do not match with current transition then go to next one.
						break;
					}
				}
				if (trigger != null) {
					// if all triggers found then this is the transition we are looking for.
					result = t.toUML();
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Find trigger.
	 * 
	 * @param transition
	 *            transition
	 * @param detail
	 *            trigger detail
	 * @return trigger
	 */
	private Trigger findTrigger(Transition transition, TriggerDetail detail) {
		Trigger result = null;
		UMLRTTransition rtTransition = UMLRTTransition.getInstance(transition);
		List<Trigger> triggers = new ArrayList<>();
		if (rtTransition != null) {
			triggers = rtTransition.getTriggers().stream().map(t -> t.toUML()).collect(Collectors.toList());
		} else {
			triggers = transition.getTriggers();
		}
		for (Trigger trigger : triggers) {
			Event event = trigger.getEvent();
			if (event == null || !(event instanceof CallEvent || event instanceof AnyReceiveEvent)) {
				continue;
			}
			String signalName = "*";
			if (event instanceof CallEvent) {
				Operation message = ((CallEvent) event).getOperation();
				signalName = message.getName();
			}
			if (detail.getSignal().equals(signalName) && comparetriggerPorts(trigger.getPorts(), detail.getPorts())) {
				result = trigger;
				break;
			}
		}
		return result;
	}

	/**
	 * Compare actual trigger ports with expected port names.
	 * 
	 * @param ports
	 *            actual ports
	 * @param expectedPorts
	 *            expected port names
	 * @return boolean
	 */
	private boolean comparetriggerPorts(List<Port> ports, List<String> expectedPorts) {

		List<String> actualPorts = new ArrayList<>();
		for (Port p : ports) {
			actualPorts.add(p.getName());
		}
		actualPorts.retainAll(expectedPorts);
		return actualPorts.size() == expectedPorts.size();

	}

	/**
	 * Calculate qualified name relative to State Machine.
	 * 
	 * @param eObject
	 *            EObject
	 * @return QualifiedName
	 */
	public static String getSMQualifiedName(EObject eObject) {
		String result = UML2Util.EMPTY_STRING;
		EObject container = eObject;
		while (container != null && !(container instanceof StateMachine)) {
			if (container instanceof Vertex) {
				if (result.length() != 0) {
					result = QualifiedNames.SEPARATOR + result;
				}
				result = NamesUtil.getEffectiveName(container) + result;
			}
			container = container.eContainer();
		}
		return result;
	}

	/**
	 * @see org.eclipse.papyrusrt.codegen.IEObjectLocator#saveSource(org.eclipse.papyrusrt.codegen.UserEditableRegion.Label, java.lang.String)
	 *
	 * @param label
	 * @param source
	 * @return
	 */
	@Override
	public CommitResult saveSource(Label label, String source) {
		EObject eo = getEObject(label);
		CommitResult result = null;
		if (eo instanceof OpaqueBehavior) {
			result = saveOpaqueBehaviour(eo, (OpaqueBehavior) eo, source);

		} else if (eo instanceof Constraint) {
			Constraint guard = (Constraint) eo;
			ValueSpecification value = guard.getSpecification();
			if (value instanceof OpaqueExpression) {
				result = saveOpaqueExpression(eo, (OpaqueExpression) value, source);
			}
		}
		if (result == null) {
			result = new CommitResult(eo, null, false);
		}

		return result;
	}

	/**
	 * Trim source without carriage returns.
	 * 
	 * @param source
	 *            source
	 * @return result
	 */
	private String trimSource(String source) {
		String pattern = "\\r *|\\n *|\\t";
		String replaceString = "";

		return source.trim().replaceAll(pattern, replaceString);
	}

	/**
	 * Update source for OpaqueExpression.
	 * 
	 * @param context
	 *            context
	 * @param oe
	 *            OpaqueExpression
	 * @param source
	 *            source string
	 * @return true if updated
	 */
	private CommitResult saveOpaqueExpression(EObject context, OpaqueExpression oe, String source) {
		boolean shouldUpdate = false;
		final int bodyIndex = oe.getLanguages().indexOf(LANGUAGE);

		if (!oe.getBodies().isEmpty()) {
			String body = oe.getBodies().get(0);
			if (!trimSource(source).equals(trimSource(body))) {
				shouldUpdate = true;
			}
		} else {
			shouldUpdate = true;
		}
		TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(oe);
		Command command = null;
		if (shouldUpdate) {
			command = new RecordingCommand(domain) {

				@Override
				protected void doExecute() {
					if (bodyIndex != -1) {
						oe.getBodies().remove(bodyIndex);
						oe.getBodies().add(bodyIndex, source);
					} else {
						oe.getLanguages().add(LANGUAGE);
						oe.getBodies().add(source);
					}
				}
			};
		}
		return new CommitResult(context, command, command != null && !modelSetMap.containsKey(rset));
	}

	/**
	 * Update source for OpaqueBehavior.
	 * 
	 * @param context
	 *            context
	 * @param ob
	 *            OpaqueBehavior
	 * @param source
	 *            source string
	 * @return true if updated
	 */
	private CommitResult saveOpaqueBehaviour(EObject context, OpaqueBehavior ob, String source) {

		boolean shouldUpdate = true;
		Command command = null;

		int index = ob.getLanguages().indexOf(LANGUAGE);
		if (index >= 0) {
			// check to see if we need to update the source
			if (ob.getBodies().size() > index) {
				String body = ob.getBodies().get(index);
				if (trimSource(source).equals(trimSource(body))) {
					shouldUpdate = false;
				}
			}
		}

		if (shouldUpdate) {
			AtomicReference<Runnable> reference = new AtomicReference<>();
			UMLRTOpaqueBehavior rtOb = UMLRTOpaqueBehavior.getInstance(ob);
			if (rtOb != null) {
				// use facade if available
				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						rtOb.getBodies().put(LANGUAGE, source);
					}
				};
				reference.set(runnable);
			} else {
				// this is opaque behaviour for Operation
				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						int bodyIndex = ob.getLanguages().indexOf(LANGUAGE);
						if (bodyIndex == -1) {
							// add language if not found
							ob.getLanguages().add(LANGUAGE);
							bodyIndex = ob.getLanguages().size() - 1;
						}
						// fill missing bodies with empty string so we always have at least
						// same number of bodies for languages.
						int numFillBody = ob.getLanguages().size() - ob.getBodies().size();
						while (numFillBody-- > 0) {
							ob.getBodies().add("");
						}
						ob.getBodies().set(bodyIndex, source);
					}
				};
				reference.set(runnable);
			}

			TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(ob);
			command = new RecordingCommand(domain) {

				@Override
				protected void doExecute() {
					reference.get().run();
				}
			};
		}

		return new CommitResult(context, command, command != null && !modelSetMap.containsValue(rset));
	}

	/**
	 * @see org.eclipse.papyrusrt.codegen.IEObjectLocator#initialize()
	 *
	 */
	@Override
	public void initialize() {

		List<IEditorReference> editors = new ArrayList<>();
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			for (IWorkbenchWindow window : wb.getWorkbenchWindows()) {
				for (IWorkbenchPage page : window.getPages()) {
					for (IEditorReference editor : page.getEditorReferences()) {
						IEditorPart part = editor.getEditor(false);
						if (part instanceof PapyrusMultiDiagramEditor) {
							editors.add(editor);
						}
					}
				}
			}
		} catch (IllegalStateException e) {
			// workbench not ready so skip.
		}

		for (IEditorReference ref : editors) {
			PapyrusMultiDiagramEditor editor = (PapyrusMultiDiagramEditor) ref.getEditor(false);
			ServicesRegistry reg = editor.getServicesRegistry();
			try {
				ModelSet modelSet = reg.getService(ModelSet.class);
				UmlModel model = (UmlModel) modelSet.getModel(UmlModel.MODEL_ID);
				modelSetMap.put(model.getResourceURI().toString(), modelSet);
			} catch (ServiceException e) {
			}
		}
	}

	/**
	 * @see org.eclipse.papyrusrt.codegen.IEObjectLocator#cleanUp()
	 *
	 */
	@Override
	public void cleanUp() {
		if (rset != null && !modelSetMap.containsValue(rset)) {
			rset.unload();
		}
		rset = null;
		modelSetMap.clear();
	}
}
