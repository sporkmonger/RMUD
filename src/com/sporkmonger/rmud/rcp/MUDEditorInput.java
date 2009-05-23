package com.sporkmonger.rmud.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.sporkmonger.rmud.Server;

public class MUDEditorInput implements IEditorInput {
	private Server server;
	
	public MUDEditorInput(Server server) {
		this.server = server;
	}
	
	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return server.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return server.getHost() + ":" + server.getPort();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	public Server getServer() {
		return server;
	}
}
