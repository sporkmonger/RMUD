package com.sporkmonger.rmud;

import java.io.IOException;

import java.net.Socket;

public class Connection {
	private Server server;
	private Socket socket;
	
	public Connection(Server server) {
		this.server = server;
	}
	
	public void connect() throws IOException {
		socket = new Socket(server.getHost(), server.getPort());
	}

	public void disconnect() throws IOException {
		if (socket != null) {
			socket.close();
		}
		socket = null;
	}
	
	public boolean isConnected() {
		return (socket != null && socket.isConnected()); 
	}
	
	public boolean isClosed() {
		return (socket == null || socket.isClosed()); 
	}
	
	public Server getServer() {
		return server;
	}
	
	public Socket getSocket() {
		return socket;
	}
}
