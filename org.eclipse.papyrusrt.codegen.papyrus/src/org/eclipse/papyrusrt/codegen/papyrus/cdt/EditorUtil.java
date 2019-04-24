/*******************************************************************************
 * Copyright (c) 2014-2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.papyrusrt.codegen.papyrus.cdt;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.papyrusrt.codegen.UserEditableRegion;
import org.eclipse.papyrusrt.codegen.papyrus.Activator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utility class to open CDT editor.
 * 
 * @author ysroh
 *
 */
public final class EditorUtil {

	/**
	 * Constructor.
	 *
	 */
	private EditorUtil() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Open editor for given file.
	 * 
	 * @param file
	 *            file to open
	 * @param label
	 *            user tag
	 * @param rc
	 *            status
	 */
	public static void openEditor(final IFile file, final UserEditableRegion.Label label, final MultiStatus rc) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IEditorPart part = IDE.openEditor(page, file);
					int line = UserEditableRegion.LOCATOR.getLineNumber(file, label);
					if (line < 0) {
						navigateWithSelectAndReveal(rc, part, file, 0);
					} else if (!navigateWithSelectAndReveal(rc, part, file, line)) {
						navigateWithMarker(rc, part, file, line);
					}
				} catch (PartInitException e) {
					rc.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
				}
			}
		});
	}

	/**
	 * Attempt to make visible the relevant portion of the file using the more efficient
	 * selectAndReveal API. Return true if successful and false otherwise.
	 * 
	 * @param rc
	 *            status
	 * @param part
	 *            editor part
	 * @param file
	 *            file
	 * @param line
	 *            line number
	 * @return true if opened
	 */
	private static boolean navigateWithSelectAndReveal(MultiStatus rc, IEditorPart part, IFile file, int line) {
		boolean result = false;
		if (part instanceof ITextEditor) {
			ITextEditor textEditor = (ITextEditor) part;
			IDocument doc = textEditor.getDocumentProvider().getDocument(part.getEditorInput());
			if (doc != null) {
				try {
					textEditor.selectAndReveal(doc.getLineOffset(line), 0);
					result = true;
				} catch (BadLocationException e) {
					rc.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
				}
			}
		}
		return result;
	}

	/**
	 * Attempt to make visible the relevant portion of the file using the less efficient
	 * gotoMarker API. Return true if successful and false otherwise.
	 * 
	 * @param rc
	 *            status
	 * @param part
	 *            editor part
	 * @param file
	 *            file
	 * @param line
	 *            line number
	 * @return true if opened
	 */
	private static boolean navigateWithMarker(MultiStatus rc, IEditorPart part, IFile file, int line) {
		boolean result = false;
		try {
			IMarker marker = file.createMarker(IMarker.MARKER);
			marker.setAttribute(IMarker.LINE_NUMBER, line);
			IDE.gotoMarker(part, marker);
			result = true;
		} catch (CoreException e) {
			rc.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}

		return result;
	}
}
