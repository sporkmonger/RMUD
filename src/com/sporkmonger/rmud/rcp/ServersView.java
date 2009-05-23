package com.sporkmonger.rmud.rcp;

import java.io.IOException;

import com.sporkmonger.rmud.Server;
import com.sporkmonger.rmud.rcp.actions.ConnectAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

public class ServersView extends ViewPart {
	public static final String ID = "com.sporkmonger.rmud.connections.view";

	private TableViewer viewer;
	private ConnectAction connectAction;

	/**
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */
	class ConnectionsViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			try {
				return Server.load();
			} catch (IOException e) {
				e.printStackTrace();
				return new Server[0];
			}
		}
	}

	class ConnectionsViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		connectAction = new ConnectAction(this);
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ConnectionsViewContentProvider());
		viewer.setLabelProvider(new ConnectionsViewLabelProvider());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				connectAction.run();
			}
		});
		MenuManager menuManager = new MenuManager();
		viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));
		menuManager.add(connectAction);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public ConnectAction getConnectAction() {
		return connectAction;
	}
	
	public TableViewer getTableViewer() {
		return viewer;
	}
}
