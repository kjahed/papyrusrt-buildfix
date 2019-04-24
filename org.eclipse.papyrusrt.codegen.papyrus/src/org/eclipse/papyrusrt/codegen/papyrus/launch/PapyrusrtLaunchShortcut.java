package org.eclipse.papyrusrt.codegen.papyrus.launch;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.papyrus.editor.PapyrusMultiDiagramEditor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

public class PapyrusrtLaunchShortcut implements ILaunchShortcut {


	@Override
	public void launch(ISelection selection, String mode) {
		for (IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getEditorReferences()) {
			IEditorPart editor = ref.getEditor(true);
			if (editor instanceof PapyrusMultiDiagramEditor) {
				editor.setFocus();
				runCodeGen();
				break;
			}
		}
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		if (editor instanceof PapyrusMultiDiagramEditor) {
			runCodeGen();
		}
	}

	public void runCodeGen() {
		ICommandService commandSevice = ((IServiceLocator) PlatformUI.getWorkbench())
				.getService(ICommandService.class);
		IHandlerService handlerService = ((IServiceLocator) PlatformUI.getWorkbench())
				.getService(IHandlerService.class);

		Command command = commandSevice.getCommand("org.eclipse.papyrusrt.codegen.papyrus.codegen.regen");
		ExecutionEvent executionEvent = handlerService.createExecutionEvent(command, new Event());

		try {
			command.executeWithChecks(executionEvent);
		} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
			e.printStackTrace();
		}
	}
}
