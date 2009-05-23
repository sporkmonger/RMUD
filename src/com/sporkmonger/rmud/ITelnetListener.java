package com.sporkmonger.rmud;

public interface ITelnetListener {
	public void readFromRemote(TelnetEvent event);
	public void readFromLocal(TelnetEvent event);
}
