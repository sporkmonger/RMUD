package com.sporkmonger.rmud;

import java.io.OutputStream;

public interface ITelnetBridge {
	public void sendToRemote(String data);
	public void sendToLocal(String data);
	public void receivedFromRemote(String data);
	public void receivedFromLocal(String data);
	
	public void addTelnetListener(ITelnetListener listener);

	public OutputStream getConsoleOutputStream();
	public OutputStream getConsoleErrorStream();
	public OutputStream getConsoleRemoteNetworkStream();
	public OutputStream getConsoleLocalNetworkStream();
}
