package com.sporkmonger.rmud;

public class TelnetEvent {
	private ITelnetBridge bridge;
	private String data;
	
	public TelnetEvent(ITelnetBridge bridge, String data) {
		this.bridge = bridge;
		this.data = data;
	}
	
	public ITelnetBridge getBridge() {
		return bridge;
	}

	public String getData() {
		return data;
	}
}
