package com.sporkmonger.rmud.rcp.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.sporkmonger.rmud.Server;
import com.sporkmonger.rmud.rcp.MUDEditor;
import com.sporkmonger.rmud.rcp.MUDEditorInput;
import com.sporkmonger.rmud.rcp.ServersView;

public class ConnectAction extends Action {
	ServersView serversView;

	public ConnectAction(ServersView sv)
	{
		this.serversView = sv;
		setText("Connect");
		setToolTipText("Connect to the associated MUD server.");
	}

	public void run()
	{
		TableViewer tableViewer = serversView.getTableViewer();
		StructuredSelection selection = (StructuredSelection)tableViewer.getSelection();
	    if (selection.size() != 1) {
	    	return;
	    }
		Server server = (Server)selection.getFirstElement();
				
		MUDEditorInput input = new MUDEditorInput(server);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		
		try {
			page.openEditor(input, MUDEditor.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
}
